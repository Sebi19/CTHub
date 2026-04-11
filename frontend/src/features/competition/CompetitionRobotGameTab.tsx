import {useMemo, useState} from 'react';
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
    SegmentedControl, SimpleGrid, Select, Loader,
} from '@mantine/core';
import {IconLayoutGrid, IconList, IconSortAscending} from '@tabler/icons-react';
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
type SortMode = 'prelim' | 'playoff';

export const CompetitionRobotGameTab = ({ competition }: Props) => {
    const {t} = useTranslation();
    const results = competition.results;
    const scores = results?.robotGameEntries || [];
    const teams = competition.registeredTeams || [];
    const navigate = useNavigate();

    const [viewMode, setViewMode] = useSessionStorage<string | undefined>({
        key: `competition-robotgame-view-${competition.season?.id}-${competition.urlPart}`,
        defaultValue: 'teams',
    });

    const [sortMode, setSortMode] = useSessionStorage<SortMode>({
        key: `competition-robotgame-sort-${competition.season?.id}-${competition.urlPart}`,
        defaultValue: 'prelim',
    });

    const [teamCardsLoading, setTeamCardsLoading] = useState(false);

    const handleSortModeChange = (mode: SortMode) => {
        setSortMode(mode);
        setTeamCardsLoading(true);
        setTimeout(() => setTeamCardsLoading(false), 100); // Simulate loading delay for smoother transition
    }

    const sortedTeams = useMemo(() => {
        return [...teams]
            .filter(team => scores.some(score => score.teamId === team.id))
            .sort((a, b) => {
                const scoreA = scores.find(s => s.teamId === a.id);
                const scoreB = scores.find(s => s.teamId === b.id);

                // Determine which rank to use based on sortMode
                const rankA = (sortMode === 'playoff' ? scoreA?.rank : scoreA?.prelimRank) || 999;
                const rankB = (sortMode === 'playoff' ? scoreB?.rank : scoreB?.prelimRank) || 999;

                return rankA - rankB;
            });
    }, [teams, scores, sortMode]);

    if (!results) {
        return <Text c="dimmed" ta="center" py="xl">{t("app.competition.robot_game.empty")}</Text>;
    }

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
                {(viewMode === 'teams') && (
                    <Select
                        leftSection={<IconSortAscending size={16} />}
                        value={sortMode}
                        variant='default'
                        onChange={(val) => handleSortModeChange(val as SortMode)}
                        allowDeselect={false}
                        data={[
                            { value: 'prelim', label: t('app.competition.robot_game.sort.prelim') },
                            { value: 'playoff', label: t('app.competition.robot_game.sort.playoff') },
                        ]}
                        style={{ width: 220 }}
                        styles={{
                            input: {
                                // This is the specific "Zinc 800" color used in Shadcn Dark
                                borderColor: '#27272a',
                                backgroundColor: 'transparent',
                                color: 'var(--mantine-color-white)',
                                transition: 'border-color 0.2s ease, box-shadow 0.2s ease',
                            },
                            // This removes the heavy blue/white "glow" that makes it look focused
                            section: {
                                color: 'var(--mantine-color-dark-2)',
                            }
                        }}
                        onFocus={(e) => {
                            e.currentTarget.style.borderColor = '#3f3f46'; // Zinc 700 on focus
                        }}
                        onBlur={(e) => {
                            e.currentTarget.style.borderColor = '#27272a';
                        }}
                    />
                )}
            </Group>

            {(viewMode === 'teams') && (
                <Stack gap="md"
                    //hiddenFrom={viewMode === 'table' ? "sm" : undefined}
                >
                    {teamCardsLoading && (
                        <Center py="xl">
                            <Loader variant="dots" />
                        </Center>
                    )}
                    {!teamCardsLoading && (
                        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
                            {sortedTeams.map(team => (
                                <TeamRobotGameCard key={team.id} team={team} />
                            ))}
                        </SimpleGrid>
                    )}
                </Stack>
            )}

            {viewMode === 'table' && (
                    <RobotGameTableView scores={scores} teams={teams} />
                )}
        </Stack>

    );
};