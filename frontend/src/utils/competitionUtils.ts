import {
    CompetitionAwardCategoryDto,
    type CompetitionResultsDto, type CompetitionSearchResultDto,
    CompetitionType
} from "../api/generated.ts";
import {
    IconBulb,
    IconChevronsUp,
    IconCrown, IconHeartHandshake,
    IconHomeStats,
    IconQuestionMark, IconRobot, IconSettings,
    IconTrophy, IconUsers,
    type TablerIcon
} from "@tabler/icons-react";
import type {MantineColor} from "@mantine/core";
import {checkExhaustive} from "./typeUtils.ts";
import dayjs from "dayjs";

/**
 * Returns the standardized Mantine color for a competition type.
 */
export const getCompetitionTypeColor = (type: CompetitionType): MantineColor => {
    switch (type) {
        case CompetitionType.REGIONAL:
            return 'green';
        case CompetitionType.QUALIFICATION:
            return 'orange';
        case CompetitionType.FINAL:
            return 'red';
        default:
            checkExhaustive(type);
            return 'gray';
    }
};

/**
 * Returns the standardized Tabler icon for a competition type.
 */
export const getCompetitionTypeIcon = (type: CompetitionType): TablerIcon => {
    switch (type) {
        case CompetitionType.REGIONAL:
            return IconHomeStats;
        case CompetitionType.QUALIFICATION:
            return IconChevronsUp;
        case CompetitionType.FINAL:
            return IconCrown;
        default:
            checkExhaustive(type);
            return IconQuestionMark;
    }
};

export const getTeamAchievements = (teamId: number, results: CompetitionResultsDto) => {
    const place = results.places.find(p => p.teamId === teamId);
    const awards = results.nominations.filter(n => n.teamId === teamId);
    const robotGameEntry = results.robotGameEntries.find(e => e.teamId === teamId);
    return { place, awards, robotGameEntry };
};

export interface CategoryConfig {
    CategoryIcon: TablerIcon;
    color: MantineColor;
}

export const getCategoryConfig = (category: CompetitionAwardCategoryDto): CategoryConfig => {
    switch (category) {
        case CompetitionAwardCategoryDto.CHAMPION:
            return { CategoryIcon: IconTrophy, color: 'red' }
        case CompetitionAwardCategoryDto.RESEARCH:
            return { CategoryIcon: IconBulb, color: 'green' };
        case CompetitionAwardCategoryDto.ROBOT_DESIGN:
            return { CategoryIcon: IconSettings, color: 'orange' };
        case CompetitionAwardCategoryDto.CORE_VALUES:
            return { CategoryIcon: IconUsers, color: 'grape' };
        case CompetitionAwardCategoryDto.ROBOT_GAME:
            return { CategoryIcon: IconRobot, color: 'blue' };
        case CompetitionAwardCategoryDto.COACHING:
            return { CategoryIcon: IconHeartHandshake, color: 'lime' };
        default:
            checkExhaustive(category);
            return { CategoryIcon: IconQuestionMark, color: 'gray' };
    }
};

export const getFormattedCompetitionDate = (competition: CompetitionSearchResultDto): string => {
    if (!competition.date) return 'N/A';

    if (!competition.endDate) {
        return dayjs(competition.date).format('L');
    }

    return `${dayjs(competition.date).format('L')} - ${dayjs(competition.endDate).format('L')}`;
}