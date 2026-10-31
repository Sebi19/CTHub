package org.cthub.backend.auth.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByParentId(Long parentId);

    /**
     * The atomic rotation claim. Under concurrent refreshes of the same token Postgres serialises
     * the row update; exactly one caller sees {@code 1} and may insert the successor. Clears the
     * persistence context so callers re-read fresh state afterwards.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.rotatedAt = :now where t.id = :id and t.rotatedAt is null")
    int markRotated(@Param("id") long id, @Param("now") Instant now);
}
