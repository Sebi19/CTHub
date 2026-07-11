package org.cthub.backend.service.syncer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowEventDto {
    private Integer id; // The flow_id
    private String name;
    private String slug;
    private String date;
    private String endDate;

    @JsonProperty("event_challenge")
    private Integer challengeId; // Nullable if it's an explore-only event

    @JsonProperty("seasonRel")
    private FlowSeasonRelDto seasonRel;

    @JsonProperty("levelRel")
    private FlowLevelRelDto levelRel;
}
