package org.cthub.backend.service.syncer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowPublicInfoDto {
    @JsonProperty("event_id")
    private Integer eventId;
    private String address;
    private List<FlowContactDto> contact;
    private FlowTeamsWrapperDto teams;
}
