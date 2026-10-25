package org.cthub.backend.service;

import org.cthub.backend.mail.MailService;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.model.RegistrationOutcome;
import org.cthub.backend.model.User;
import org.cthub.backend.model.UserToken;
import org.cthub.backend.model.UserTokenType;
import org.cthub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private MailService mailService;

    private RegistrationService registrationService;

    /**
     * Mirrors {@code TokenService.issueToken}: hands back a detached attempt row that the service
     * then stamps with its challenge number, so assertions can read that number back.
     */
    private UserToken stubHandle(String rawToken) {
        UserToken attempt = UserToken.builder()
            .type(UserTokenType.REGISTRATION_STATUS)
            .tokenHash("hash-of-" + rawToken)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        when(tokenService.issueToken(any(), eq(UserTokenType.REGISTRATION_STATUS), any()))
            .thenReturn(new TokenService.IssuedToken(rawToken, attempt));
        return attempt;
    }

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setBaseUrl("https://app.example.org");
        registrationService = new RegistrationService(
            userRepository, passwordEncoder, tokenService, mailService, mailProperties);
    }

    // --- register -------------------------------------------------------------------------

    @Test
    void registerCreatesNamelessPasswordlessUserAndQueuesConfirmation() {
        when(userRepository.findByEmail("new@example.org")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(tokenService.issue(any(), eq(UserTokenType.EMAIL_VERIFICATION), any())).thenReturn("raw-token");
        stubHandle("reg-handle");

        RegistrationService.RegistrationStart result =
            registrationService.register("NEW@example.org", Locale.ENGLISH);

        assertThat(result.status()).isEqualTo(RegistrationService.RegistrationStart.Status.PENDING);
        assertThat(result.registrationId()).isEqualTo("reg-handle");
        assertThat(result.challengeNumber()).isBetween((short) 10, (short) 99);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.org");
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.hasPassword()).isFalse();
        assertThat(saved.getFirstName()).isNull();
        assertThat(saved.isProfileComplete()).isFalse();
        assertThat(saved.getPreferredLocale()).isEqualTo("en");

        verify(mailService).enqueue(eq(MailTemplate.REGISTRATION_CONFIRMATION), any(MailAddress.class),
 any(), eq(Locale.ENGLISH));
    }

    @Test
    void registerResendsConfirmationWhenExistingAccountUnverified() {
        User existing = User.builder().id(5L).email("taken@example.org").emailVerifiedAt(null).build();
        when(userRepository.findByEmail("taken@example.org")).thenReturn(Optional.of(existing));
        when(tokenService.issue(any(), eq(UserTokenType.EMAIL_VERIFICATION), any())).thenReturn("raw-token");
        stubHandle("reg-handle");

        RegistrationService.RegistrationStart result =
            registrationService.register("taken@example.org", Locale.GERMAN);

        assertThat(result.status()).isEqualTo(RegistrationService.RegistrationStart.Status.PENDING);
        assertThat(result.registrationId()).isEqualTo("reg-handle");
        verify(mailService).enqueue(eq(MailTemplate.REGISTRATION_CONFIRMATION), any(MailAddress.class),
 any(), eq(Locale.GERMAN));
    }

    @Test
    void registerReportsAlreadyRegisteredWithoutSendingMailWhenAccountVerified() {
        User existing = User.builder().id(7L).email("verified@example.org").firstName("Ada").lastName("Lovelace")
            .emailVerifiedAt(Instant.now()).build();
        when(userRepository.findByEmail("verified@example.org")).thenReturn(Optional.of(existing));

        RegistrationService.RegistrationStart result =
            registrationService.register("verified@example.org", Locale.GERMAN);

        assertThat(result.status()).isEqualTo(RegistrationService.RegistrationStart.Status.ALREADY_REGISTERED);
        assertThat(result.registrationId()).isNull();
        verify(userRepository, never()).save(any());
        verifyNoInteractions(mailService, tokenService);
    }

    // --- registrationState ----------------------------------------------------------------

    @Test
    void registrationStateReportsPendingThenVerified() {
        User user = User.builder().id(2L).email("p@example.org").build();
        UserToken handle = UserToken.builder().user(user).type(UserTokenType.REGISTRATION_STATUS)
            .expiresAt(Instant.now().plusSeconds(60)).build();
        when(tokenService.findUsable("handle", UserTokenType.REGISTRATION_STATUS)).thenReturn(Optional.of(handle));

        assertThat(registrationService.registrationState("handle"))
            .isEqualTo(RegistrationService.RegistrationState.PENDING);

        user.setEmailVerifiedAt(Instant.now());
        assertThat(registrationService.registrationState("handle"))
            .isEqualTo(RegistrationService.RegistrationState.VERIFIED);
    }

    @Test
    void registrationStateIsUnknownForBlankOrUnusableHandle() {
        assertThat(registrationService.registrationState("  "))
            .isEqualTo(RegistrationService.RegistrationState.UNKNOWN);
        assertThat(registrationService.registrationState(null))
            .isEqualTo(RegistrationService.RegistrationState.UNKNOWN);

        when(tokenService.findUsable("gone", UserTokenType.REGISTRATION_STATUS)).thenReturn(Optional.empty());
        assertThat(registrationService.registrationState("gone"))
            .isEqualTo(RegistrationService.RegistrationState.UNKNOWN);
    }

    /** The polling handle must never be able to stand in for the e-mailed verification token. */
    @Test
    void registrationStateNeverVerifiesTheAccountItself() {
        User user = User.builder().id(2L).email("p@example.org").enabled(false).build();
        UserToken handle = UserToken.builder().user(user).type(UserTokenType.REGISTRATION_STATUS)
            .expiresAt(Instant.now().plusSeconds(60)).build();
        when(tokenService.findUsable("handle", UserTokenType.REGISTRATION_STATUS)).thenReturn(Optional.of(handle));

        registrationService.registrationState("handle");

        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getEmailVerifiedAt()).isNull();
        verify(tokenService, never()).consume(any());
        verify(userRepository, never()).save(any());
    }

    // --- verifyEmail ----------------------------------------------------------------------

    @Test
    void verifyEmailActivatesUserAndReturnsItForSignIn() {
        User user = User.builder().id(9L).email("pending@example.org").emailVerifiedAt(null)
            .preferredLocale("en").build();
        UserToken token = UserToken.builder().user(user).type(UserTokenType.EMAIL_VERIFICATION)
            .expiresAt(Instant.now().plusSeconds(60)).build();
        when(tokenService.findUsable("raw", UserTokenType.EMAIL_VERIFICATION)).thenReturn(Optional.of(token));

        Optional<User> result = registrationService.verifyEmail("raw");

        assertThat(result).containsSame(user);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        verify(tokenService).consume(token);
        // The welcome mail now waits for profile completion, when a name actually exists.
        verifyNoInteractions(mailService);
    }

    @Test
    void verifyEmailReturnsEmptyWhenTokenNotUsable() {
        when(tokenService.findUsable("bad", UserTokenType.EMAIL_VERIFICATION)).thenReturn(Optional.empty());

        assertThat(registrationService.verifyEmail("bad")).isEmpty();
        verifyNoInteractions(mailService);
    }

    // --- completeProfile ------------------------------------------------------------------

    @Test
    void completeProfileStoresNameHashesPasswordAndSendsWelcomeOnce() {
        User user = User.builder().id(4L).email("fresh@example.org").preferredLocale("en").build();
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("S3cret!!")).thenReturn("hashed");

        User result = registrationService.completeProfile(user, " Ada ", " Lovelace ", "S3cret!!");

        assertThat(result.getFirstName()).isEqualTo("Ada");
        assertThat(result.getLastName()).isEqualTo("Lovelace");
        assertThat(result.hasPassword()).isTrue();
        assertThat(result.isProfileComplete()).isTrue();
        verify(mailService).enqueue(eq(MailTemplate.WELCOME), any(MailAddress.class), any(), eq(Locale.ENGLISH));

        // A second call is an ordinary profile edit and must not re-send the welcome mail.
        registrationService.completeProfile(user, "Ada", "Lovelace", null);
        verify(mailService, times(1)).enqueue(eq(MailTemplate.WELCOME), any(MailAddress.class), any(), any(Locale.class));
    }

    @Test
    void completeProfileLeavesAccountPasswordlessWhenPasswordOmitted() {
        User user = User.builder().id(4L).email("fresh@example.org").preferredLocale("de").build();
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        User result = registrationService.completeProfile(user, "Grace", "Hopper", "   ");

        assertThat(result.isProfileComplete()).isTrue();
        assertThat(result.hasPassword()).isFalse();
        verifyNoInteractions(passwordEncoder);
    }

    // --- password reset -------------------------------------------------------------------

    @Test
    void requestPasswordResetQueuesMailForKnownAccount() {
        User user = User.builder().id(6L).email("known@example.org").firstName("Ada").lastName("Lovelace")
            .emailVerifiedAt(Instant.now()).preferredLocale("de").build();
        when(userRepository.findByEmail("known@example.org")).thenReturn(Optional.of(user));
        when(tokenService.issue(user, UserTokenType.PASSWORD_RESET, TokenService.PASSWORD_RESET_TTL))
            .thenReturn("reset-token");

        registrationService.requestPasswordReset("Known@example.org", Locale.ENGLISH);

        // The account's stored preference wins over the locale of the current request.
        verify(mailService).enqueue(eq(MailTemplate.PASSWORD_RESET), any(MailAddress.class), any(), eq(Locale.GERMAN));
    }

    @Test
    void requestPasswordResetIsSilentForUnknownAccount() {
        when(userRepository.findByEmail("ghost@example.org")).thenReturn(Optional.empty());

        registrationService.requestPasswordReset("ghost@example.org", Locale.GERMAN);

        verifyNoInteractions(mailService, tokenService);
    }

    @Test
    void resetPasswordSetsPasswordAndAlsoVerifiesAStillPendingAccount() {
        User user = User.builder().id(8L).email("pending@example.org").emailVerifiedAt(null).enabled(false).build();
        UserToken token = UserToken.builder().user(user).type(UserTokenType.PASSWORD_RESET)
            .expiresAt(Instant.now().plusSeconds(60)).build();
        when(tokenService.findUsable("reset", UserTokenType.PASSWORD_RESET)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("brandNewPw1")).thenReturn("hashed");

        Optional<User> result = registrationService.resetPassword("reset", "brandNewPw1");

        assertThat(result).containsSame(user);
        assertThat(user.hasPassword()).isTrue();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        verify(tokenService).consume(token);
    }

    @Test
    void resetPasswordReturnsEmptyWhenTokenNotUsable() {
        when(tokenService.findUsable("bad", UserTokenType.PASSWORD_RESET)).thenReturn(Optional.empty());

        assertThat(registrationService.resetPassword("bad", "whatever1")).isEmpty();
        verifyNoInteractions(passwordEncoder);
    }

    // --- cross-device confirmation --------------------------------------------------------

    /**
     * A legitimate user must never lose their registration to a glance-alike, so every pair of
     * options has to differ in both digit positions.
     */
    @Test
    void challengeOptionsAreThreeClearlyDistinguishableNumbersIncludingTheCorrectOne() {
        for (int run = 0; run < 200; run++) {
            UserToken attempt = attempt(RegistrationChallenge.newNumber());
            attempt.setTokenHash("hash-" + run);

            List<Short> options = RegistrationChallenge.optionsFor(attempt);

            assertThat(options).hasSize(RegistrationChallenge.OPTION_COUNT)
                .contains(attempt.getChallengeNumber());
            for (Short a : options) {
                for (Short b : options) {
                    if (!a.equals(b)) {
                        assertThat(RegistrationChallenge.isClearlyDistinct(a, b))
                            .as("%s vs %s", a, b).isTrue();
                    }
                }
            }
        }
    }

    @Test
    void optionsAreStableAcrossRepeatedOpensOfTheSameLink() {
        UserToken attempt = attempt((short) 42);
        assertThat(RegistrationChallenge.optionsFor(attempt))
            .isEqualTo(RegistrationChallenge.optionsFor(attempt));
    }

    @Test
    void openChallengeShowsOptionsAndMarksTheAttemptAwaitingConfirmation() {
        User user = pendingUser();
        UserToken attempt = attempt((short) 42);
        stubVerificationToken(user);
        stubLiveAttempt(user, attempt);

        RegistrationService.ChallengeView view = registrationService.openChallenge("raw-token", null);

        assertThat(view).isInstanceOf(RegistrationService.ChallengeView.Options.class);
        assertThat(((RegistrationService.ChallengeView.Options) view).numbers()).contains((short) 42);
        assertThat(attempt.getChallengeOpenedAt()).isNotNull();
    }

    /** The originating browser opened its own link, so there is nobody to protect from it. */
    @Test
    void openChallengeSkipsThePromptForTheBrowserThatStartedTheRegistration() {
        User user = pendingUser();
        stubVerificationToken(user);
        stubLiveAttempt(user, attempt((short) 42));
        when(tokenService.findUsable("reg-handle", UserTokenType.REGISTRATION_STATUS))
            .thenReturn(Optional.of(liveAttempt));

        RegistrationService.ChallengeView view = registrationService.openChallenge("raw-token", "reg-handle");

        assertThat(view).isInstanceOf(RegistrationService.ChallengeView.SameBrowser.class);
    }

    /** No attempt waiting (a resend from the login screen, say) means no session to hand over. */
    @Test
    void openChallengeAsksNothingWhenNoBrowserIsWaiting() {
        User user = pendingUser();
        stubVerificationToken(user);
        when(tokenService.findUsableByUser(user, UserTokenType.REGISTRATION_STATUS)).thenReturn(Optional.empty());

        assertThat(registrationService.openChallenge("raw-token", null))
            .isInstanceOf(RegistrationService.ChallengeView.None.class);
    }

    @Test
    void correctPickConfirmsTheAttemptAndVerifiesTheAddress() {
        User user = pendingUser();
        UserToken attempt = attempt((short) 42);
        UserToken token = stubVerificationToken(user);
        stubLiveAttempt(user, attempt);

        assertThat(registrationService.confirmChallenge("raw-token", (short) 42)).isTrue();

        assertThat(attempt.getChallengeOutcome()).isEqualTo(RegistrationOutcome.CONFIRMED);
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.isEnabled()).isTrue();
        verify(tokenService).consume(token);
    }

    /** An honest misclick costs one resend, not the whole registration. */
    @Test
    void wrongPickBurnsOnlyTheLinkAndLeavesTheAttemptAlive() {
        User user = pendingUser();
        UserToken attempt = attempt((short) 42);
        UserToken token = stubVerificationToken(user);
        stubLiveAttempt(user, attempt);

        assertThat(registrationService.confirmChallenge("raw-token", (short) 17)).isFalse();

        verify(tokenService).consume(token);
        assertThat(attempt.getChallengeAttempts()).isEqualTo((short) 1);
        assertThat(attempt.getChallengeOutcome()).isNull();
        assertThat(user.getEmailVerifiedAt()).isNull();
    }

    @Test
    void thirdWrongPickCancelsTheAttempt() {
        User user = pendingUser();
        UserToken attempt = attempt((short) 42);
        attempt.setChallengeAttempts((short) (RegistrationChallenge.MAX_ATTEMPTS - 1));
        stubVerificationToken(user);
        stubLiveAttempt(user, attempt);

        assertThat(registrationService.confirmChallenge("raw-token", (short) 17)).isFalse();

        assertThat(attempt.getChallengeOutcome()).isEqualTo(RegistrationOutcome.CANCELLED);
        when(tokenService.findUsable("reg-handle", UserTokenType.REGISTRATION_STATUS))
            .thenReturn(Optional.of(attempt));
        assertThat(registrationService.registrationState("reg-handle"))
            .isEqualTo(RegistrationService.RegistrationState.CANCELLED);
    }

    @Test
    void claimRequiresAConfirmedOutcome() {
        UserToken attempt = attempt((short) 42);
        attempt.setUser(pendingUser());
        when(tokenService.findUsable("reg-handle", UserTokenType.REGISTRATION_STATUS))
            .thenReturn(Optional.of(attempt));

        assertThat(registrationService.claimRegistration("reg-handle")).isEmpty();

        attempt.setChallengeOutcome(RegistrationOutcome.CONFIRMED);
        assertThat(registrationService.claimRegistration("reg-handle")).isPresent();
        verify(tokenService).consume(attempt);
    }

    /** "Continue on this device instead" must stand the waiting browser down. */
    @Test
    void verifyingOnTheOpeningDeviceClaimsTheAttemptElsewhere() {
        User user = pendingUser();
        UserToken attempt = attempt((short) 42);
        stubVerificationToken(user);
        stubLiveAttempt(user, attempt);

        assertThat(registrationService.verifyEmail("raw-token")).contains(user);

        assertThat(attempt.getChallengeOutcome()).isEqualTo(RegistrationOutcome.CLAIMED_ELSEWHERE);
        assertThat(registrationService.claimRegistration("reg-handle")).isEmpty();
    }

    @Test
    void rejectingCancelsTheAttemptAndRemovesThePristineAccount() {
        User user = pendingUser();
        UserToken attempt = attempt((short) 42);
        stubVerificationToken(user);
        stubLiveAttempt(user, attempt);

        assertThat(registrationService.rejectRegistration("raw-token")).contains("pending@example.org");

        assertThat(attempt.getChallengeOutcome()).isEqualTo(RegistrationOutcome.CANCELLED);
        verify(userRepository).delete(user);
    }

    /** Only ever delete an account that has never been anything but a pending registration. */
    @Test
    void rejectingKeepsAnAccountThatAlreadyHasAPassword() {
        User user = pendingUser();
        user.setPassword("$2a$10$hash");
        stubVerificationToken(user);
        stubLiveAttempt(user, attempt((short) 42));

        registrationService.rejectRegistration("raw-token");

        verify(userRepository, never()).delete(any());
    }

    // --- confirmation fixtures ------------------------------------------------------------

    private UserToken liveAttempt;

    private User pendingUser() {
        return User.builder().id(42L).email("pending@example.org").enabled(false).build();
    }

    private UserToken attempt(short number) {
        return UserToken.builder()
            .type(UserTokenType.REGISTRATION_STATUS)
            .tokenHash("attempt-hash")
            .expiresAt(Instant.now().plusSeconds(3600))
            .challengeNumber(number)
            .build();
    }

    private UserToken stubVerificationToken(User user) {
        UserToken token = UserToken.builder()
            .type(UserTokenType.EMAIL_VERIFICATION)
            .tokenHash("verification-hash")
            .expiresAt(Instant.now().plusSeconds(3600))
            .user(user)
            .build();
        when(tokenService.findUsable("raw-token", UserTokenType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(token));
        return token;
    }

    private void stubLiveAttempt(User user, UserToken attempt) {
        attempt.setUser(user);
        liveAttempt = attempt;
        when(tokenService.findUsableByUser(user, UserTokenType.REGISTRATION_STATUS))
            .thenReturn(Optional.of(attempt));
    }

    // --- login links ----------------------------------------------------------------------

    @Test
    void requestLoginLinkQueuesLinkForVerifiedAccount() {
        User user = User.builder().id(10L).email("verified@example.org").firstName("Ada").lastName("Lovelace")
            .emailVerifiedAt(Instant.now()).build();
        when(userRepository.findByEmail("verified@example.org")).thenReturn(Optional.of(user));
        when(tokenService.issue(user, UserTokenType.LOGIN_LINK, TokenService.LOGIN_LINK_TTL))
            .thenReturn("login-token");

        registrationService.requestLoginLink("verified@example.org", Locale.ENGLISH);

        verify(mailService).enqueue(eq(MailTemplate.LOGIN_LINK), any(MailAddress.class), any(), eq(Locale.ENGLISH));
    }

    /** An unverified address has not been proven to belong to anyone, so it gets verification instead. */
    @Test
    void requestLoginLinkFallsBackToVerificationForUnverifiedAccount() {
        User user = User.builder().id(11L).email("pending@example.org").emailVerifiedAt(null).build();
        when(userRepository.findByEmail("pending@example.org")).thenReturn(Optional.of(user));
        when(tokenService.issue(any(), eq(UserTokenType.EMAIL_VERIFICATION), any())).thenReturn("raw-token");

        registrationService.requestLoginLink("pending@example.org", Locale.GERMAN);

        verify(tokenService, never()).issue(any(), eq(UserTokenType.LOGIN_LINK), any());
        verify(mailService).enqueue(eq(MailTemplate.REGISTRATION_CONFIRMATION), any(MailAddress.class),
 any(), eq(Locale.GERMAN));
    }

    @Test
    void requestLoginLinkIsSilentForUnknownAccount() {
        when(userRepository.findByEmail("ghost@example.org")).thenReturn(Optional.empty());

        registrationService.requestLoginLink("ghost@example.org", Locale.GERMAN);

        verifyNoInteractions(mailService, tokenService);
    }

    @Test
    void consumeLoginLinkReturnsUserAndBurnsToken() {
        User user = User.builder().id(12L).email("verified@example.org").emailVerifiedAt(Instant.now()).build();
        UserToken token = UserToken.builder().user(user).type(UserTokenType.LOGIN_LINK)
            .expiresAt(Instant.now().plusSeconds(60)).build();
        when(tokenService.findUsable("login", UserTokenType.LOGIN_LINK)).thenReturn(Optional.of(token));

        assertThat(registrationService.consumeLoginLink("login")).containsSame(user);
        verify(tokenService).consume(token);
    }

    @Test
    void consumeLoginLinkReturnsEmptyWhenTokenNotUsable() {
        when(tokenService.findUsable("bad", UserTokenType.LOGIN_LINK)).thenReturn(Optional.empty());

        assertThat(registrationService.consumeLoginLink("bad")).isEmpty();
    }

    // --- resendVerification ---------------------------------------------------------------

    @Test
    void resendVerificationDoesNothingWhenUserAlreadyVerified() {
        User user = User.builder().id(3L).email("done@example.org").emailVerifiedAt(Instant.now()).build();
        when(userRepository.findByEmail("done@example.org")).thenReturn(Optional.of(user));

        registrationService.resendVerification("done@example.org", Locale.GERMAN);

        verifyNoInteractions(mailService, tokenService);
    }

    @Test
    void resendVerificationDoesNothingWhenEmailUnknown() {
        when(userRepository.findByEmail("ghost@example.org")).thenReturn(Optional.empty());

        registrationService.resendVerification("ghost@example.org", Locale.GERMAN);

        verifyNoInteractions(mailService, tokenService);
    }

    /**
     * Regression guard: outbox dedupe keys are permanent, so attaching a per-user key here made the
     * second and every later confirmation mail disappear silently.
     */
    @Test
    void resendVerificationEnqueuesAFreshMailEveryTime() {
        User user = User.builder().id(13L).email("pending@example.org").emailVerifiedAt(null).build();
        when(userRepository.findByEmail("pending@example.org")).thenReturn(Optional.of(user));
        when(tokenService.issue(any(), eq(UserTokenType.EMAIL_VERIFICATION), any()))
            .thenReturn("token-1", "token-2");

        registrationService.resendVerification("pending@example.org", Locale.GERMAN);
        registrationService.resendVerification("pending@example.org", Locale.GERMAN);

        verify(mailService, times(2)).enqueue(eq(MailTemplate.REGISTRATION_CONFIRMATION),
            any(MailAddress.class), any(), eq(Locale.GERMAN));
    }
}
