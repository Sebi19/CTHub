package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

/** @param user set when {@link LoginResultStatus#SIGNED_IN} */
public record LoginResultDto(@NotNull LoginResultStatus status, UserDto user) {
}