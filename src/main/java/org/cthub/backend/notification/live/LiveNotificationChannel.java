package org.cthub.backend.notification.live;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.notification.UnreadCountDto;
import org.cthub.backend.mapper.NotificationMapper;
import org.cthub.backend.notification.NotificationService;
import org.cthub.backend.notification.event.NotificationReadStateChangedEvent;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pushes notification changes to the user's open streams. Runs after commit, so clients never see
 * a notification that was rolled back; the unread count is re-read on the broker's lane thread.
 *
 * <p>Stream events: {@code notification} ({@code NotificationDto}) and {@code unread-count}
 * ({@code UnreadCountDto}).
 */
@Component
@RequiredArgsConstructor
public class LiveNotificationChannel {

    static final String EVENT_NOTIFICATION = "notification";
    static final String EVENT_UNREAD_COUNT = "unread-count";

    private final LiveNotificationBroker broker;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(NotificationsCreatedEvent event) {
        Map<Long, List<NotificationsCreatedEvent.Item>> byUser = event.notifications().stream()
            .collect(Collectors.groupingBy(NotificationsCreatedEvent.Item::userId));

        byUser.forEach((userId, items) -> broker.dispatch(userId, sender -> {
            items.forEach(item -> sender.send(EVENT_NOTIFICATION, notificationMapper.toDto(item)));
            sender.send(EVENT_UNREAD_COUNT, unreadCount(userId));
        }));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReadStateChanged(NotificationReadStateChangedEvent event) {
        broker.dispatch(event.userId(), sender -> sender.send(EVENT_UNREAD_COUNT, unreadCount(event.userId())));
    }

    private UnreadCountDto unreadCount(long userId) {
        return new UnreadCountDto(notificationService.unreadCount(userId));
    }
}
