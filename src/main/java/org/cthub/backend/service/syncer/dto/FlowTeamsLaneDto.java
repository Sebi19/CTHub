package org.cthub.backend.service.syncer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowTeamsLaneDto {
    private String name;
    private List<FlowTeamDto> teams;
}
