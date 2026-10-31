package org.cthub.backend.notification.push;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Delivery targets: subscriptions of enabled users whose session is still live. */
    @Query("select new org.cthub.backend.notification.push.PushTarget(s.id, u.id, s.endpoint, s.p256dh, s.auth) "
        + "from PushSubscription s join s.user u join s.authSession a "
        + "where u.id in :userIds and u.enabled = true and a.revokedAt is null and a.expiresAt > :now")
    List<PushTarget> findDeliverable(@Param("userIds") Collection<Long> userIds, @Param("now") Instant now);

    @Modifying
    @Query("delete from PushSubscription s where s.id = :id and s.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") long id, @Param("userId") long userId);

    @Modifying
    @Query("delete from PushSubscription s where s.endpoint = :endpoint and s.user.id = :userId")
    int deleteByEndpointAndUserId(@Param("endpoint") String endpoint, @Param("userId") long userId);

    @Modifying
    @Query("delete from PushSubscription s where s.endpoint = :endpoint")
    int deleteByEndpoint(@Param("endpoint") String endpoint);

    @Modifying
    @Query("delete from PushSubscription s where s.authSession.id = :sessionId")
    int deleteByAuthSessionId(@Param("sessionId") UUID sessionId);
}
