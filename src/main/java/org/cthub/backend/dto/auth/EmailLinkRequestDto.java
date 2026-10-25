package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Requests either a password reset link or a passwordless sign-in link. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailLinkRequestDto {
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Must be a valid e-mail address")
    private String email;

    /** BCP-47 tag of the language selected in the UI. */
    private String locale;
}
