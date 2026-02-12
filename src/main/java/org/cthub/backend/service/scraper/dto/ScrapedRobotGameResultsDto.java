package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ScrapedRobotGameResultsDto {
    private String contentHash;
    private List<ScrapedRobotGameScoreDto> scores;
}