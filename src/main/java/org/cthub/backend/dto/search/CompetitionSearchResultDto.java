package org.cthub.backend.dto.search;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cthub.backend.dto.competition.CompetitionType;
import org.cthub.backend.dto.season.SeasonDto;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionSearchResultDto {
    @NotNull
    private SeasonDto season;
    @NotNull
    private String urlPart;
    @NotNull
    private String name;
    @NotNull
    private CompetitionType type;
    private String country;
    private LocalDate date;
    private LocalDate endDate;
}
