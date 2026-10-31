package org.cthub.backend.auth.attempt;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    Optional<LoginAttempt> findByNonceHash(String nonceHash);

    Optional<LoginAttempt> findByLinkTokenHash(String linkTokenHash);

    /** Serialises every state transition of one attempt (approve vs. reject vs. complete races). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoginAttempt a where a.id = :id")
    Optional<LoginAttempt> findByIdForUpdate(@Param("id") UUID id);

    @Query("select a.id from LoginAttempt a where a.status = org.cthub.backend.auth.attempt.LoginAttemptStatus.PENDING "
        + "and a.emailSentAt is null and a.emailDueAt <= :now and a.expiresAt > :now")
    List<UUID> findEmailDue(@Param("now") Instant now);

    @Modifying
    @Query("delete from LoginAttempt a where a.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
