package org.cthub.backend.mapper;

import org.cthub.backend.dto.robotgame.OverallRobotGameEntryDto;
import org.cthub.backend.model.RobotGameResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CompetitionMapper.class})
public interface RobotGameEntryMapper {
    @Mapping(target = "teamName", source = "result.seasonTeam.name")
    @Mapping(target = "teamId", source = "result.seasonTeam.fllId")
    @Mapping(target = "competition", source = "result.competition")
    @Mapping(target = "country", source = "result.seasonTeam.country")
    @Mapping(target = "bestScore", expression = "java(calculateBestScore(result))")
    @Mapping(target = "medianScore", expression = "java(calculateMedianScore(result))")
    @Mapping(target = "averageScore", expression = "java(calculateAverageScore(result))")
    @Mapping(target = "worstScore", expression = "java(calculateWorstScore(result))")
    @Mapping(target = "preliminaryRound1", source = "result.pr1")
    @Mapping(target = "preliminaryRound2", source = "result.pr2")
    @Mapping(target = "preliminaryRound3", source = "result.pr3")
    @Mapping(target = "bestPreliminaryScore", source = "result.bestPr")
    @Mapping(target = "roundOf16", source = "result.r16")
    @Mapping(target = "quarterFinal", source = "result.qf")
    @Mapping(target = "semiFinal", source = "result.sf")
    @Mapping(target = "final1", source = "result.f1")
    @Mapping(target = "final2", source = "result.f2")
    OverallRobotGameEntryDto toOverallRobotGameEntryDto(RobotGameResult result, boolean qualified);

    // Helper to get list of relevant scores for further calculations
    default List<Integer> extractAllScores(RobotGameResult result) {
        List<Integer> scores = new ArrayList<>();
        scores.add(result.getPr1());
        scores.add(result.getPr2());
        scores.add(result.getPr3());
        if (result.getR16() != null) scores.add(result.getR16());
        if (result.getQf() != null) scores.add(result.getQf());
        if (result.getSf() != null) scores.add(result.getSf());
        if (result.getF1() != null) scores.add(result.getF1());
        if (result.getF2() != null) scores.add(result.getF2());
        return scores;
    }

    default int calculateBestScore(RobotGameResult result) {
        return extractAllScores(result).stream().max(Integer::compare).orElse(0);
    }

    default int calculateWorstScore(RobotGameResult result) {
        return extractAllScores(result).stream().min(Integer::compare).orElse(0);
    }

    default double calculateAverageScore(RobotGameResult result) {
        List<Integer> scores = extractAllScores(result);
        double avg = scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(avg * 10.0) / 10.0; // Round to 1 decimal place
    }

    default double calculateMedianScore(RobotGameResult result) {
        List<Integer> scores = extractAllScores(result);
        if (scores.isEmpty()) return 0.0;
        scores.sort(Integer::compare);
        int size = scores.size();
        if (size % 2 == 0) {
            return (scores.get(size / 2 - 1) + scores.get(size / 2)) / 2.0;
        } else {
            return scores.get(size / 2);
        }
    }
}
