package org.cthub.backend.dto.auth;

import jakarta.validation.constraints.Size;

/** @param pushEndpoint this browser's own push endpoint, excluded from receiving the approval push */
public record PushEndpointRequestDto(@Size(max = 2048) String pushEndpoint) {
}