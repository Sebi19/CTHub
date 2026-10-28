package org.cthub.backend.notification.push;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Delivery targets: subscriptions of the given users whose account is still enabled. */
    @Query("select new org.cthub.backend.notification.push.PushTarget(s.id, u.id, s.endpoint, s.p256dh, s.auth) "
        + "from PushSubscription s join s.user u where u.id in :userIds and u.enabled = true")
    List<PushTarget> findDeliverable(@Param("userIds") Collection<Long> userIds);

    @Modifying
    @Query("delete from PushSubscription s where s.id = :id and s.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") long id, @Param("userId") long userId);

    @Modifying
    @Query("delete from PushSubscription s where s.endpoint = :endpoint and s.user.id = :userId")
    int deleteByEndpointAndUserId(@Param("endpoint") String endpoint, @Param("userId") long userId);
}
