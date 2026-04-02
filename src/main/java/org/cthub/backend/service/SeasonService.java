package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.season.SeasonDto;
import org.cthub.backend.mapper.SeasonMapper;
import org.cthub.backend.model.Season;
import org.cthub.backend.repository.SeasonRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonService {
    private final SeasonRepository seasonRepository;
    private final SeasonMapper seasonMapper;

    @Cacheable("seasons")
    public List<SeasonDto> getAllSeasons() {
        return seasonRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(Season::getStartYear, Comparator.reverseOrder()))
            .map(seasonMapper::toSeasonDto)
            .toList();
    }
}
