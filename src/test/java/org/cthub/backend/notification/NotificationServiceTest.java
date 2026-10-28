package org.cthub.backend.notification;

import org.cthub.backend.exception.ApiException;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.event.NotificationReadStateChangedEvent;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.cthub.backend.notification.model.Notification;
import org.cthub.backend.notification.model.NotificationType;
import org.cthub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, userRepository, eventPublisher);
    }

    private void stubPersistence() {
        when(userRepository.getReferenceById(anyLong()))
            .thenAnswer(inv -> User.builder().id(inv.getArgument(0)).build());
        AtomicLong ids = new AtomicLong(100);
        when(repository.saveAll(anyList())).thenAnswer(inv -> {
            List<Notification> rows = inv.getArgument(0);
            rows.forEach(n -> n.setId(ids.incrementAndGet()));
            return rows;
        });
    }

    private static Notification owned(long id, long userId, Instant readAt) {
        return Notification.builder()
            .id(id)
            .user(User.builder().id(userId).build())
            .type(NotificationType.ANNOUNCEMENT)
            .readAt(readAt)
            .build();
    }

    @Test
    void notifyCreatesOneRowPerDistinctRecipientAndPublishesSnapshots() {
        stubPersistence();
        NotificationRequest request = NotificationRequest.builder()
            .type(NotificationType.ANNOUNCEMENT)
            .params(Map.of("title", "Hello"))
            .link("/competitions")
            .build();

        List<Notification> created = service.notify(request, List.of(1L, 2L, 1L));

        assertThat(created).extracting(n -> n.getUser().getId()).containsExactly(1L, 2L);
        assertThat(created).allSatisfy(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.ANNOUNCEMENT);
            assertThat(n.getParams()).containsEntry("title", "Hello");
            assertThat(n.getLink()).isEqualTo("/competitions");
            assertThat(n.isRead()).isFalse();
        });

        ArgumentCaptor<NotificationsCreatedEvent> event = ArgumentCaptor.forClass(NotificationsCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().notifications())
            .extracting(NotificationsCreatedEvent.Item::userId, NotificationsCreatedEvent.Item::id)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1L, created.get(0).getId()),
                org.assertj.core.groups.Tuple.tuple(2L, created.get(1).getId()));
    }

    @Test
    void notifySkipsRecipientsThatAlreadyHaveTheDedupeKey() {
        stubPersistence();
        when(repository.findUserIdsWithDedupeKey(eq("results:42"), anyCollection())).thenReturn(Set.of(1L));
        NotificationRequest request = NotificationRequest.builder()
            .type(NotificationType.ANNOUNCEMENT)
            .dedupeKey("results:42")
            .build();

        List<Notification> created = service.notify(request, List.of(1L, 2L));

        assertThat(created).extracting(n -> n.getUser().getId()).containsExactly(2L);
        assertThat(created.getFirst().getDedupeKey()).isEqualTo("results:42");
    }

    @Test
    void notifyDoesNothingWhenEveryRecipientIsDeduplicated() {
        when(repository.findUserIdsWithDedupeKey(eq("once"), anyCollection())).thenReturn(Set.of(1L));
        NotificationRequest request = NotificationRequest.builder()
            .type(NotificationType.WELCOME)
            .dedupeKey("once")
            .build();

        assertThat(service.notify(request, 1L)).isEmpty();
        verify(repository, never()).saveAll(anyList());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void notifyChunksTheDedupeLookupForLargeBroadcasts() {
        stubPersistence();
        when(repository.findUserIdsWithDedupeKey(eq("big"), anyCollection())).thenReturn(Set.of());
        List<Long> recipients = LongStream.rangeClosed(1, 2500).boxed().toList();

        service.notify(NotificationRequest.builder().type(NotificationType.ANNOUNCEMENT).dedupeKey("big").build(),
            recipients);

        verify(repository, times(3)).findUserIdsWithDedupeKey(eq("big"), anyCollection());
    }

    @Test
    void notifyWithoutRecipientsDoesNothing() {
        assertThat(service.notify(NotificationRequest.of(NotificationType.WELCOME), List.of())).isEmpty();
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void listReturnsCursorWhenAnotherPageExists() {
        List<Notification> rows = new ArrayList<>();
        for (long id = 30; id > 27; id--) {
            rows.add(owned(id, 1, null));
        }
        when(repository.findPage(1L, Long.MAX_VALUE, false, Limit.of(3))).thenReturn(rows);

        NotificationPage page = service.list(1L, null, 2, false);

        assertThat(page.items()).extracting(Notification::getId).containsExactly(30L, 29L);
        assertThat(page.nextCursor()).isEqualTo(29L);
    }

    @Test
    void listReturnsNoCursorOnLastPage() {
        when(repository.findPage(1L, 29L, true, Limit.of(3))).thenReturn(List.of(owned(28, 1, null)));

        NotificationPage page = service.list(1L, 29L, 2, true);

        assertThat(page.items()).hasSize(1);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void listClampsPageSize() {
        when(repository.findPage(anyLong(), anyLong(), anyBoolean(), any())).thenReturn(List.of());

        service.list(1L, null, 10_000, false);
        service.list(1L, null, 0, false);

        verify(repository).findPage(1L, Long.MAX_VALUE, false, Limit.of(NotificationService.MAX_PAGE_SIZE + 1));
        verify(repository).findPage(1L, Long.MAX_VALUE, false, Limit.of(2));
    }

    @Test
    void markReadSetsReadAtAndPublishes() {
        Notification notification = owned(5, 1, null);
        when(repository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(notification));

        service.markRead(1L, 5L);

        assertThat(notification.isRead()).isTrue();
        verify(eventPublisher).publishEvent(new NotificationReadStateChangedEvent(1L));
    }

    @Test
    void markReadIsIdempotent() {
        Instant originalReadAt = Instant.parse("2026-01-01T00:00:00Z");
        Notification notification = owned(5, 1, originalReadAt);
        when(repository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(notification));

        service.markRead(1L, 5L);

        assertThat(notification.getReadAt()).isEqualTo(originalReadAt);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void markReadOfForeignOrUnknownNotificationIsNotFound() {
        when(repository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(2L, 5L))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> {
                assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(((ApiException) e).getCode()).isEqualTo("NOTIFICATION_NOT_FOUND");
            });
    }

    @Test
    void markUnreadClearsReadAtAndPublishes() {
        Notification notification = owned(5, 1, Instant.now());
        when(repository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(notification));

        service.markUnread(1L, 5L);

        assertThat(notification.isRead()).isFalse();
        verify(eventPublisher).publishEvent(new NotificationReadStateChangedEvent(1L));
    }

    @Test
    void markAllReadPublishesOnlyWhenSomethingChanged() {
        when(repository.markAllReadUpTo(eq(1L), eq(50L), any())).thenReturn(3, 0);

        assertThat(service.markAllRead(1L, 50L)).isEqualTo(3);
        assertThat(service.markAllRead(1L, 50L)).isZero();

        verify(eventPublisher, times(1)).publishEvent(new NotificationReadStateChangedEvent(1L));
    }
}
