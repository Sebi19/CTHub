package org.cthub.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Application-level failure carrying a stable machine-readable {@code code} alongside the HTTP
 * status. Throwing this instead of returning an error {@code ResponseEntity} lets controllers
 * declare their happy-path type concretely, which in turn gives springdoc - and therefore the
 * generated TypeScript client - a real schema instead of {@code object}.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static ApiException tooManyRequests(String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", message);
    }
}
