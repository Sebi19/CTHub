package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPageDto {
    @NotNull
    private List<NotificationDto> items;
    /** Pass as {@code cursor} to load the next (older) page; {@code null} on the last page. */
    private Long nextCursor;
}
