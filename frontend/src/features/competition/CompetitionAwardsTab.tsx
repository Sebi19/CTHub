import {
    Anchor,
    Badge,
    Box,
    Card,
    Center,
    Divider,
    Group,
    SegmentedControl,
    SimpleGrid,
    Stack,
    Table,
    Text,
    ThemeIcon,
    Title,
    Tooltip,
    TooltipGroup
} from '@mantine/core';
import {IconLayoutBoard, IconLayoutGrid, IconList, IconMedal, IconStar, IconTrophy} from '@tabler/icons-react';
import {CompetitionAwardCategoryDto, type CompetitionDetailDto, type SeasonTeamDto} from '../../api/generated';
import {Link, useNavigate} from "react-router-dom";
import {useMemo} from "react";
import {useTranslation} from "react-i18next";
import {getTeamLink} from "../../utils/routingUtils.ts";
import {PlaceBadge} from "../common/team/PlaceBadge.tsx";
import {useSessionStorage} from "@mantine/hooks";
import {TeamAchievementsStack} from "../common/team/TeamAchievementsStack.tsx";
import {getCategoryConfig, getTeamAchievements} from "../../utils/competitionUtils.ts";
import {AdvancingBadge} from "../common/team/AdvancingBadge.tsx";
import {SeasonTeamAvatar} from "../common/team/avatar/SeasonTeamAvatar.tsx";

interface Props {
    competition: CompetitionDetailDto
}

interface RgAdditionalPlace extends SeasonTeamDto {
    rank: number;
}

type ViewMode = 'categories' | 'teams' | 'matrix';

