package org.cthub.backend.auth.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    @Query("select s from AuthSession s where s.user.id = :userId and s.revokedAt is null and s.expiresAt > :now "
        + "order by s.lastUsedAt desc")
    List<AuthSession> findLiveByUserId(@Param("userId") long userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from AuthSession s where s.expiresAt < :cutoff or s.revokedAt < :cutoff")
    int deleteEndedBefore(@Param("cutoff") Instant cutoff);
}
