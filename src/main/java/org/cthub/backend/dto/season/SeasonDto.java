package org.cthub.backend.dto.season;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeasonDto {
    private String id;
    private String name;
    private Integer startYear;
    private boolean active;
}
