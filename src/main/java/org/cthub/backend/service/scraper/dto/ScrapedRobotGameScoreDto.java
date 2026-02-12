package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScrapedRobotGameScoreDto {
    private String fllId;

    // Preliminary Rounds (Vorrunden)
    private int run1;
    private int run2;
    private int run3;
    private int bestRun;

    // Knockout Rounds (Nullable)
    private Integer roundOf16;     // AF
    private Integer quarterFinal; // VF
    private Integer semiFinal;    // HF
    private Integer final1;       // F1
    private Integer final2;       // F2

    private Integer rank;
}