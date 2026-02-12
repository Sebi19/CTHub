package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScrapedPlaceDto {
    private String teamName; // Again, matching by name is necessary here
    private int place;       // 1, 2, 3...
    private boolean advancing; // true if they qualified for next round
}