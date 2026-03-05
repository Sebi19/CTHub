package org.cthub.backend.dto.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.common.LinkDto;
import org.cthub.backend.dto.season.SeasonDto;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeasonTeamDto {
    private Long id;
    private SeasonDto season;
    private boolean active;
    private String fllId;

    private String name;
    private String institution;
    private String city;
    private String country;
    private List<LinkDto> links;

    private TeamProfileDto profile; // null if no profile exists


}
