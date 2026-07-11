package org.cthub.backend.service.syncer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowLevelRelDto {
    private Integer id;
    private String name;
}
