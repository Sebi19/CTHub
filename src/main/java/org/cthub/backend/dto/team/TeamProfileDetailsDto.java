package org.cthub.backend.dto.team;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProfileDetailsDto {
    @NotNull
    private String profileName;
    @NotNull
    private String profileUrl;
    private String avatarUrl;

    @NotNull
    private List<SeasonTeamDetailsDto> seasons;
}