export const CompetitionAwardsTab = ({ competition }: Props) => {
    const { t } = useTranslation();
    const results = competition.results;
    const teams = competition.registeredTeams;
    const navigate = useNavigate();

    if (!results) {
        return <Text c="dimmed" ta="center" py="xl">{t("app.competition.awards.empty")}</Text>;
    }

    const [viewMode, setViewMode] = useSessionStorage<string | undefined>({
        key: `competition-awards-view-${competition.season.id}-${competition.urlPart}`,
        defaultValue: 'categories',
    });


    const relevantCategories = ['RESEARCH', 'ROBOT_DESIGN', 'CORE_VALUES', 'CHAMPION'];
    const categoryScores = {'RESEARCH': 3, 'ROBOT_DESIGN': 2, 'CORE_VALUES': 1, 'ROBOT_GAME': 0.5};


    // Helper: Safely get team data
    const getTeam = (teamId: number) => teams.find(t => t.id === teamId);

    // Helper: Get winner for a specific category
    const getWinners = (category: string) => {
        return results.nominations
            .filter(n => n.category === category && n.winner)
            .map(n => getTeam(n.teamId))
            .sort((a, b) => {
                const fllA = parseInt(a?.fllId as any) || 9999;
                const fllB = parseInt(b?.fllId as any) || 9999;
                return fllA - fllB;
            })
            .filter(Boolean) as SeasonTeamDto[];
    };

    // Helper: Get the other nominees (who didn't win) for a category
    const getNominees = (category: string) => {
        return results.nominations
            .filter(n => n.category === category && !n.winner)
            .map(n => getTeam(n.teamId))
            //sort nominees by FLL ID ascending
            .sort((a, b) => {
                const fllA = parseInt(a?.fllId as any) || 9999;
                const fllB = parseInt(b?.fllId as any) || 9999;
                return fllA - fllB;
            })
            .filter(Boolean) as SeasonTeamDto[];
    };

    const getRobotGameAdditionalPlaces = () => {
        return results.robotGameEntries
            .filter(e => e.rank && e.rank > 1 && e.rank <= 3)
            .map(e => {
                const team = getTeam(e.teamId);
                if(!team) return null;

                return {
                    ...team,
                    rank: e.rank
                } as RgAdditionalPlace;
            })
            .filter((item): item is RgAdditionalPlace => item !== null)
            .sort((a, b) => {
                const rankA = results.robotGameEntries.find(e => e.teamId === a.id)?.rank || 999;
                const rankB = results.robotGameEntries.find(e => e.teamId === b.id)?.rank || 999;
                if(rankA !== rankB) return rankA - rankB;

                const fllA = parseInt(a?.fllId as any) || 9999;
                const fllB = parseInt(b?.fllId as any) || 9999;
                return fllA - fllB;
            })
    }

    // Reusable Card Component for the Grid
    const AwardCard = ({ category }: { category: CompetitionAwardCategoryDto }) => {
        const winners = getWinners(category);
        const nominees = getNominees(category);
        const config = getCategoryConfig(category);
        const rgAdditionalPlaces = getRobotGameAdditionalPlaces();

        if (winners.length === 0 && nominees.length === 0) return null; // Hide if no data yet

        return (
            <Card withBorder radius="md" p="md" shadow="sm">
                <Group gap="sm" mb="md">
                    <ThemeIcon size={40} radius="md" color={config.color} variant="light">
                        <config.CategoryIcon size={24} />
                    </ThemeIcon>
                    <Text fw={700} size="lg">{t('app.competition.awards.category', {context: category})}</Text>
                </Group>

                {winners.length > 0 ? (
                    <Box mb={nominees.length > 0 ? 'md' : 0}>
                        <Text size="sm" c="dimmed" tt="uppercase" fw={700} mb={"xs"}>{t("app.competition.awards.winner")}</Text>
                        <Stack gap={4}>
                            {winners.map(winner => (
                                <Group gap="xs" key={winner.id}>
                                    <ThemeIcon color={config.color} variant={"outline"} radius="xl" size={28}>
                                        <IconTrophy size={16}/>
                                    </ThemeIcon>
                                    <Group gap={4} align={"baseline"}>
                                        <Anchor
                                            component={Link}
                                            to={getTeamLink(winner)}
                                            c="inherit"
                                            underline="hover"
                                            fw={600}
                                        >
                                            {winner.name}
                                        </Anchor>
                                        <Text span c="dimmed" size="sm" fw={400}>[{winner.fllId}]</Text>
                                    </Group>
                                </Group>
                            ))}
                        </Stack>
                    </Box>
                ) : (
                    <Text c="dimmed" fs="italic" size="sm">Gewinner noch nicht bekannt</Text>
                )}

                {nominees.length > 0 && (
                    <>
                        <Divider my="sm" variant="dashed" />
                        <Text size="xs" c="dimmed" tt="uppercase" fw={700} mb="xs">{t("app.competition.awards.additional_nominations")}</Text>
                        <Stack gap={4}>
                            {nominees.map(nom => (
                                <Group gap={"xs"} key={nom.id}>
                                    <ThemeIcon color={config.color} variant={"subtle"} radius="xl">
                                        <IconStar size={16}/>
                                    </ThemeIcon>
                                    <Group key={nom.id} gap={4} align="baseline">
                                        <Anchor
                                            component={Link}
                                            to={getTeamLink(nom)}
                                            c="inherit"
                                            underline="hover"
                                            size={'sm'}
                                        >
                                            {nom.name}
                                        </Anchor>
                                        <Text span c="dimmed" size="xs">[{nom.fllId}]</Text>
                                    </Group>

                                </Group>

                            ))}
                        </Stack>
                    </>
                )}

                {category === 'ROBOT_GAME' && (
                    <>
                        <Divider my="sm" variant="dashed" />
                        <Text size="xs" c="dimmed" tt="uppercase" fw={700} mb="md">{t("app.competition.awards.additional_places")}</Text>
                        <Stack gap={10}>
                            {rgAdditionalPlaces.map(rgPlace => (
                                <Group gap={"xs"} key={rgPlace.id}>
                                    <Tooltip label={t("app.competition.awards.place", { count: rgPlace.rank, ordinal: true })}>
                                        <Badge size={"lg"} leftSection={<IconMedal size={16}></IconMedal>} color={config.color} variant={"light"}>
                                            {rgPlace.rank}
                                        </Badge>
                                    </Tooltip>
                                    <Group gap={4} align="baseline">
                                        <Anchor
                                            component={Link}
                                            to={getTeamLink(rgPlace)}
                                            c="inherit"
                                            underline="hover"
                                            size={'sm'}
                                        >
                                            {rgPlace.name}
                                        </Anchor>
                                        <Text span c="dimmed" size="xs">[{rgPlace.fllId}]</Text>
                                    </Group>
                                </Group>
                            ))}
                        </Stack>
                    </>
                )}
            </Card>
        );
    };

    // Ensure places are sorted 1, 2, 3...
    const sortedPlaces = [...(results.places || [])].sort((a, b) => (a.place || 99) - (b.place || 99));

    // --- TABLE VIEW LOGIC --- //

    // 1. Sort teams so the best performing teams are at the top of the table!
    const tableTeams = useMemo(() => {
        return [...teams].sort((a, b) => {
            // Primary sort: Overall Place
            const placeA = results.places.find(p => p.teamId === a.id)?.place || 999;
            const placeB = results.places.find(p => p.teamId === b.id)?.place || 999;
            if (placeA !== placeB) return placeA - placeB;

            // #2 sort: Number of Nominations in key categories
            const nomsA = results.nominations.filter(n => n.teamId === a.id && relevantCategories.includes(n.category)).length || 0;
            const nomsB = results.nominations.filter(n => n.teamId === b.id && relevantCategories.includes(n.category)).length || 0;
            if (nomsA !== nomsB) return nomsB - nomsA;

            // #3 sort: Number of Wins in key categories
            const winsA = results.nominations.filter(n => n.teamId === a.id && relevantCategories.includes(n.category) && n.winner).length || 0;
            const winsB = results.nominations.filter(n => n.teamId === b.id && relevantCategories.includes(n.category) && n.winner).length || 0;
            if (winsA !== winsB) return winsB - winsA;

            // #4 sort: Robot Game Rank (only top 3 matter, and lower is better)
            const rgRankA = results.robotGameEntries.find(e => e.teamId === a.id && e.rank && e.rank <= 3)?.rank || 999;
            const rgRankB = results.robotGameEntries.find(e => e.teamId === b.id && e.rank && e.rank <= 3)?.rank || 999;
            if (rgRankA !== rgRankB) return rgRankA - rgRankB;

            // #5 sort: Coaching Award
            const coachingA = results.nominations.find(n => n.teamId === a.id && n.category === 'COACHING' && n.winner) ? 1 : 0;
            const coachingB = results.nominations.find(n => n.teamId === b.id && n.category === 'COACHING' && n.winner) ? 1 : 0;
            if (coachingA !== coachingB) return coachingB - coachingA;

            // #6 sort: nomination score
            const scoreA = results.nominations.filter(n => n.teamId === a.id).reduce((sum, n) => sum + (categoryScores[n.category as keyof typeof categoryScores] || 0), 0) || 0;
            const scoreB = results.nominations.filter(n => n.teamId === b.id).reduce((sum, n) => sum + (categoryScores[n.category as keyof typeof categoryScores] || 0), 0) || 0;
            if (scoreA !== scoreB) return scoreB - scoreA;

            // #7 sort: winner scores awards only
            const winnerScoreA = results.nominations.filter(n => n.teamId === a.id && n.winner).reduce((sum, n) => sum + (categoryScores[n.category as keyof typeof categoryScores] || 0), 0) || 0;
            const winnerScoreB = results.nominations.filter(n => n.teamId === b.id && n.winner).reduce((sum, n) => sum + (categoryScores[n.category as keyof typeof categoryScores] || 0), 0) || 0;
            if (winnerScoreA !== winnerScoreB) return winnerScoreB - winnerScoreA;

            // Final tiebreaker: FLL ID (lower is better)
            const fllA = parseInt(a.fllId as any) || 9999;
            const fllB = parseInt(b.fllId as any) || 9999;
            return fllA - fllB;
        });
    }, [teams, results]);

    // Filter teams to ONLY those who actually won or were nominated for something (for the Team Cards)
    const cookingTeams = useMemo(() => {
        return tableTeams.filter(team => {
            const hasPlace = results.places.some(p => p.teamId === team.id);
            const hasNom = results.nominations.some(n => n.teamId === team.id);
            const hasRobotGame = results.robotGameEntries.some(e => e.teamId === team.id && e.rank && e.rank <= 3);
            return hasPlace || hasNom || hasRobotGame;
        });
    }, [tableTeams, results]);

    // Helper to render the correct icon in the table cell
    const renderTableCell = (teamId: number, category: CompetitionAwardCategoryDto) => {
        const color = getCategoryConfig(category).color;
        if (category === 'ROBOT_GAME') {
            const rgEntry = results.robotGameEntries.find(e => e.teamId === teamId);
            if (rgEntry && rgEntry.rank && rgEntry.rank >= 2 && rgEntry.rank <= 3) {
                return (
                    <Tooltip label={t("app.competition.awards.tooltip.place_ROBOT_GAME", {place: t("app.competition.awards.place", { count: rgEntry.rank, ordinal: true })})}>
                        <Badge size={"lg"} leftSection={<IconMedal size={16}></IconMedal>} color={color} variant={"light"}>
                            {rgEntry.rank}
                        </Badge>
                    </Tooltip>
                );
            }
        }
        const nom = results.nominations.find(n => n.teamId === teamId && n.category === category);
        if (!nom) return null;

        if (nom.winner) {
            return (
                <Tooltip label={t("app.competition.awards.tooltip.winner", {context: category})}>
                    <ThemeIcon size={28} radius="xl" color={color} variant="outline">
                        <IconTrophy size={16} />
                    </ThemeIcon>
                </Tooltip>
            );
        }

        return (
            <Tooltip label={t("app.competition.awards.tooltip.nominated", {context: category})}>
                <ThemeIcon size={28} radius="xl" color={color} variant="subtle">
                    <IconStar size={16} />
                </ThemeIcon>
            </Tooltip>
        );
    };

    const TeamAwardCard = ({ team }: { team: SeasonTeamDto }) => {
        const { place, awards, robotGameEntry } = getTeamAchievements(team.id, results);

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
                                <Group gap="xs" align="center" mt={2}>
                                    <Text size="xs" c="dimmed">#{team.fllId}</Text>
                                    <AdvancingBadge competitionType={competition.type} advancing={place?.advancing} size="xs" nextCompetition={competition.nextCompetition} />
                                </Group>
                            </Stack>
                        </Group>


                    </Box>
                    {place && (
                        <PlaceBadge placeObj={place} displayStyle={'full'} />
                    )}
                </Group>

                <Divider my="sm" variant="dashed" />

                <TeamAchievementsStack awards={awards} robotGameEntry={robotGameEntry} />
            </Card>
        )
    };

    return (
        <Stack gap="xl" mt="md">

            {/* View Mode Toggle */}
            <Group justify="space-between" align="center">
                <TooltipGroup openDelay={500} closeDelay={100}>
                    <SegmentedControl
                        visibleFrom="sm"
                        value={viewMode}
                        onChange={(val) => setViewMode(val as ViewMode)}
                        styles={{ label: { padding: 0 } }}
                        data={[
                            {
                                value: 'categories',
                                label: (
                                    <Tooltip label={t("app.competition.awards.tooltip.view.categories")}>
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconLayoutBoard size={18} /></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            },
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
                                value: 'matrix',
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
                    <SegmentedControl
                        hiddenFrom="sm"
                        value={viewMode}
                        onChange={(val) => setViewMode(val as ViewMode)}
                        styles={{ label: { padding: 0 } }}
                        data={[
                            {
                                value: 'categories',
                                label: (
                                    <Tooltip label={t("app.competition.awards.tooltip.view.categories")}>
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconLayoutBoard size={18} /></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            },
                            {
                                value: 'teams',
                                label: (
                                    <Tooltip label={t("app.competition.awards.tooltip.view.teams")}>
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconLayoutGrid size={18} /></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            }
                        ]}
                    />
                </TooltipGroup>
            </Group>

            {/* 1. OVERALL PLACES & ADVANCING */}
            {viewMode == 'categories' && (
                <Box>
                    {sortedPlaces.length > 0 && (
                        <Card withBorder radius="md" p="lg" shadow="sm" bg="var(--mantine-color-body)">
                            <Title order={3} mb="md">{t("app.competition.awards.title")}</Title>
                            <Stack gap="md">
                                {sortedPlaces.map((p) => {
                                    const team = getTeam(p.teamId);
                                    if (!team) return null;

                                    return (
                                        <Group key={p.teamId} gap={"lg"} wrap="nowrap" align={"center"}>
                                            <PlaceBadge placeObj={p} displayStyle={'compact'} size="xl" />

                                            {/* 2. Team Info & Badge Grouped Tightly */}
                                            <Box style={{ flex: 1, minWidth: 0 }}>
                                                {/* Line 1: Team Name (Forced to act as a block, taking its own line) */}
                                                <Anchor
                                                    component={Link}
                                                    to={getTeamLink(team)}
                                                    c="inherit"
                                                    underline="hover"
                                                    fw={600}
                                                    size="md"
                                                    style={{ display: 'block', lineHeight: 1.4, marginBottom: 4 }}
                                                >
                                                    {team.name}
                                                </Anchor>

                                                {/* Line 2: ID and Badge paired together */}
                                                <Group gap="xs" align="center">
                                                    <Text size="sm" c="dimmed">[{team.fllId}]</Text>
                                                    <AdvancingBadge competitionType={competition.type} advancing={p.advancing} nextCompetition={competition.nextCompetition} />
                                                </Group>
                                            </Box>
                                        </Group>
                                    );
                                })}
                            </Stack>
                        </Card>
                    )}
                    {/* 2. SPECIFIC AWARDS GRID */}
                    <Box mt={"md"}>
                        <Title order={3} mb="md">{t("app.competition.awards.title_awards")}</Title>
                        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
                            <AwardCard category={CompetitionAwardCategoryDto.RESEARCH} />
                            <AwardCard category={CompetitionAwardCategoryDto.ROBOT_DESIGN} />
                            <AwardCard category={CompetitionAwardCategoryDto.CORE_VALUES} />
                            <AwardCard category={CompetitionAwardCategoryDto.ROBOT_GAME} />
                            <AwardCard category={CompetitionAwardCategoryDto.COACHING} />
                        </SimpleGrid>
                    </Box>
                </Box>
            )}

            {/* --- VIEW: TEAM CARDS (New) --- */}
            {(viewMode === 'teams' || viewMode === 'matrix') && (
                <Box hiddenFrom={viewMode === 'matrix' ? "sm" : undefined}>
                    <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
                        {cookingTeams.map(team => (
                            <TeamAwardCard key={team.id} team={team} />
                        ))}
                    </SimpleGrid>
                </Box>
            )}

            {/* --- TABLE VIEW --- */}
            {viewMode === 'matrix' && (
                <Box visibleFrom="sm">
                    <Card withBorder radius="md" p={0} shadow="sm">
                        <Table striped highlightOnHover verticalSpacing="md" horizontalSpacing="md">
                            <Table.Thead>
                                <Table.Tr>
                                    <Table.Th>{t("app.competition.awards.table.header_team")}</Table.Th>
                                    <Table.Th ta="center">{t("app.competition.awards.table.header_place")}</Table.Th>
                                    <Table.Th ta="center">{t("app.competition.awards.category", {context: "RESEARCH"})}</Table.Th>
                                    <Table.Th ta="center">{t("app.competition.awards.category", {context: "ROBOT_DESIGN"})}</Table.Th>
                                    <Table.Th ta="center">{t("app.competition.awards.category", {context: "CORE_VALUES"})}</Table.Th>
                                    <Table.Th ta="center">{t("app.competition.awards.category", {context: "ROBOT_GAME"})}</Table.Th>
                                    <Table.Th ta="center">{t("app.competition.awards.category", {context: "COACHING"})}</Table.Th>
                                </Table.Tr>
                            </Table.Thead>
                            <Table.Tbody>
                                {cookingTeams.map((team) => {
                                    const placeObj = results.places.find(p => p.teamId === team.id);

                                    return (
                                        <Table.Tr
                                            key={team.id}
                                            onClick={() => navigate(getTeamLink(team))}
                                            style={{ cursor: 'pointer' }}
                                        >
                                            <Table.Td>
                                                <Group gap="xs">
                                                    <SeasonTeamAvatar team={team}/>
                                                    <Stack gap={0}>
                                                        <Anchor
                                                            component={Link}
                                                            to={getTeamLink(team)}
                                                            c="inherit" // Inherit text color so it doesn't look like a standard blue link
                                                            underline="hover" // Only underline when they hover exactly over the text
                                                            fw={600}
                                                        >
                                                            {team.name}
                                                        </Anchor>

                                                        <Group gap="xs" align="center">
                                                            <Text size="xs" c="dimmed">[{team.fllId}]</Text>
                                                            <AdvancingBadge competitionType={competition.type} advancing={placeObj?.advancing} size="xs" nextCompetition={competition.nextCompetition} />
                                                        </Group>
                                                    </Stack>
                                                </Group>
                                            </Table.Td>

                                            <Table.Td>
                                                <Center>
                                                    <PlaceBadge placeObj={placeObj} displayStyle={'compact'} />
                                                </Center>
                                            </Table.Td>

                                            <Table.Td><Center>{renderTableCell(team.id, CompetitionAwardCategoryDto.RESEARCH)}</Center></Table.Td>
                                            <Table.Td><Center>{renderTableCell(team.id, CompetitionAwardCategoryDto.ROBOT_DESIGN)}</Center></Table.Td>
                                            <Table.Td><Center>{renderTableCell(team.id, CompetitionAwardCategoryDto.CORE_VALUES)}</Center></Table.Td>
                                            <Table.Td><Center>{renderTableCell(team.id, CompetitionAwardCategoryDto.ROBOT_GAME)}</Center></Table.Td>
                                            <Table.Td><Center>{renderTableCell(team.id, CompetitionAwardCategoryDto.COACHING)}</Center></Table.Td>
                                        </Table.Tr>
                                    );
                                })}
                            </Table.Tbody>
                        </Table>
                    </Card>
                </Box>
            )}


        </Stack>
    );
};