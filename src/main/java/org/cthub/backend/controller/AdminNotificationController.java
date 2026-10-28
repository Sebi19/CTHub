package org.cthub.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.notification.AnnouncementRequestDto;
import org.cthub.backend.dto.notification.AnnouncementResultDto;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.notification.NotificationRequest;
import org.cthub.backend.notification.NotificationService;
import org.cthub.backend.notification.model.NotificationType;
import org.cthub.backend.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Admin-only: sends a free-text {@code ANNOUNCEMENT} notification to one user or to everyone. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // POST /api/admin/notifications/announcement
    @PostMapping("/announcement")
    public AnnouncementResultDto sendAnnouncement(@Valid @RequestBody AnnouncementRequestDto request) {
        NotificationRequest notification = toNotificationRequest(request);

        List<Long> recipients;
        if (request.getUserId() != null) {
            if (!userRepository.existsById(request.getUserId())) {
                throw ApiException.notFound("USER_NOT_FOUND", "User not found.");
            }
            recipients = List.of(request.getUserId());
        } else {
            recipients = userRepository.findAllIds();
        }

        return new AnnouncementResultDto(notificationService.notify(notification, recipients).size());
    }

    private static NotificationRequest toNotificationRequest(AnnouncementRequestDto request) {
        String link = request.getLink() == null || request.getLink().isBlank() ? null : request.getLink().trim();
        try {
            return NotificationRequest.builder()
                .type(NotificationType.ANNOUNCEMENT)
                .params(Map.of("title", request.getTitle().trim(), "body", request.getBody().trim()))
                .link(link)
                .build();
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_LINK", "Link must be an in-app path starting with '/'.");
        }
    }
}
