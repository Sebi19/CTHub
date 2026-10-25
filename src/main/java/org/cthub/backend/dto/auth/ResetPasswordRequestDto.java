package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Redeems a password reset token and sets the new password. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequestDto {
    @NotBlank(message = "Token cannot be empty")
    private String token;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}
