package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ScrapedTeamDto {
    private String fllId;      // "2415"
    private String name;       // "RootBots"
    private String institution;// "School of Code"
    private String city;       // "Linz"
    private String country;    // "AT"
    private List<ScrapedLinkDto> links; // Social media links found in the popup
}