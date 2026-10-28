package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PushUnregisterRequestDto {
    @NotBlank
    @Size(max = 2048)
    private String endpoint;
}
