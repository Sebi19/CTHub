package org.cthub.backend.mail.provider.zeptomail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body for ZeptoMail's "send mail" API. Only the fields we actually use are mapped;
 * unknown properties are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ZeptoMailResponse(
    String message,
    List<Data> data,
    @JsonProperty("request_id") String requestId,
    String object,
    ZeptoMailError error
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
        String code,
        String message,
        @JsonProperty("message_id") String messageId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ZeptoMailError(
        String code,
        String message
    ) {
    }
}
