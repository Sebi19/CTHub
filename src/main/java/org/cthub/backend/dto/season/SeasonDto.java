package org.cthub.backend.dto.season;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeasonDto {
    @NotNull
    private String id;
    @NotNull
    private String name;
    @NotNull
    private Integer startYear;
    @NotNull
    private boolean active;
}
