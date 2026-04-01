package org.cthub.backend.dto.search;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProfileSearchResultDto {
    @NotNull
    private String profileName;
    @NotNull
    private String profileUrl;
    private String avatarUrl;
}
