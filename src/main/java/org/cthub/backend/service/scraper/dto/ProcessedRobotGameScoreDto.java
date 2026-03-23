package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcessedRobotGameScoreDto {
    private String fllId;

    private ScrapedRobotGameScoreDto robotGameScoreDto;

    private int bestRun;
    private int secondBestRun;
    private int thirdBestRun;

    private Integer prelimRank;
}
