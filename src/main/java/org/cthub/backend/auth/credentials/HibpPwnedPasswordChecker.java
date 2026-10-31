package org.cthub.backend.auth.credentials;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Have I Been Pwned "Pwned Passwords" range API with k-anonymity: only the first five hex digits of
 * the password's SHA-1 leave the server, and {@code Add-Padding} hides the real size of the answer.
 */
@Component
@Slf4j
public class HibpPwnedPasswordChecker implements PwnedPasswordChecker {

    static final String BASE_URL = "https://api.pwnedpasswords.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final RestClient client;

    public HibpPwnedPasswordChecker() {
        this(BASE_URL);
    }

    HibpPwnedPasswordChecker(String baseUrl) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        factory.setReadTimeout(TIMEOUT);
        this.client = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .defaultHeader("Add-Padding", "true")
            .defaultHeader("User-Agent", "ChallengeTeamHub")
            .build();
    }

    @Override
    public boolean isPwned(String password) {
        String hash = sha1Hex(password);
        String prefix = hash.substring(0, 5);
        String suffix = hash.substring(5);
        try {
            String body = client.get().uri("/range/{prefix}", prefix).retrieve().body(String.class);
            return body != null && containsSuffix(body, suffix);
        } catch (RuntimeException e) {
            log.warn("Pwned Passwords check failed; accepting the password without it: {}", e.toString());
            return false;
        }
    }

    /** Lines are {@code SUFFIX:COUNT}; padding entries have a count of 0. */
    static boolean containsSuffix(String body, String suffix) {
        for (String line : body.split("\\R")) {
            int colon = line.indexOf(':');
            if (colon < 0 || !line.substring(0, colon).trim().equalsIgnoreCase(suffix)) {
                continue;
            }
            try {
                return Long.parseLong(line.substring(colon + 1).trim()) > 0;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }

    static String sha1Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
