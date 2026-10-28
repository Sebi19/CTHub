package org.cthub.backend.notification.live;

import org.cthub.backend.dto.notification.NotificationDto;
import org.cthub.backend.dto.notification.UnreadCountDto;
import org.cthub.backend.mapper.NotificationMapper;
import org.cthub.backend.notification.NotificationService;
import org.cthub.backend.notification.event.NotificationReadStateChangedEvent;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.cthub.backend.notification.model.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveNotificationChannelTest {

    @Mock
    private LiveNotificationBroker broker;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationMapper notificationMapper;

    private record Sent(String name, Object data) {
    }

    @SuppressWarnings("unchecked")
    private List<Sent> runDispatch(long userId) {
        ArgumentCaptor<Consumer<LiveNotificationBroker.Sender>> work = ArgumentCaptor.forClass(Consumer.class);
        verify(broker).dispatch(eq(userId), work.capture());
        List<Sent> sent = new ArrayList<>();
        work.getValue().accept((name, data) -> sent.add(new Sent(name, data)));
        return sent;
    }

    private static NotificationsCreatedEvent.Item item(long id, long userId) {
        return new NotificationsCreatedEvent.Item(id, userId, NotificationType.ANNOUNCEMENT, Map.of(), null, Instant.now());
    }

    @Test
    void createdNotificationsAreSentPerUserFollowedByUnreadCount() {
        LiveNotificationChannel channel = new LiveNotificationChannel(broker, notificationService, notificationMapper);
        NotificationDto dto10 = NotificationDto.builder().id(10L).build();
        NotificationDto dto11 = NotificationDto.builder().id(11L).build();
        when(notificationMapper.toDto(any(NotificationsCreatedEvent.Item.class)))
            .thenAnswer(inv -> ((NotificationsCreatedEvent.Item) inv.getArgument(0)).id() == 10L ? dto10 : dto11);
        when(notificationService.unreadCount(1L)).thenReturn(2L);

        channel.onCreated(new NotificationsCreatedEvent(List.of(item(10L, 1L), item(11L, 1L), item(12L, 2L))));

        List<Sent> sent = runDispatch(1L);
        assertThat(sent).extracting(Sent::name)
            .containsExactly(LiveNotificationChannel.EVENT_NOTIFICATION, LiveNotificationChannel.EVENT_NOTIFICATION,
                LiveNotificationChannel.EVENT_UNREAD_COUNT);
        assertThat(sent.get(0).data()).isSameAs(dto10);
        assertThat(sent.get(1).data()).isSameAs(dto11);
        assertThat(sent.get(2).data()).isEqualTo(new UnreadCountDto(2L));
        verify(broker).dispatch(eq(2L), any());
    }

    @Test
    void readStateChangeSendsFreshUnreadCount() {
        LiveNotificationChannel channel = new LiveNotificationChannel(broker, notificationService, notificationMapper);
        when(notificationService.unreadCount(5L)).thenReturn(0L);

        channel.onReadStateChanged(new NotificationReadStateChangedEvent(5L));

        assertThat(runDispatch(5L))
            .containsExactly(new Sent(LiveNotificationChannel.EVENT_UNREAD_COUNT, new UnreadCountDto(0L)));
    }
}
