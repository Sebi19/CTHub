package org.cthub.backend.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.notification.event.NotificationReadStateChangedEvent;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.cthub.backend.notification.model.Notification;
import org.cthub.backend.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The only thing business code should touch to notify users. {@link #notify} persists one row per
 * recipient inside the caller's transaction and publishes a {@link NotificationsCreatedEvent} for
 * the delivery channels. All read/update operations are scoped to the given user id.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;
    /** Upper bound for a reconnect replay; a client that missed more simply reloads its first page. */
    public static final int MAX_REPLAY = 100;
    /** Keeps the dedupe lookup's IN-list well below Postgres' bind-parameter limit on broadcasts. */
    private static final int DEDUPE_LOOKUP_CHUNK = 1000;

    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public List<Notification> notify(NotificationRequest request, long recipientId) {
        return notify(request, List.of(recipientId));
    }

    /**
     * Creates one notification per distinct recipient. Recipients that already received a
     * notification with the same {@code dedupeKey} are skipped.
     */
    @Transactional
    public List<Notification> notify(NotificationRequest request, Collection<Long> recipientIds) {
        Set<Long> recipients = new LinkedHashSet<>(recipientIds);
        if (request.dedupeKey() != null) {
            recipients.removeAll(alreadyNotified(request.dedupeKey(), recipients));
        }
        if (recipients.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();
        List<Notification> notifications = recipients.stream()
            .map(userId -> Notification.builder()
                .user(userRepository.getReferenceById(userId))
                .type(request.type())
                .params(request.params())
                .link(request.link())
                .dedupeKey(request.dedupeKey())
                .createdAt(now)
                .build())
            .toList();

        List<Notification> saved = repository.saveAll(notifications);
        eventPublisher.publishEvent(NotificationsCreatedEvent.of(saved));
        log.debug("Created {} {} notification(s).", saved.size(), request.type());
        return saved;
    }

    private Set<Long> alreadyNotified(String dedupeKey, Set<Long> recipients) {
        List<Long> ids = new ArrayList<>(recipients);
        Set<Long> result = new LinkedHashSet<>();
        for (int i = 0; i < ids.size(); i += DEDUPE_LOOKUP_CHUNK) {
            result.addAll(repository.findUserIdsWithDedupeKey(
                dedupeKey, ids.subList(i, Math.min(i + DEDUPE_LOOKUP_CHUNK, ids.size()))));
        }
        return result;
    }

    /** Newest first. Pass the previous page's {@code nextCursor} to continue, or {@code null} to start. */
    @Transactional(readOnly = true)
    public NotificationPage list(long userId, Long cursor, int size, boolean unreadOnly) {
        int pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        long before = cursor != null ? cursor : Long.MAX_VALUE;

        // Fetch one extra row to learn whether another page exists without a count query.
        List<Notification> rows = repository.findPage(userId, before, unreadOnly, Limit.of(pageSize + 1));
        boolean hasMore = rows.size() > pageSize;
        List<Notification> items = hasMore ? rows.subList(0, pageSize) : rows;
        return new NotificationPage(items, hasMore ? items.getLast().getId() : null);
    }

    /** Notifications created after {@code afterId}, oldest first, capped at {@link #MAX_REPLAY}. */
    @Transactional(readOnly = true)
    public List<Notification> listAfter(long userId, long afterId) {
        return repository.findByUserIdAndIdGreaterThanOrderByIdAsc(userId, afterId, Limit.of(MAX_REPLAY));
    }

    @Transactional(readOnly = true)
    public long unreadCount(long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    /** Idempotent: an already-read notification keeps its original {@code readAt}. */
    @Transactional
    public Notification markRead(long userId, long notificationId) {
        Notification notification = findOwned(userId, notificationId);
        if (!notification.isRead()) {
            notification.setReadAt(Instant.now());
            eventPublisher.publishEvent(new NotificationReadStateChangedEvent(userId));
        }
        return notification;
    }

    @Transactional
    public Notification markUnread(long userId, long notificationId) {
        Notification notification = findOwned(userId, notificationId);
        if (notification.isRead()) {
            notification.setReadAt(null);
            eventPublisher.publishEvent(new NotificationReadStateChangedEvent(userId));
        }
        return notification;
    }

    /** Marks every notification up to and including {@code upToId} as read; returns how many changed. */
    @Transactional
    public int markAllRead(long userId, long upToId) {
        int updated = repository.markAllReadUpTo(userId, upToId, Instant.now());
        if (updated > 0) {
            eventPublisher.publishEvent(new NotificationReadStateChangedEvent(userId));
        }
        return updated;
    }

    private Notification findOwned(long userId, long notificationId) {
        return repository.findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND", "Notification not found."));
    }
}
