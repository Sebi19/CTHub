package org.cthub.backend.notification;

import org.cthub.backend.notification.model.Notification;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Every lookup is scoped by user id, so one user can never read or modify another user's rows - an
 * unknown or foreign id simply isn't found.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /** Keyset page, newest first: rows with {@code id < before}. Use {@link Long#MAX_VALUE} for the first page. */
    @Query("""
        select n from Notification n
        where n.user.id = :userId
          and n.id < :before
          and (:unreadOnly = false or n.readAt is null)
        order by n.id desc
        """)
    List<Notification> findPage(Long userId, long before, boolean unreadOnly, Limit limit);

    /** Rows created after {@code afterId}, oldest first - used to replay what a reconnecting client missed. */
    List<Notification> findByUserIdAndIdGreaterThanOrderByIdAsc(Long userId, Long afterId, Limit limit);

    long countByUserIdAndReadAtIsNull(Long userId);

    /**
     * Marks everything up to and including {@code upToId} as read. The upper bound keeps a
     * notification that arrives while the user clicks "mark all as read" unread.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Notification n set n.readAt = :readAt
        where n.user.id = :userId
          and n.id <= :upToId
          and n.readAt is null
        """)
    int markAllReadUpTo(Long userId, long upToId, Instant readAt);

    @Query("select n.user.id from Notification n where n.dedupeKey = :dedupeKey and n.user.id in :userIds")
    Set<Long> findUserIdsWithDedupeKey(String dedupeKey, Collection<Long> userIds);

    @Modifying
    @Query("delete from Notification n where n.readAt < :cutoff")
    int deleteReadBefore(Instant cutoff);
}
