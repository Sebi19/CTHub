package org.cthub.backend.auth;

/**
 * Turns a User-Agent string into a short "Browser on OS" label for session lists and login
 * approvals. Deliberately coarse: it only has to help a person recognise their own device.
 */
public final class DeviceLabel {

    private DeviceLabel() {
    }

    public record Parts(String browser, String os) {
        public String label() {
            if (browser == null && os == null) {
                return null;
            }
            if (browser == null) {
                return os;
            }
            return os == null ? browser : browser + " on " + os;
        }
    }

    public static String of(String userAgent) {
        return parse(userAgent).label();
    }

    public static Parts parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new Parts(null, null);
        }
        return new Parts(browser(userAgent), os(userAgent));
    }

    private static String browser(String ua) {
        // Order matters: most browsers also claim to be Chrome and/or Safari.
        if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/")) {
            return "Edge";
        }
        if (ua.contains("OPR/") || ua.contains("Opera")) {
            return "Opera";
        }
        if (ua.contains("SamsungBrowser/")) {
            return "Samsung Internet";
        }
        if (ua.contains("Firefox/") || ua.contains("FxiOS/")) {
            return "Firefox";
        }
        if (ua.contains("CriOS/") || ua.contains("Chrome/") || ua.contains("Chromium/")) {
            return "Chrome";
        }
        if (ua.contains("Safari/") || ua.contains("AppleWebKit/")) {
            return "Safari";
        }
        return null;
    }

    private static String os(String ua) {
        if (ua.contains("iPhone")) {
            return "iPhone";
        }
        if (ua.contains("iPad")) {
            return "iPad";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("CrOS")) {
            return "ChromeOS";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return null;
    }
}
