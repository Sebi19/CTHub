package org.cthub.backend.dto.competition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.season.SeasonDto;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionShortInfoDto {
    private Long id;
    private SeasonDto season;
    private String name;
    private String urlPart;

    private CompetitionType type;
    boolean active;

    private String country;
    private LocalDate date;
}
