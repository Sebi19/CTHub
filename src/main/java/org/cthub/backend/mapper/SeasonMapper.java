package org.cthub.backend.mapper;

import org.cthub.backend.dto.season.SeasonDto;
import org.cthub.backend.model.Season;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SeasonMapper {
    SeasonDto toSeasonDto(Season season);
}
