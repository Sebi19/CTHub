package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ScrapedAwardsAndRanksDto {
    private String contentHash;
    // The "Champion" Logic (Winner + Advancing Teams)
    private List<ScrapedPlaceDto> places;

    // The Category Awards (Research, Design, Core Values, Robot Game)
    private List<ScrapedNominationDto> nominations;
}