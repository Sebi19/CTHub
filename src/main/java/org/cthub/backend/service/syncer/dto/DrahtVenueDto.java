package org.cthub.backend.service.syncer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrahtVenueDto {
    private Integer id; // Matches FlowEventDto.challengeId
    private String country;
    private Double lat;
    private Double lon;
    private String date;
    private String endDate;
    private Integer capacity;
    private Integer registered;
}