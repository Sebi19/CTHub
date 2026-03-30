import {Card, Group, Text, Grid, Box, Divider, Anchor, type BoxProps} from '@mantine/core';
import {IconCalendar} from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import {Link, useNavigate} from 'react-router-dom';
import type {TeamCompetitionRecordDto} from "../../api/generated.ts";
import {getCompetitionLink} from "../../utils/routingUtils.ts";
import {PlaceBadge} from "../common/team/PlaceBadge.tsx";
import {TeamAchievementsStack} from "../common/team/TeamAchievementsStack.tsx";
import {AdvancingBadge} from "../common/team/AdvancingBadge.tsx";
import {TeamRobotgameOverview} from "../common/team/TeamRobotgameOverview.tsx";
import {getFormattedCompetitionDate} from "../../utils/competitionUtils.ts";

interface CompetitionRecordCardProps extends BoxProps {
    record: TeamCompetitionRecordDto;
}

export const CompetitionRecordCard = ({ record, ...other }: CompetitionRecordCardProps) => {
    const { t } = useTranslation();
    const comp = record.competition;
    const navigate = useNavigate();

    const resultsAvailable = record.competition.resultsAvailable;

    const place = record.place;
    const nominations = record.nominations;
    const robotGame = record.robotGame;

    return (
        <Card
            withBorder
            radius="md"
            p="md"
            shadow="sm"
            onClick={() => navigate(getCompetitionLink(comp))}
            style={{
                cursor: 'pointer',
                transition: 'transform 0.2s ease, box-shadow 0.2s ease'
            }}
            onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = 'var(--mantine-shadow-md)';
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = 'var(--mantine-shadow-sm)';
            }}
            {...other}
        >
            {/* Header: Competition Info & Overall Status */}
            <Group justify="space-between" mb="sm" wrap="nowrap" align="flex-start">
                <Box>
                    <Anchor
                        component={Link}
                        to={getCompetitionLink(comp)}
                        c="inherit"
                        underline="hover"
                        // Prevent the Anchor click from bubbling up and triggering the Card's onClick twice
                        onClick={(e) => e.stopPropagation()}
                    >
                        <Text
                            fw={700}
                            size="lg"
                            c="inherit"
                            style={{ textDecoration: 'none' }}
                        >
                            {comp.name}
                        </Text>
                    </Anchor>
                    <Group gap="xs" mt={4}>
                        <IconCalendar size={14} style={{ color: 'var(--mantine-color-dimmed)' }} />
                        <Text  size="xs" c="dimmed">{getFormattedCompetitionDate(comp)}</Text>
                    </Group>
                </Box>

                <Group gap="xs">
                    {place && (
                        <PlaceBadge placeObj={place} displayStyle="full"/>
                    )}
                    <AdvancingBadge competitionType={comp.type} advancing={place?.advancing} size="lg" nextCompetition={record.nextCompetition}/>

                </Group>
            </Group>

            <Divider my="sm" variant="dashed" />

            {!resultsAvailable && (
                <Text size="sm" c="dimmed" fs="italic">
                    {t('app.season_team.detail.results_not_available')}
                </Text>
            )}

            {resultsAvailable && (
                <Grid gutter="md">
                    {/* Left Column: Awards & Nominations */}
                    <Grid.Col span={{ base: 12, sm: 6 }}>
                        <Text size="sm" fw={600} c="dimmed" mb="xs" tt="uppercase">
                            {t('app.season_team.detail.awards.title')}
                        </Text>
                        {(nominations.length === 0) ? (
                            <Text size="sm" c="dimmed" fs="italic">{t('app.season_team.detail.awards.none')}</Text>
                        ) : (
                            <TeamAchievementsStack awards={nominations} robotGameEntry={robotGame} />
                        )}

                    </Grid.Col>

                    {/* Right Column: Robot Game */}
                    <Grid.Col span={{ base: 12, sm: 6 }}>
                        <Text size="sm" fw={600} c="dimmed" mb="xs" tt="uppercase">
                            {t('app.season_team.detail.robot_game.title')}
                        </Text>
                        {!record.robotGame ? (
                            <Text size="sm" c="dimmed" fs="italic">{t('app.season_team.detail.robot_game.none')}</Text>
                        ) : (
                            <TeamRobotgameOverview robotGameEntry={record.robotGame} />
                        )}
                    </Grid.Col>
                </Grid>
            )}
        </Card>
    );
};