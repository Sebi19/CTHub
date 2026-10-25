package org.cthub.backend.service;

import org.cthub.backend.model.User;
import org.cthub.backend.model.UserToken;
import org.cthub.backend.model.UserTokenType;
import org.cthub.backend.repository.UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private UserTokenRepository userTokenRepository;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(userTokenRepository);
    }

    private User user() {
        return User.builder().id(1L).email("test@example.org").build();
    }

    @Test
    void issueInvalidatesPreviousUnconsumedTokensOfSameType() {
        User user = user();
        UserToken previous = UserToken.builder().id(10L).user(user).type(UserTokenType.EMAIL_VERIFICATION).build();
        when(userTokenRepository.findByUserAndTypeAndConsumedAtIsNull(user, UserTokenType.EMAIL_VERIFICATION))
            .thenReturn(List.of(previous));

        String rawToken = tokenService.issue(user, UserTokenType.EMAIL_VERIFICATION, Duration.ofHours(24));

        assertThat(rawToken).isNotBlank();
        assertThat(previous.isConsumed()).isTrue();
        verify(userTokenRepository).saveAll(List.of(previous));
        verify(userTokenRepository).save(any(UserToken.class));
    }

    @Test
    void findUsableReturnsEmptyWhenTokenExpired() {
        UserToken expired = UserToken.builder()
            .type(UserTokenType.EMAIL_VERIFICATION)
            .expiresAt(Instant.now().minusSeconds(60))
            .build();
        when(userTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThat(tokenService.findUsable("raw", UserTokenType.EMAIL_VERIFICATION)).isEmpty();
    }

    @Test
    void findUsableReturnsEmptyWhenTokenConsumed() {
        UserToken consumed = UserToken.builder()
            .type(UserTokenType.EMAIL_VERIFICATION)
            .expiresAt(Instant.now().plusSeconds(60))
            .consumedAt(Instant.now())
            .build();
        when(userTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(consumed));

        assertThat(tokenService.findUsable("raw", UserTokenType.EMAIL_VERIFICATION)).isEmpty();
    }

    @Test
    void findUsableReturnsEmptyWhenTypeDoesNotMatch() {
        UserToken resetToken = UserToken.builder()
            .type(UserTokenType.PASSWORD_RESET)
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
        when(userTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(resetToken));

        assertThat(tokenService.findUsable("raw", UserTokenType.EMAIL_VERIFICATION)).isEmpty();
    }

    @Test
    void findUsableReturnsTokenWhenValid() {
        UserToken valid = UserToken.builder()
            .type(UserTokenType.EMAIL_VERIFICATION)
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
        when(userTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(valid));

        assertThat(tokenService.findUsable("raw", UserTokenType.EMAIL_VERIFICATION)).contains(valid);
    }

    @Test
    void consumeMarksTokenWithConsumedAt() {
        UserToken token = UserToken.builder().build();

        tokenService.consume(token);

        assertThat(token.isConsumed()).isTrue();
        verify(userTokenRepository).save(token);
    }

    @Test
    void issuedTokensAreUniqueAndHashedDifferentlyEachTime() {
        User user = user();
        when(userTokenRepository.findByUserAndTypeAndConsumedAtIsNull(any(), any())).thenReturn(List.of());

        String first = tokenService.issue(user, UserTokenType.PASSWORD_RESET, Duration.ofHours(1));
        String second = tokenService.issue(user, UserTokenType.PASSWORD_RESET, Duration.ofHours(1));

        assertThat(first).isNotEqualTo(second);

        ArgumentCaptor<UserToken> captor = ArgumentCaptor.forClass(UserToken.class);
        verify(userTokenRepository, times(2)).save(captor.capture());
        // The raw token must never be stored verbatim - only its hash.
        captor.getAllValues().forEach(saved -> {
            assertThat(saved.getTokenHash()).isNotEqualTo(first).isNotEqualTo(second);
            assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex digest
        });
    }
}
