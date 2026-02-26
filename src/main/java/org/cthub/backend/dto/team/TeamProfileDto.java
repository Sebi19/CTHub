package org.cthub.backend.dto.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProfileDto {
    private String profileName;
    private String profileUrl;
}
