package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** This browser's push subscription, as returned by {@code PushSubscription.toJSON()}. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PushSubscriptionRequestDto {
    @NotBlank
    @Size(max = 2048)
    private String endpoint;
    @NotBlank
    @Size(max = 128)
    private String p256dh;
    @NotBlank
    @Size(max = 64)
    private String auth;
    /** Whether the page runs as installed app (standalone display mode). */
    @NotNull
    private Boolean installed;
    /** Set when the browser rotated the subscription; that old endpoint is removed. */
    @Size(max = 2048)
    private String previousEndpoint;
    /**
     * Only refresh an existing registration of this user (404 otherwise). The app sets this on start,
     * so a device removed from another device's settings stays removed.
     */
    private Boolean renewOnly;
}
