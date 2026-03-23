import {Box, Group, type GroupProps, Text, ThemeIcon} from '@mantine/core';
import {IconMedal, IconStar, IconTrophy, type TablerIcon} from '@tabler/icons-react';
import {useTranslation} from 'react-i18next';
import {CompetitionAwardCategoryDto, type CompetitionNominationDto} from "../../../api/generated.ts";
import {getCategoryConfig} from "../../../utils/competitionUtils.ts";

interface TeamAchievementItemBaseProps extends Omit<GroupProps, 'children'> {
    category: CompetitionAwardCategoryDto;
    AchievementIcon: TablerIcon;
    variant: string;
    translation: string;
}

const TeamAchievementItemBase = ({ category, AchievementIcon, variant, translation, ...others }: TeamAchievementItemBaseProps) => {
    const { t } = useTranslation();

    const config = getCategoryConfig(category);

    return (
        <Group gap="sm" wrap="nowrap" {...others}>
            <ThemeIcon size={28} radius="xl" color={config.color} variant={variant}>
                <AchievementIcon size={16}/>
            </ThemeIcon>
            <Box>
                <Text size="sm" fw={600} lh={1.2} c={config.color}>
                    {t('app.competition.awards.category', { context: category })}
                </Text>
                <Text size="xs" c="dimmed" fw={700}>
                    {translation}
                </Text>
            </Box>
        </Group>
    );
};

interface AwardItemProps extends Omit<GroupProps, 'children'> {
    nomination: CompetitionNominationDto;
}

export const AwardItem = ({ nomination, ...others }: AwardItemProps) => {
    const { t } = useTranslation();
    const { category, winner } = nomination;

    return (
        <TeamAchievementItemBase
            category={category}
            AchievementIcon={winner ? IconTrophy : IconStar}
            variant={winner ? "outline" : "subtle"}
            translation={winner ? t("app.competition.awards.winner") : t("app.competition.awards.nominated")}
            {...others}
        />
    );
};

interface RobotGamePlaceItemProps extends Omit<GroupProps, 'children'> {
    rgPlace: number;
}

export const RobotGamePlaceItem = ({ rgPlace, ...others }: RobotGamePlaceItemProps) => {
    const { t } = useTranslation();

    return (
        <TeamAchievementItemBase
            category={CompetitionAwardCategoryDto.ROBOT_GAME}
            AchievementIcon={IconMedal}
            variant="subtle"
            translation={t("app.competition.awards.place", { count: rgPlace, ordinal: true })}
            {...others}
        />
    );
};