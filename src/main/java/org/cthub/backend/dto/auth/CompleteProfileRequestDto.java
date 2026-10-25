package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Finishes an account after e-mail verification. The password is optional: omitting it leaves a
 * passwordless account that signs in through e-mailed login links.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompleteProfileRequestDto {
    @NotBlank(message = "First name cannot be empty")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Size(max = 100)
    private String lastName;

    /** Optional. When present it must still satisfy the minimum length. */
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}
