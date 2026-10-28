package org.cthub.backend.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.notification.MarkAllReadRequestDto;
import org.cthub.backend.dto.notification.NotificationDto;
import org.cthub.backend.dto.notification.NotificationPageDto;
import org.cthub.backend.dto.notification.NotificationPreferenceDto;
import org.cthub.backend.dto.notification.UnreadCountDto;
import org.cthub.backend.mapper.NotificationMapper;
import org.cthub.backend.model.User;
import org.cthub.backend.notification.NotificationPreferenceService;
import org.cthub.backend.notification.NotificationService;
import org.cthub.backend.notification.live.LiveNotificationBroker;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/** The signed-in user's own notifications. Every operation is scoped to the principal. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationMapper notificationMapper;
    private final LiveNotificationBroker liveNotificationBroker;

    /** Which notification types the user also wants via e-mail (and, later, push). */
    // GET /api/notifications/preferences
    @GetMapping("/preferences")
    public List<NotificationPreferenceDto> getNotificationPreferences(@AuthenticationPrincipal User user) {
        return notificationMapper.toPreferenceDtos(preferenceService.list(user.getId()));
    }

    // PUT /api/notifications/preferences
    @PutMapping("/preferences")
    public NotificationPreferenceDto updateNotificationPreference(
        @AuthenticationPrincipal User user, @Valid @RequestBody NotificationPreferenceDto request) {
        return notificationMapper.toDto(preferenceService.update(
            user.getId(),
            notificationMapper.toDomain(request.getType()),
            notificationMapper.toDomain(request.getChannel()),
            request.getEnabled()));
    }

    // GET /api/notifications?cursor=123&size=20&unreadOnly=false
    @GetMapping
    public NotificationPageDto listNotifications(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "" + NotificationService.DEFAULT_PAGE_SIZE) int size,
        @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationMapper.toPageDto(notificationService.list(user.getId(), cursor, size, unreadOnly));
    }

    /** Everything created after {@code after}, oldest first; used to catch up after a reconnect. */
    // GET /api/notifications/replay?after=123
    @GetMapping("/replay")
    public List<NotificationDto> replayNotifications(@AuthenticationPrincipal User user, @RequestParam long after) {
        return notificationMapper.toDtos(notificationService.listAfter(user.getId(), after));
    }

    // GET /api/notifications/unread-count
    @GetMapping("/unread-count")
    public UnreadCountDto getUnreadCount(@AuthenticationPrincipal User user) {
        return new UnreadCountDto(notificationService.unreadCount(user.getId()));
    }

    // POST /api/notifications/{id}/read
    @PostMapping("/{id}/read")
    public NotificationDto markNotificationRead(@AuthenticationPrincipal User user, @PathVariable long id) {
        return notificationMapper.toDto(notificationService.markRead(user.getId(), id));
    }

    // POST /api/notifications/{id}/unread
    @PostMapping("/{id}/unread")
    public NotificationDto markNotificationUnread(@AuthenticationPrincipal User user, @PathVariable long id) {
        return notificationMapper.toDto(notificationService.markUnread(user.getId(), id));
    }

    // POST /api/notifications/read-all
    @PostMapping("/read-all")
    public UnreadCountDto markAllNotificationsRead(
        @AuthenticationPrincipal User user, @Valid @RequestBody MarkAllReadRequestDto request) {
        notificationService.markAllRead(user.getId(), request.getUpToId());
        return new UnreadCountDto(notificationService.unreadCount(user.getId()));
    }

    /**
     * Live stream ({@code text/event-stream}). Deliberately does no database work: with
     * open-in-view, anything touching JPA here would pin a pooled connection for the stream's
     * whole lifetime. Missed events are fetched via {@code /replay}.
     */
    // GET /api/notifications/stream
    @Hidden
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
        @AuthenticationPrincipal User user, HttpServletRequest request, HttpServletResponse response) {
        // Proxies (nginx, Railway) must not buffer the stream.
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        HttpSession session = request.getSession(false);
        return liveNotificationBroker.connect(user.getId(), session != null ? session.getId() : null);
    }
}
