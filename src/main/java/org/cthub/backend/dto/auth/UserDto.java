package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    @NotNull
    private Long id;
    @NotNull
    private String email;
    /** {@code null} until the user completes their profile. */
    private String firstName;
    /** {@code null} until the user completes their profile. */
    private String lastName;
    private String role;
    /** Language the user last chose in the UI, as a BCP-47 tag. */
    private String preferredLocale;
    /** {@code false} while the account still needs to go through the complete-profile step. */
    private boolean profileComplete;
    /** {@code false} for passwordless accounts, which sign in via e-mailed links. */
    private boolean passwordSet;
}
