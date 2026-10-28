package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Whether Web Push is available, and the key browsers subscribe with. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PushConfigDto {
    @NotNull
    private Boolean enabled;
    /** VAPID public key (base64url), {@code null} when push is disabled. */
    private String publicKey;
}
