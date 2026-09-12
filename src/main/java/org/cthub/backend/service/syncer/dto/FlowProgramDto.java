package org.cthub.backend.service.syncer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowProgramDto {
    private Integer id;
    @JsonProperty("draht_id")
    private Integer drahtId;
    private Integer event;
    private String name;
}