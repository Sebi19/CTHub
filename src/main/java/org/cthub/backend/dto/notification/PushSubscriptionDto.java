package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A registered push device. The endpoint is deliberately not exposed. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PushSubscriptionDto {
    @NotNull
    private Long id;
    /** Raw User-Agent at registration; the client derives a readable device label from it. */
    private String userAgent;
    @NotNull
    private Boolean installed;
    @NotNull
    private Instant createdAt;
    @NotNull
    private Instant lastSeenAt;
}
