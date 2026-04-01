package org.cthub.backend.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultItemDto {
    private SearchResultTypeDto type;
    private Double score;

    private CompetitionSearchResultDto competition;
    private SeasonTeamSearchResultDto seasonTeam;
    private TeamProfileSearchResultDto teamProfile;
    private SeasonSearchResultDto season;
}
