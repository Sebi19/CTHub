package org.cthub.backend.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** {@code app.security.*} */
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class AuthProperties {

    /**
     * Whether auth cookies are {@code Secure} (and use the {@code __Host-} prefix). Only disabled for
     * plain-http local development.
     */
    private boolean cookieSecure = true;

    /** Base64-encoded 256-bit AES key that encrypts TOTP secrets at rest. */
    private String totpEncryptionKey;

    /** Password for the seeded admin account (see DataSeeder). */
    private String adminPassword;

    private WebAuthn webauthn = new WebAuthn();

    @Getter
    @Setter
    public static class WebAuthn {
        /** Relying party id: the registrable domain passkeys are bound to (e.g. {@code challengeteamhub.org}). */
        private String rpId = "localhost";
        private String rpName = "Challenge Team Hub";
        /** Exact origins allowed to perform ceremonies (scheme, host and port). */
        private List<String> origins = new ArrayList<>(List.of("http://localhost:5173", "http://localhost:8080"));
    }
}
