package org.cthub.backend.controller;

import org.cthub.backend.dto.notification.AnnouncementRequestDto;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.notification.NotificationRequest;
import org.cthub.backend.notification.NotificationService;
import org.cthub.backend.notification.model.Notification;
import org.cthub.backend.notification.model.NotificationType;
import org.cthub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminNotificationControllerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;

    private AdminNotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminNotificationController(notificationService, userRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcastsToAllUsersWithTrimmedParams() {
        when(userRepository.findAllIds()).thenReturn(List.of(1L, 2L, 3L));
        when(notificationService.notify(any(NotificationRequest.class), anyCollection()))
            .thenReturn(List.of(new Notification(), new Notification(), new Notification()));

        var result = controller.sendAnnouncement(
            new AnnouncementRequestDto(" Hello ", " World ", " /competitions ", null));

        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        ArgumentCaptor<Collection<Long>> recipients = ArgumentCaptor.forClass(Collection.class);
        verify(notificationService).notify(request.capture(), recipients.capture());
        assertThat(request.getValue().type()).isEqualTo(NotificationType.ANNOUNCEMENT);
        assertThat(request.getValue().params()).isEqualTo(Map.of("title", "Hello", "body", "World"));
        assertThat(request.getValue().link()).isEqualTo("/competitions");
        assertThat(recipients.getValue()).containsExactly(1L, 2L, 3L);
        assertThat(result.getRecipients()).isEqualTo(3);
    }

    @Test
    void sendsToSingleUser() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(notificationService.notify(any(NotificationRequest.class), anyCollection()))
            .thenReturn(List.of(new Notification()));

        var result = controller.sendAnnouncement(new AnnouncementRequestDto("T", "B", "", 7L));

        verify(notificationService).notify(argThat((NotificationRequest r) -> r.link() == null), eq(List.of(7L)));
        verify(userRepository, never()).findAllIds();
        assertThat(result.getRecipients()).isEqualTo(1);
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> controller.sendAnnouncement(new AnnouncementRequestDto("T", "B", null, 7L)))
            .isInstanceOf(ApiException.class);
        verifyNoInteractions(notificationService);
    }

    @Test
    void rejectsExternalLinks() {
        assertThatThrownBy(() -> controller.sendAnnouncement(
            new AnnouncementRequestDto("T", "B", "https://evil.example", null)))
            .isInstanceOf(ApiException.class);
        verifyNoInteractions(notificationService);
    }
}
