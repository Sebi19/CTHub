package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;
import org.cthub.backend.model.Competition;

@Data
@Builder
public class ScrapedEventOverviewDto {
    private String name;
    private String urlPart;
    private String country; // "DE", "AT", "CH", or null
    private Competition.CompetitionType type; // REGIONAL, QUALIFICATION, FINAL
    private int registeredTeamCount;
    private int maxTeamCount;
}
