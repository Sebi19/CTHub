package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ScrapedEventDetailsDto {
    private String contentHash;
    private String name;
    private LocalDate date;
    private LocalDate endDate;
    private String location;
    private String contactName;
    private String contactEmail;
    private List<ScrapedLinkDto> webLinks;

    private String resultsUrlPart;
    private String qualificationUrlPart;

    private List<ScrapedTeamDto> teams;
}