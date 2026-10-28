package org.cthub.backend.notification.push;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Web Push configuration ({@code app.push}). Push is switched off unless both VAPID keys are set;
 * generate a pair with {@code npx web-push generate-vapid-keys}.
 */
@Data
@ConfigurationProperties(prefix = "app.push")
public class PushProperties {

    @NestedConfigurationProperty
    private Vapid vapid = new Vapid();

    /** How long a push service keeps an undelivered message (device offline) before dropping it. */
    private Duration ttl = Duration.ofHours(24);

    /** Devices per user; registering one more drops the least recently seen. */
    private int maxSubscriptionsPerUser = 10;

    /**
     * Hosts a subscription endpoint may point to. Prevents users from making the server POST to
     * arbitrary URLs (SSRF). {@code *.example.com} matches any subdomain of example.com.
     */
    private List<String> allowedEndpointHosts = new ArrayList<>(List.of(
        "fcm.googleapis.com",            // Chrome, Edge (Android), Opera, Samsung Internet, Brave
        "*.push.services.mozilla.com",   // Firefox
        "*.push.apple.com",              // Safari (macOS, iOS home-screen apps)
        "*.notify.windows.com"));        // Edge (Windows)

    public boolean isEnabled() {
        return vapid.getPublicKey() != null && !vapid.getPublicKey().isBlank()
            && vapid.getPrivateKey() != null && !vapid.getPrivateKey().isBlank();
    }

    @Data
    public static class Vapid {
        /** base64url uncompressed P-256 point; handed to browsers as {@code applicationServerKey}. */
        private String publicKey;
        /** base64url 32-byte P-256 scalar. A secret. */
        private String privateKey;
        /** Contact for push services; must be a {@code mailto:} or {@code https:} URI (Apple enforces this). */
        private String subject = "mailto:info@challengeteamhub.org";
    }
}
