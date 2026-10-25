package org.cthub.backend.repository;

import org.cthub.backend.model.User;
import org.cthub.backend.model.UserToken;
import org.cthub.backend.model.UserTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHash(String tokenHash);

    List<UserToken> findByUserAndTypeAndConsumedAtIsNull(User user, UserTokenType type);

    @Modifying
    @Query("delete from UserToken t where t.expiresAt < :cutoff or t.consumedAt is not null")
    int deleteExpiredOrConsumed(Instant cutoff);
}
