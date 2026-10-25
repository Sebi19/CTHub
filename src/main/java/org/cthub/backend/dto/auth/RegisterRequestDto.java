package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration asks for nothing but the e-mail address; name and (optional) password are collected
 * after verification on the complete-profile step.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestDto {
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Must be a valid e-mail address")
    private String email;

    /**
     * BCP-47 tag of the language the user had selected in the UI. Preferred over the
     * {@code Accept-Language} header, which reflects browser settings rather than an explicit choice.
     */
    private String locale;
}
