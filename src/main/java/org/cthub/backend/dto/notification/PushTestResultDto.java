package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** How many of the user's devices the push services accepted the test message for. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PushTestResultDto {
    @NotNull
    private Integer devices;
    @NotNull
    private Integer delivered;
}
