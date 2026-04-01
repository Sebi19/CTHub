import {useMemo} from 'react';
import {
    Text,
    Stack,
    Box,
    Anchor,
    Card,
    Group,
    Divider,
    TooltipGroup,
    Tooltip,
    Center,
    SegmentedControl, SimpleGrid,
} from '@mantine/core';
import {IconLayoutGrid, IconList} from '@tabler/icons-react';
import {type CompetitionDetailDto, type SeasonTeamDto} from '../../api/generated';
import {Link, useNavigate} from "react-router-dom";
import {getTeamLink} from "../../utils/routingUtils.ts";
import {getTeamAchievements} from "../../utils/competitionUtils.ts";
import {SeasonTeamAvatar} from "../common/team/avatar/SeasonTeamAvatar.tsx";
import {useTranslation} from "react-i18next";
import {useSessionStorage} from "@mantine/hooks";
import {TeamRobotgameOverview} from "../common/team/TeamRobotgameOverview.tsx";
import {RobotGameTableView} from "./RobotgameTableView.tsx";

interface Props {
    competition: CompetitionDetailDto;
}

type ViewMode = 'teams' | 'table';

export const CompetitionRobotGameTab = ({ competition }: Props) => {
    const {t} = useTranslation();
    const results = competition.results;
    const scores = results?.robotGameEntries || [];
    const teams = competition.registeredTeams || [];
    const navigate = useNavigate();

    if (!results) {
        return <Text c="dimmed" ta="center" py="xl">{t("app.competition.robot_game.empty")}</Text>;
    }

    const [viewMode, setViewMode] = useSessionStorage<string | undefined>({
        key: `competition-robotgame-view-${competition.season?.id}-${competition.urlPart}`,
        defaultValue: 'teams',
    });

    const sortedTeams = useMemo(() => {
        return [...teams].sort((a, b) => {
            const rankA = scores.find(s => s.teamId === a.id)?.prelimRank || 0;
            const rankB = scores.find(s => s.teamId === b.id)?.prelimRank || 0;
            return rankA - rankB; // Descending order
        }).filter(
            team => scores.some(score => score.teamId === team.id) // Only include teams that have scores
        );
    }, [teams])

    const TeamRobotGameCard = ({ team }: { team: SeasonTeamDto }) => {
        const {  robotGameEntry } = getTeamAchievements(team.id, results);

        return (
            <Card
                withBorder
                radius="md"
                p="md"
                shadow="sm"
                onClick={() => navigate(getTeamLink(team))}
                style={{ cursor: 'pointer', transition: 'transform 0.2s ease, box-shadow 0.2s ease' }}
                onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'translateY(-2px)';
                    e.currentTarget.style.boxShadow = 'var(--mantine-shadow-md)';
                }}
                onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = 'var(--mantine-shadow-sm)';
                }}
            >
                <Group justify="space-between" mb="xs" wrap="nowrap" align="flex-start">
                    <Box style={{ flex: 1 }}>
                        <Group gap="xs">
                            <SeasonTeamAvatar team={team}/>
                            <Stack gap={0}>
                                <Anchor
                                    component={Link}
                                    to={getTeamLink(team)}
                                    c="inherit"
                                    underline="hover"
                                    // Prevent the Anchor click from bubbling up and triggering the Card's onClick twice
                                    onClick={(e) => e.stopPropagation()}
                                >
                                    <Text fw={700} lineClamp={1} size={"lg"}>{team.name}</Text>
                                </Anchor>
                                <Text size="xs" c="dimmed">#{team.fllId}</Text>
                            </Stack>
                        </Group>


                    </Box>
                    {/* Optional Badge */}
                </Group>

                <Divider my="sm" variant="dashed" />

                {robotGameEntry && (
                    <TeamRobotgameOverview robotGameEntry={robotGameEntry} />
                )}
            </Card>
        )
    };

    return (
        <Stack gap="xl" mt="md">
            <Group justify="space-between" align="center">
                <TooltipGroup openDelay={500} closeDelay={100}>
                    <SegmentedControl
                        //visibleFrom="sm"
                        value={viewMode}
                        onChange={(val) => setViewMode(val as ViewMode)}
                        styles={{ label: { padding: 0 } }}
                        data={[
                            {
                                value: 'teams',
                                label: (
                                    <Tooltip label={t("app.competition.awards.tooltip.view.teams")}>
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconLayoutGrid size={18} /></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            },
                            {
                                value: 'table',
                                label: (
                                    <Tooltip label={t("app.competition.awards.tooltip.view.matrix")}>
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconList size={18} /></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            }
                        ]}
                    />
                </TooltipGroup>
            </Group>

            {(viewMode === 'teams') && (
                <Box
                    //hiddenFrom={viewMode === 'table' ? "sm" : undefined}
                >
                    <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
                        {sortedTeams.map(team => (
                            <TeamRobotGameCard key={team.id} team={team} />
                        ))}
                    </SimpleGrid>
                </Box>
            )}

            {viewMode === 'table' && (
                    <RobotGameTableView scores={scores} teams={teams} />
                )}
        </Stack>

    );
};