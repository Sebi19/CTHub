package org.cthub.backend.dto.competition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.common.LinkDto;
import org.cthub.backend.dto.season.SeasonDto;
import org.cthub.backend.dto.team.SeasonTeamDto;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionDetailDto {
    private Long id;
    private SeasonDto season;
    private String name;
    private String urlPart;

    private CompetitionType type;
    boolean active;

    private String country;
    private LocalDate date;
    private CompetitionContactInfoDto contactInfo;
    private String location;

    private List<LinkDto> links;

    private List<SeasonTeamDto> registeredTeams;
    private CompetitionResultsDto results; // null if results not published yet


    private int registeredTeamCount;
    private int maxTeamCount;
}