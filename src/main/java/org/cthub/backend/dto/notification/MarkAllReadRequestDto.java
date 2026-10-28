package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code upToId} is the newest notification the client has seen, so one that arrives while the
 * request is in flight is not silently marked read.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarkAllReadRequestDto {
    @NotNull
    private Long upToId;
}
