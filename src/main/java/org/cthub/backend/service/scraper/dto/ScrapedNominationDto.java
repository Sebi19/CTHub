package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;
import org.cthub.backend.model.Nomination; // Reuse your existing Enum

@Data
@Builder
public class ScrapedNominationDto {
    private String teamName; // Here we MUST use name, because ID is rarely on the awards list
    private Nomination.AwardCategory category;
    private boolean isWinner; // true = Winner, false = Nominee
}