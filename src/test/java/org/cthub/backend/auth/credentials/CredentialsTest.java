package org.cthub.backend.auth.credentials;

import org.cthub.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class CredentialsTest {

    // --- TOTP -------------------------------------------------------------------------------

    /** RFC 6238 Appendix B, SHA1 column. */
    @Test
    void totpMatchesTheRfc6238TestVectors() {
        byte[] secret = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

        assertThat(TotpService.generateCode(secret, 59 / 30, 8)).isEqualTo("94287082");
        assertThat(TotpService.generateCode(secret, 1111111109L / 30, 8)).isEqualTo("07081804");
        assertThat(TotpService.generateCode(secret, 1234567890L / 30, 8)).isEqualTo("89005924");
        assertThat(TotpService.generateCode(secret, 20000000000L / 30, 8)).isEqualTo("65353130");
        assertThat(TotpService.generateCode(secret, 59 / 30, 6)).isEqualTo("287082");
    }

    // --- HIBP -------------------------------------------------------------------------------

    @Test
    void hibpHashesAreUpperCaseSha1() {
        assertThat(HibpPwnedPasswordChecker.sha1Hex("password"))
            .isEqualTo("5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8");
    }

    @Test
    void hibpSuffixMatchingIgnoresPaddingEntries() {
        String body = "1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493\r\n"
            + "0018A45C4D1DEF81644B54AB7F969B88D65:0\r\n"
            + "00D4F6E8FA6EECAD2A3AA415EEC418D38EC:2";

        assertThat(HibpPwnedPasswordChecker.containsSuffix(body, "1E4C9B93F3F0682250B6CF8331B7EE68FD8")).isTrue();
        assertThat(HibpPwnedPasswordChecker.containsSuffix(body, "1e4c9b93f3f0682250b6cf8331b7ee68fd8")).isTrue();
        assertThat(HibpPwnedPasswordChecker.containsSuffix(body, "0018A45C4D1DEF81644B54AB7F969B88D65")).isFalse();
        assertThat(HibpPwnedPasswordChecker.containsSuffix(body, "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")).isFalse();
    }

    @Test
    void hibpFailsOpenWhenTheServiceIsUnreachable() {
        assertThat(new HibpPwnedPasswordChecker("http://127.0.0.1:1").isPwned("correct horse battery staple")).isFalse();
    }

    // --- password policy --------------------------------------------------------------------

    @Test
    void passwordPolicyChecksLengthAndBreachesButNoCompositionRules() {
        PasswordPolicy policy = new PasswordPolicy(password -> password.equals("passwordpassword"));

        assertThatNoException().isThrownBy(() -> policy.validate("all lower case words"));
        assertThat(code(policy, "short")).isEqualTo("PASSWORD_TOO_SHORT");
        assertThat(code(policy, "x".repeat(65))).isEqualTo("PASSWORD_TOO_LONG");
        // 30 three-byte characters: within 64 code points but beyond BCrypt's 72 bytes.
        assertThat(code(policy, "\u20ac".repeat(30))).isEqualTo("PASSWORD_TOO_LONG");
        assertThat(code(policy, "passwordpassword")).isEqualTo("PASSWORD_PWNED");
        // Emoji count as one character each, not two UTF-16 units.
        assertThat(code(policy, "\uD83D\uDE00".repeat(11))).isEqualTo("PASSWORD_TOO_SHORT");
    }

    private static String code(PasswordPolicy policy, String password) {
        ApiException e = catchThrowableOfType(ApiException.class, () -> policy.validate(password));
        assertThat(e).as(password).isNotNull();
        return e.getCode();
    }
}
