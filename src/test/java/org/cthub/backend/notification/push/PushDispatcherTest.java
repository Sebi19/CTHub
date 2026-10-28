package org.cthub.backend.notification.push;

import org.cthub.backend.notification.NotificationPreferenceService;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.cthub.backend.notification.model.NotificationChannel;
import org.cthub.backend.notification.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PushDispatcherTest {

    private final VapidAuth vapid = mock(VapidAuth.class);
    private final PushSubscriptionRepository repository = mock(PushSubscriptionRepository.class);
    private final NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
    private final WebPushSender sender = mock(WebPushSender.class);
    private final PushProperties properties = new PushProperties();
    private PushDispatcher dispatcher;

    private final PushTarget alicePhone = new PushTarget(10, 1, "https://fcm.googleapis.com/a", "k", "a");
    private final PushTarget aliceLaptop = new PushTarget(11, 1, "https://fcm.googleapis.com/b", "k", "a");
    private final PushTarget bobPhone = new PushTarget(20, 2, "https://fcm.googleapis.com/c", "k", "a");

    @BeforeEach
    void setUp() {
        when(vapid.isEnabled()).thenReturn(true);
        dispatcher = new PushDispatcher(vapid, properties, repository, preferences, new PushMessageComposer(), sender);
    }

    @Test
    void pushesToEveryDeviceOfUsersWhoKeepPushEnabled() {
        when(preferences.recipientsFor(eq(NotificationType.ANNOUNCEMENT), eq(NotificationChannel.PUSH), anyCollection()))
            .thenReturn(Set.of(1L));
        when(repository.findDeliverable(List.of(1L))).thenReturn(List.of(alicePhone, aliceLaptop));
        when(sender.send(any(), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(PushResult.DELIVERED));

        dispatcher.deliver(List.of(announcement(100, 1), announcement(101, 2), welcome(102, 1)));

        ArgumentCaptor<byte[]> payload = ArgumentCaptor.forClass(byte[].class);
        verify(sender).send(eq(alicePhone), payload.capture(), eq(Duration.ofHours(24)), eq("cthub-ANNOUNCEMENT"));
        verify(sender).send(eq(aliceLaptop), any(), any(), any());
        verifyNoMoreInteractions(sender);
        verify(preferences, never()).recipientsFor(eq(NotificationType.WELCOME), any(), anyCollection());

        JsonNode json = JsonMapper.builder().build().readTree(payload.getValue());
        assertThat(json.get("notificationId").asLong()).isEqualTo(100);
        assertThat(json.get("title").asString()).isEqualTo("Title");
        assertThat(json.get("url").asString()).isEqualTo("/account/notifications");
        assertThat(json.get("tag").asString()).isEqualTo("cthub-ANNOUNCEMENT");
    }

    @Test
    void removesSubscriptionsThePushServiceReportsGone() {
        when(preferences.recipientsFor(any(), any(), anyCollection())).thenReturn(Set.of(1L, 2L));
        when(repository.findDeliverable(any())).thenReturn(List.of(alicePhone, bobPhone));
        when(sender.send(eq(alicePhone), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(PushResult.GONE));
        when(sender.send(eq(bobPhone), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(PushResult.FAILED));

        dispatcher.deliver(List.of(announcement(100, 1), announcement(101, 2)));

        verify(repository).deleteById(10L);
        verify(repository, never()).deleteById(20L);
    }

    @Test
    void sendTestReportsDeliveredDevices() {
        when(repository.findDeliverable(any())).thenReturn(List.of(alicePhone, aliceLaptop));
        when(sender.send(eq(alicePhone), any(), any(), eq("cthub-TEST"))).thenReturn(CompletableFuture.completedFuture(PushResult.DELIVERED));
        when(sender.send(eq(aliceLaptop), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(PushResult.REJECTED));

        assertThat(dispatcher.sendTest(1, Locale.ENGLISH)).isEqualTo(new PushDispatcher.TestResult(2, 1));
    }

    @Test
    void doesNothingWhenPushIsDisabled() {
        when(vapid.isEnabled()).thenReturn(false);
        dispatcher.dispatch(List.of(announcement(100, 1)));
        verifyNoInteractions(preferences, repository, sender);
    }

    private static NotificationsCreatedEvent.Item announcement(long id, long userId) {
        return new NotificationsCreatedEvent.Item(id, userId, NotificationType.ANNOUNCEMENT,
            Map.of("title", "Title", "body", "Body"), null, Instant.now());
    }

    private static NotificationsCreatedEvent.Item welcome(long id, long userId) {
        return new NotificationsCreatedEvent.Item(id, userId, NotificationType.WELCOME, Map.of(), null, Instant.now());
    }
}
