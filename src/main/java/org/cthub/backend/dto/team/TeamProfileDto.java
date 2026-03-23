package org.cthub.backend.dto.team;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProfileDto {
    @NotNull
    private String profileName;
    @NotNull
    private String profileUrl;
    private String avatarUrl;
}
