package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * A notification as the client renders it. Text is not stored: the client builds title and body
 * from {@code type} and {@code params} in the user's current language.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    @NotNull
    private Long id;
    @NotNull
    private NotificationTypeDto type;
    @NotNull
    private Map<String, String> params;
    /** App-relative path to open on click, or {@code null}. */
    private String link;
    @NotNull
    private Instant createdAt;
    private Instant readAt;
}
