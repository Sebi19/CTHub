package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Whether notifications of {@code type} are also delivered via {@code channel}. Used both to list
 * the effective settings and to change one of them.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPreferenceDto {
    @NotNull
    private NotificationTypeDto type;
    @NotNull
    private NotificationChannelDto channel;
    @NotNull
    private Boolean enabled;
}
