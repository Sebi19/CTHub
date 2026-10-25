package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Answer to the cross-device confirmation challenge.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmChallengeRequestDto {

    @NotBlank
    private String token;

    @NotNull
    @Min(10)
    @Max(99)
    private Short number;
}
