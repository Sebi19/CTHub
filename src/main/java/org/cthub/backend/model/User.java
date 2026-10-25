package org.cthub.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "app_user") // "user" is a reserved keyword in Postgres
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq_gen")
    @SequenceGenerator(name = "user_seq_gen", sequenceName = "user_seq")
    private Long id;

    @NotNull
    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String password; // BCrypt hash; null for passwordless accounts (login link only)

    @Enumerated(EnumType.STRING)
    private Role role; // ADMIN, USER

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "email_verified_at")
    private java.time.Instant emailVerifiedAt;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @Column(name = "preferred_locale")
    private String preferredLocale;

    @Column(name = "profile_completed_at")
    private java.time.Instant profileCompletedAt;

    /**
     * A profile counts as complete once the user has supplied the details registration no longer
     * asks for up front. Incomplete profiles are allowed to log in but are forced through the
     * completion form before they can do anything else.
     */
    public boolean isProfileComplete() {
        return profileCompletedAt != null;
    }

    /** Passwordless accounts (registered but never set a password) can only use e-mail login links. */
    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }

    // --- UserDetails Implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Spring Security's {@code DaoAuthenticationProvider} rejects a null password with an opaque
     * internal error, so passwordless accounts report an unmatchable placeholder instead. It is a
     * real BCrypt hash of a random secret generated once per JVM start, so it is well-formed (no
     * encoder warnings) and no raw password can ever match it - password login fails cleanly as bad
     * credentials. {@link #hasPassword()} is the check callers should branch on.
     */
    @Override
    public String getPassword() {
        return hasPassword() ? password : NO_PASSWORD_PLACEHOLDER;
    }

    private static final String NO_PASSWORD_PLACEHOLDER =
        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
            .encode(java.util.UUID.randomUUID().toString());

    @Override
    public String getUsername() {
        return email;
    }

    // Boilerplate for account status (simplify for now)
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }

    public enum Role {
        USER, ADMIN
    }
}