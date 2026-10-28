package org.cthub.backend.notification.push;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers new notifications via Web Push. Runs after commit, so devices never show a notification
 * that was rolled back; the actual sending happens on {@link PushDispatcher}'s thread.
 */
@Component
@RequiredArgsConstructor
public class PushNotificationChannel {

    private final PushDispatcher dispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(NotificationsCreatedEvent event) {
        dispatcher.dispatch(event.notifications());
    }
}
