package org.cthub.backend.auth.webauthn;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionsAuthenticatorOutputs;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.auth.AuthProperties;
import org.cthub.backend.auth.SecureTokens;
import org.cthub.backend.dto.auth.PasskeyAuthenticationOptionsDto;
import org.cthub.backend.dto.auth.PasskeyDescriptorDto;
import org.cthub.backend.dto.auth.PasskeyRegistrationOptionsDto;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.model.User;
import org.cthub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Passkey ceremonies (WebAuthn Level 3 via webauthn4j). Challenges live in the HTTP session, are
 * single-use and expire after {@link #CEREMONY_TIMEOUT}. User verification is required, since a
 * passkey signs the user in on its own.
 */
@Service
@Slf4j
public class WebAuthnService {

    public static final Duration CEREMONY_TIMEOUT = Duration.ofMinutes(5);
    public static final int MAX_PASSKEYS = 20;
    private static final String REGISTRATION_KEY = "WEBAUTHN_REGISTRATION";
    private static final String AUTHENTICATION_KEY = "WEBAUTHN_AUTHENTICATION";
    private static final List<COSEAlgorithmIdentifier> ALGORITHMS = List.of(
        COSEAlgorithmIdentifier.ES256, COSEAlgorithmIdentifier.EdDSA, COSEAlgorithmIdentifier.RS256);

    private final WebAuthnCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final AuthProperties.WebAuthn config;
    private final Set<Origin> origins;
    private final WebAuthnManager manager = WebAuthnManager.createNonStrictWebAuthnManager();
    private final AttestedCredentialDataConverter credentialDataConverter =
        new AttestedCredentialDataConverter(new ObjectConverter());

    public WebAuthnService(WebAuthnCredentialRepository credentialRepository, UserRepository userRepository,
                           Clock clock, AuthProperties properties) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.config = properties.getWebauthn();
        this.origins = config.getOrigins().stream().map(String::trim).map(Origin::create).collect(Collectors.toSet());
    }

    /** A pending ceremony; {@code userId} is set for registrations and step-ups. */
    private record Ceremony(byte[] challenge, Instant expiresAt, Long userId) implements Serializable {
    }

    public boolean hasPasskeys(long userId) {
        return credentialRepository.existsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<WebAuthnCredential> list(long userId) {
        return credentialRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    // ---------------------------------------------------------------- registration

    @Transactional
    public PasskeyRegistrationOptionsDto registrationOptions(User user, HttpSession httpSession) {
        User managed = userRepository.findById(user.getId()).orElseThrow();
        if (managed.getWebauthnUserHandle() == null) {
            managed.setWebauthnUserHandle(SecureTokens.generate());
        }
        byte[] challenge = SecureTokens.randomBytes(32);
        httpSession.setAttribute(REGISTRATION_KEY,
            new Ceremony(challenge, clock.instant().plus(CEREMONY_TIMEOUT), managed.getId()));

        List<PasskeyDescriptorDto> exclude = credentialRepository.findByUserIdOrderByCreatedAtAsc(managed.getId())
            .stream().map(WebAuthnService::descriptor).toList();
        String displayName = managed.isProfileComplete()
            ? managed.getFirstName() + " " + managed.getLastName()
            : managed.getEmail();
        return new PasskeyRegistrationOptionsDto(
            new PasskeyRegistrationOptionsDto.PasskeyRpDto(config.getRpId(), config.getRpName()),
            new PasskeyRegistrationOptionsDto.PasskeyUserDto(managed.getWebauthnUserHandle(), managed.getEmail(), displayName),
            base64Url(challenge),
            ALGORITHMS.stream()
                .map(alg -> new PasskeyRegistrationOptionsDto.PasskeyCredentialParamDto("public-key", (int) alg.getValue()))
                .toList(),
            CEREMONY_TIMEOUT.toMillis(),
            exclude,
            new PasskeyRegistrationOptionsDto.PasskeyAuthenticatorSelectionDto("required", true, "required"),
            "none");
    }

    @Transactional
    public WebAuthnCredential register(User user, String responseJson, String name, HttpSession httpSession) {
        Ceremony ceremony = takeCeremony(httpSession, REGISTRATION_KEY);
        if (ceremony == null || !user.getId().equals(ceremony.userId())) {
            throw expired();
        }
        if (credentialRepository.findByUserIdOrderByCreatedAtAsc(user.getId()).size() >= MAX_PASSKEYS) {
            throw ApiException.badRequest("PASSKEY_LIMIT", "You have reached the maximum number of passkeys.");
        }
        List<PublicKeyCredentialParameters> params = ALGORITHMS.stream()
            .map(alg -> new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, alg))
            .toList();
        RegistrationData data;
        try {
            data = manager.verifyRegistrationResponseJSON(responseJson,
                new RegistrationParameters(serverProperty(ceremony), params, true, true));
        } catch (DataConversionException | VerificationException e) {
            log.info("Passkey registration rejected: {}", e.toString());
            throw invalid();
        }
        AuthenticatorData<?> authData = data.getAttestationObject().getAuthenticatorData();
        AttestedCredentialData attested = authData.getAttestedCredentialData();
        String credentialId = base64Url(attested.getCredentialId());
        if (credentialRepository.findByCredentialId(credentialId).isPresent()) {
            throw ApiException.badRequest("PASSKEY_EXISTS", "This passkey is already registered.");
        }
        String transports = data.getTransports() == null ? null : data.getTransports().stream()
            .map(AuthenticatorTransport::getValue).sorted().collect(Collectors.joining(","));

        return credentialRepository.save(WebAuthnCredential.builder()
            .user(userRepository.getReferenceById(user.getId()))
            .credentialId(credentialId)
            .attestedCredentialData(credentialDataConverter.convert(attested))
            .signCount(authData.getSignCount())
            .transports(transports == null || transports.isEmpty() ? null : transports)
            .backupEligible(authData.isFlagBE())
            .backupState(authData.isFlagBS())
            .uvInitialized(authData.isFlagUV())
            .name(cleanName(name))
            .createdAt(clock.instant())
            .build());
    }

    // ---------------------------------------------------------------- authentication

    /**
     * Options for a sign-in ({@code forUser == null}, any discoverable passkey) or a step-up
     * ({@code forUser} set; the passkey must then belong to that user).
     */
    public PasskeyAuthenticationOptionsDto authenticationOptions(HttpSession httpSession, User forUser) {
        byte[] challenge = SecureTokens.randomBytes(32);
        httpSession.setAttribute(AUTHENTICATION_KEY, new Ceremony(challenge,
            clock.instant().plus(CEREMONY_TIMEOUT), forUser == null ? null : forUser.getId()));
        return new PasskeyAuthenticationOptionsDto(base64Url(challenge), config.getRpId(),
            CEREMONY_TIMEOUT.toMillis(), "required", List.of());
    }

    /** Verifies an assertion and returns the user it belongs to. */
    @Transactional
    public User authenticate(String responseJson, HttpSession httpSession, User expectedUser) {
        Ceremony ceremony = takeCeremony(httpSession, AUTHENTICATION_KEY);
        Long expectedId = expectedUser == null ? null : expectedUser.getId();
        if (ceremony == null || !java.util.Objects.equals(ceremony.userId(), expectedId)) {
            throw expired();
        }
        AuthenticationData parsed;
        try {
            parsed = manager.parseAuthenticationResponseJSON(responseJson);
        } catch (DataConversionException e) {
            throw invalid();
        }
        if (parsed.getCredentialId() == null) {
            throw invalid();
        }
        WebAuthnCredential credential = credentialRepository.findByCredentialId(base64Url(parsed.getCredentialId()))
            .orElseThrow(WebAuthnService::invalid);
        User owner = credential.getUser();
        if (expectedId != null && !expectedId.equals(owner.getId())) {
            throw invalid();
        }
        byte[] userHandle = parsed.getUserHandle();
        if (userHandle != null && userHandle.length > 0
            && !base64Url(userHandle).equals(owner.getWebauthnUserHandle())) {
            throw invalid();
        }

        CredentialRecordImpl record = new CredentialRecordImpl(
            new NoneAttestationStatement(),
            credential.isUvInitialized(),
            credential.isBackupEligible(),
            credential.isBackupState(),
            credential.getSignCount(),
            credentialDataConverter.convert(credential.getAttestedCredentialData()),
            new AuthenticationExtensionsAuthenticatorOutputs<>(),
            null,
            null,
            parseTransports(credential.getTransports()));
        try {
            manager.verify(parsed, new AuthenticationParameters(serverProperty(ceremony), record, null, true, true));
        } catch (VerificationException e) {
            log.info("Passkey assertion rejected for credential {}: {}", credential.getId(), e.toString());
            throw invalid();
        }
        AuthenticatorData<?> authData = parsed.getAuthenticatorData();
        if (authData != null) {
            credential.setSignCount(authData.getSignCount());
            credential.setBackupState(authData.isFlagBS());
            if (authData.isFlagUV()) {
                credential.setUvInitialized(true);
            }
        }
        credential.setLastUsedAt(clock.instant());
        return owner;
    }

    // ---------------------------------------------------------------- management

    @Transactional
    public WebAuthnCredential rename(long userId, long id, String name) {
        WebAuthnCredential credential = credentialRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> ApiException.notFound("PASSKEY_NOT_FOUND", "Passkey not found."));
        credential.setName(cleanName(name));
        return credential;
    }

    @Transactional
    public WebAuthnCredential delete(long userId, long id) {
        WebAuthnCredential credential = credentialRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> ApiException.notFound("PASSKEY_NOT_FOUND", "Passkey not found."));
        credentialRepository.delete(credential);
        return credential;
    }

    // ---------------------------------------------------------------- helpers

    private Ceremony takeCeremony(HttpSession httpSession, String key) {
        Object value = httpSession.getAttribute(key);
        httpSession.removeAttribute(key);
        if (value instanceof Ceremony ceremony && ceremony.expiresAt().isAfter(clock.instant())) {
            return ceremony;
        }
        return null;
    }

    private ServerProperty serverProperty(Ceremony ceremony) {
        return new ServerProperty(origins, config.getRpId(), new DefaultChallenge(ceremony.challenge()));
    }

    private static PasskeyDescriptorDto descriptor(WebAuthnCredential credential) {
        List<String> transports = credential.getTransports() == null ? null
            : Arrays.asList(credential.getTransports().split(","));
        return new PasskeyDescriptorDto(credential.getCredentialId(), "public-key", transports);
    }

    private static Set<AuthenticatorTransport> parseTransports(String transports) {
        if (transports == null || transports.isBlank()) {
            return null;
        }
        return Arrays.stream(transports.split(",")).map(AuthenticatorTransport::create).collect(Collectors.toSet());
    }

    private static String cleanName(String name) {
        String cleaned = Optional.ofNullable(name).map(String::strip).orElse("");
        if (cleaned.isEmpty()) {
            cleaned = "Passkey";
        }
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static ApiException invalid() {
        return ApiException.badRequest("PASSKEY_INVALID", "The passkey could not be verified.");
    }

    private static ApiException expired() {
        return ApiException.badRequest("PASSKEY_CEREMONY_EXPIRED", "The passkey request expired. Please try again.");
    }
}
