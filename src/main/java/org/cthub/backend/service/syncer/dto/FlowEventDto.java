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

    private FlowProgramDto[] programs;

    @JsonProperty("seasonRel")
    private FlowSeasonRelDto seasonRel;

    @JsonProperty("levelRel")
    private FlowLevelRelDto levelRel;

    public FlowProgramDto getChallengeProgram() {
        if (programs == null) return null;
        for (FlowProgramDto program : programs) {
            if ("CHALLENGE".equalsIgnoreCase(program.getName())) {
                return program;
            }
        }
        return null;
    }
}
