package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Finishes an account after its first sign-in: just the name. Credentials are optional extras in the account settings. */
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
}
