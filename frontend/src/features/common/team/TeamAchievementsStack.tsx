import {
    CompetitionAwardCategoryDto,
    type CompetitionNominationDto,
    type CompetitionRobotGameEntryDto
} from "../../../api/generated.ts";
import {AwardItem, RobotGamePlaceItem} from "./TeamAchievementItem.tsx";
import {Stack, type StackProps} from "@mantine/core";

interface TeamAchievementsStackProps extends Omit<StackProps, 'children'> {
    awards: CompetitionNominationDto[];
    robotGameEntry?: CompetitionRobotGameEntryDto;
}

const relevantCategories = [
    CompetitionAwardCategoryDto.RESEARCH,
    CompetitionAwardCategoryDto.ROBOT_DESIGN,
    CompetitionAwardCategoryDto.CORE_VALUES,
    CompetitionAwardCategoryDto.CHAMPION,
    CompetitionAwardCategoryDto.ROBOT_GAME];

const categoryScores: Partial<Record<CompetitionAwardCategoryDto, number>> = {
    [CompetitionAwardCategoryDto.RESEARCH]: 3,
    [CompetitionAwardCategoryDto.ROBOT_DESIGN]: 2,
    [CompetitionAwardCategoryDto.CORE_VALUES]: 1,
    [CompetitionAwardCategoryDto.ROBOT_GAME]: 0.5
};

export const TeamAchievementsStack = ({awards, robotGameEntry, ...others}: TeamAchievementsStackProps) => {
    const relevantNominations = awards.filter(a => relevantCategories.includes(a.category)).sort(
        (a, b) => {
            const winnerA = a.winner && a.category !== CompetitionAwardCategoryDto.ROBOT_GAME ? 1 : 0;
            const winnerB = b.winner && b.category !== CompetitionAwardCategoryDto.ROBOT_GAME ? 1 : 0;
            if (winnerA !== winnerB) return winnerB - winnerA; // Winners first

            const scoreA = categoryScores[a.category] || 0;
            const scoreB = categoryScores[b.category] || 0;
            return scoreB - scoreA; // Higher score first
        }
    );

    const rgPlace = robotGameEntry?.rank;

    const coachingAward = awards.find(a => a.category === CompetitionAwardCategoryDto.COACHING && a.winner);

    return (
        <Stack gap="xs" {...others}>
            {relevantNominations.map(nom => (
                <AwardItem key={nom.category} nomination={nom} />
            ))}
            {rgPlace && rgPlace >= 2 && rgPlace <= 3 && (
                <RobotGamePlaceItem rgPlace={rgPlace} />
            )}
            {coachingAward && (
                <AwardItem nomination={coachingAward} />
            )}
        </Stack>
    )
}