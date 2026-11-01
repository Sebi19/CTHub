package org.cthub.backend.mapper;

import org.cthub.backend.auth.DeviceLabel;
import org.cthub.backend.auth.attempt.LoginAttempt;
import org.cthub.backend.auth.attempt.LoginAttemptService;
import org.cthub.backend.auth.session.AuthSession;
import org.cthub.backend.auth.webauthn.WebAuthnCredential;
import org.cthub.backend.dto.auth.ChallengeViewDto;
import org.cthub.backend.dto.auth.ChallengeViewKind;
import org.cthub.backend.dto.auth.LoginAttemptState;
import org.cthub.backend.dto.auth.LoginAttemptStateDto;
import org.cthub.backend.dto.auth.PasskeyDto;
import org.cthub.backend.dto.auth.SessionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "synced", source = "backupState")
    PasskeyDto toPasskeyDto(WebAuthnCredential credential);

    List<PasskeyDto> toPasskeyDtos(List<WebAuthnCredential> credentials);

    default SessionDto toSessionDto(AuthSession session, UUID currentSessionId) {
        return new SessionDto(session.getId(), DeviceLabel.of(session.getUserAgent()), session.getCreatedAt(),
            session.getLastUsedAt(), session.getId().equals(currentSessionId), session.isInstalledApp(),
            List.copyOf(session.methods()));
    }

    default LoginAttemptStateDto toStateDto(LoginAttempt attempt, Instant now) {
        LoginAttemptState state = switch (attempt.getStatus()) {
            case PENDING -> attempt.isExpired(now) ? LoginAttemptState.EXPIRED : LoginAttemptState.WAITING;
            case APPROVED -> attempt.isExpired(now) ? LoginAttemptState.EXPIRED : LoginAttemptState.APPROVED;
            case COMPLETED -> LoginAttemptState.COMPLETED;
            case CLAIMED_ELSEWHERE -> LoginAttemptState.CLAIMED_ELSEWHERE;
            case REJECTED -> LoginAttemptState.REJECTED;
            case CANCELLED -> LoginAttemptState.CANCELLED;
        };
        return new LoginAttemptStateDto(state, attempt.getPurpose(), attempt.getChallengeNumber(),
            attempt.getExpiresAt(), attempt.getLinkOpenedAt() != null || attempt.getPushOpenedAt() != null);
    }

    default ChallengeViewDto toChallengeViewDto(LoginAttemptService.LinkView view) {
        ChallengeViewKind kind = switch (view.kind()) {
            case SAME_BROWSER -> ChallengeViewKind.SAME_BROWSER;
            case CROSS -> ChallengeViewKind.CROSS;
            case INVALID -> ChallengeViewKind.INVALID;
        };
        LoginAttempt attempt = view.attempt();
        if (attempt == null) {
            return new ChallengeViewDto(kind, null, List.of(), null, null, null);
        }
        return new ChallengeViewDto(kind, attempt.getPurpose(), view.options(),
            DeviceLabel.of(attempt.getInitiatorUserAgent()), attempt.getCreatedAt(), attempt.getExpiresAt());
    }
}
