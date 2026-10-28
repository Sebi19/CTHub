package org.cthub.backend.notification;

import org.cthub.backend.notification.model.NotificationChannel;
import org.cthub.backend.notification.model.NotificationPreference;
import org.cthub.backend.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserIdAndTypeAndChannel(
        Long userId, NotificationType type, NotificationChannel channel);

    /** Explicit choices among {@code userIds}, as {@code [userId, enabled]} pairs. */
    @Query("""
        select p.user.id as userId, p.enabled as enabled from NotificationPreference p
        where p.type = :type and p.channel = :channel and p.user.id in :userIds
        """)
    List<ExplicitChoice> findChoices(NotificationType type, NotificationChannel channel, Collection<Long> userIds);

    interface ExplicitChoice {
        Long getUserId();

        Boolean getEnabled();
    }
}
