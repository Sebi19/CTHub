package org.cthub.backend.service.syncer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowTeamsWrapperDto {
    private FlowTeamsLaneDto[] lanes;

    public FlowTeamsLaneDto getChallengeLane() {
        for (FlowTeamsLaneDto lane : lanes) {
            if ("Challenge".equalsIgnoreCase(lane.getName())) {
                return lane;
            }
        }
        return null;
    }
}
