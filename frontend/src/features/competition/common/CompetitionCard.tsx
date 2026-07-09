import { Card, Text, Group, Stack, Progress } from '@mantine/core';
import { IconCalendar } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {getCompetitionLink} from "../../../utils/routingUtils.ts";
import type {CompetitionShortInfoDto} from "../../../api/generated.ts";
import {
    getCompetitionTypeColor,
    getFormattedCompetitionDate
} from "../../../utils/competitionUtils.ts";
import {CompetitionTypeBadge} from "../../common/competition/CompetitionTypeBadge.tsx";
import {CountryBadge} from "../../common/CountryBadge.tsx";

// Creating a flexible type that works for both the old Dto and your new ShortInfoDto
interface CompetitionCardProps {
    competition: CompetitionShortInfoDto;
}

export const CompetitionCard = ({ competition }: CompetitionCardProps) => {
    const { t } = useTranslation();
    const navigate = useNavigate();

    // Calculate capacity details if present
    const hasCapacity = competition.maxTeamCount !== undefined && competition.maxTeamCount > 0;
    const registered = competition.registeredTeamCount || 0;
    const max = competition.maxTeamCount || 0;
    const capacityPercentage = hasCapacity ? (registered / max) * 100 : 0;

    // Grab the theme color once to use for both the badge and the progress bar
    const themeColor = getCompetitionTypeColor(competition.type);

    return (
        <Card
            onClick={() => navigate(getCompetitionLink(competition))}
            withBorder
            shadow="sm"
            radius="md"
            p="md"
            style={{
                cursor: 'pointer',
                transition: 'transform 0.2s ease, box-shadow 0.2s ease',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                height: '100%'
            }}
            onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = 'var(--mantine-shadow-md)';
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'none';
                e.currentTarget.style.boxShadow = 'var(--mantine-shadow-sm)';
            }}
        >
            <Stack gap="sm" style={{ flexGrow: 1 }}>
                <Group justify="space-between" wrap="nowrap">
                    <CompetitionTypeBadge type={competition.type} />
                    <CountryBadge country={competition.country} />
                </Group>

                <Text fw={700} size="lg" lineClamp={2}>{competition.name}</Text>

                {competition.date && (
                    <Group gap="xs" c="dimmed" mt="auto">
                        <IconCalendar size={16} />
                        <Text size="sm">{getFormattedCompetitionDate(competition)}</Text>
                    </Group>
                )}
            </Stack>

            {/* Visual Capacity Bar - Now using theme color */}
            {hasCapacity && (
                <Stack gap="xs" mt="md">
                    <Group justify="space-between" align="center">
                        <Text size="xs" c="dimmed">{t('app.competition.overview.capacity') || 'Capacity'}</Text>
                        <Text size="xs" fw={500} c="dimmed">
                            {registered} / {max} {t('app.competition.overview.teams') || 'Teams'}
                        </Text>
                    </Group>
                    <Progress
                        value={capacityPercentage}
                        color={themeColor}
                        size="sm"
                        radius="xl"
                    />
                </Stack>
            )}
        </Card>
    );
};