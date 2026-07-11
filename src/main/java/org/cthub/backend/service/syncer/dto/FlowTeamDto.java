package org.cthub.backend.service.syncer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowTeamDto {
    @JsonProperty("team_number_hot")
    private String fllId;
    private String name;
    private String organization;
    private String location; // Mapped to City
}