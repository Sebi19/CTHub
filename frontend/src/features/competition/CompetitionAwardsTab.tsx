import {
    SimpleGrid,
    Card,
    Text,
    Title,
    Group,
    Stack,
    Badge,
    ThemeIcon,
    Divider,
    Box,
    Center,
    Tooltip, TooltipGroup, SegmentedControl, Table
} from '@mantine/core';
import {
    IconTrophy,
    IconMedal,
    IconArrowUpRight,
    IconBulb,
    IconRobot,
    IconUser,
    IconLayoutGrid,
    IconList,
    IconStar,
    IconHash,
    IconUsers,
    IconHeartHandshake,
    IconSettings, IconLayoutBoard
} from '@tabler/icons-react';
import {
    type CompetitionDetailDto,
    type CompetitionPlaceDto,
    type SeasonTeamDto
} from '../../api/generated';
import {Link, useNavigate} from "react-router-dom";
import React, {useMemo, useState} from "react";
import {useTranslation} from "react-i18next";
import {getCompetitionLink, getTeamLink} from "../../utils/routingUtils.ts";

interface Props {
    competition: CompetitionDetailDto
}

type ViewMode = 'categories' | 'teams' | 'matrix';

export const CompetitionAwardsTab = ({ competition }: Props) => {
    const { t } = useTranslation();
    const results = competition.results;
    const teams = competition.registeredTeams || [];
    const [viewMode, setViewMode] = useState<ViewMode>('categories');
    const navigate = useNavigate();

    // Helper: Safely get team data
    const getTeam = (teamId?: number) => teams.find(t => t.id === teamId);

    // Helper: Get winner for a specific category
    const getWinner = (category: string) => {
        const nom = results?.nominations?.find(n => n.category === category && n.winner);
        return nom ? getTeam(nom.teamId) : null;
    };

    const getTeamAchievements = (teamId: number) => {
        const place = results?.places?.find(p => p.teamId === teamId);
        const awards = results?.nominations?.filter(n => n.teamId === teamId) || [];
        return { place, awards };
    };

    // Helper: Get the other nominees (who didn't win) for a category
    const getNominees = (category: string) => {
        return results?.nominations
            ?.filter(n => n.category === category && !n.winner)
            .map(n => getTeam(n.teamId))
            .filter(Boolean) as SeasonTeamDto[];
    };

    const getRobotGameRank = (rank: number) => {
        return results?.robotGameEntries
            ?.filter(e => e.rank === rank)
            .map(n => getTeam(n.teamId))
            .filter(Boolean)[0] as SeasonTeamDto | undefined;
    }

    // Category styling metadata
    const categoryConfig: Record<string, { icon: React.ReactNode, color: string }> = {
        CHAMPION: { icon: <IconTrophy size={24} />, color: 'red' },
        RESEARCH: { icon: <IconBulb size={24} />, color: 'green' },
        ROBOT_DESIGN: { icon: <IconSettings size={24} />, color: 'orange' },
        CORE_VALUES: { icon: <IconUsers size={24} />, color: 'grape' },
        ROBOT_GAME: { icon: <IconRobot size={24} />, color: 'blue' },
        COACHING: { icon: <IconHeartHandshake size={24} />, color: 'lime' },
    };

    // Reusable Card Component for the Grid
    const AwardCard = ({ category }: { category: keyof typeof categoryConfig }) => {
        const winner = getWinner(category);
        const nominees = getNominees(category);
        const config = categoryConfig[category];

        const rgSecondPlace = getRobotGameRank(2);
        const rgThirdPlace = getRobotGameRank(3);

        if (!winner && nominees.length === 0) return null; // Hide if no data yet

        return (
            <Card withBorder radius="md" p="md" shadow="sm">
                <Group gap="sm" mb="md">
                    <ThemeIcon size={40} radius="md" color={config.color} variant="light">
                        {config.icon}
                    </ThemeIcon>
                    <Text fw={700} size="lg">{t('app.competition.awards.category', {context: category})}</Text>
                </Group>

                {winner ? (
                    <Box mb={nominees.length > 0 ? 'md' : 0}>
                        <Text size="sm" c="dimmed" tt="uppercase" fw={700} mb={"xs"}>{t("app.competition.awards.winner")}</Text>
                        <Group gap="xs">
                            <ThemeIcon color={config.color} variant={"outline"} radius="xl" size={28}>
                                <IconTrophy size={16}/>
                            </ThemeIcon>
                            <Text fw={600}>{winner.name} <Text span c="dimmed" size="sm" fw={400}>[{winner.fllId}]</Text></Text>
                        </Group>
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
                                    <Text key={nom.id} size="sm">
                                        {nom.name} <Text span c="dimmed" size="xs">[{nom.fllId}]</Text>
                                    </Text>
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
                            {rgSecondPlace && (
                                <Group gap={"xs"}>
                                    <Tooltip label={t("app.competition.awards.place", { count: 2, ordinal: true })}>
                                        <Badge size={"lg"} leftSection={<IconMedal size={16}></IconMedal>} color={config.color} variant={"light"}>
                                            2
                                        </Badge>
                                    </Tooltip>
                                    <Text size="sm">
                                        {rgSecondPlace?.name} <Text span c="dimmed" size="xs">[{rgSecondPlace?.fllId}]</Text>
                                    </Text>
                                </Group>
                            )}
                            {rgThirdPlace && (
                                <Group gap={"xs"}>
                                    <Tooltip label={t("app.competition.awards.place", { count: 3, ordinal: true })}>
                                        <Badge size={"lg"} leftSection={<IconMedal size={16}></IconMedal>} color={config.color} variant={"light"}>
                                            3
                                        </Badge>
                                    </Tooltip>
                                    <Text size="sm">
                                        {rgThirdPlace?.name} <Text span c="dimmed" size="xs">[{rgSecondPlace?.fllId}]</Text>
                                    </Text>
                                </Group>
                            )}
                        </Stack>
                    </>
                )}
            </Card>
        );
    };

    // Ensure places are sorted 1, 2, 3...
    const sortedPlaces = [...(results?.places || [])].sort((a, b) => (a.place || 99) - (b.place || 99));

    // --- TABLE VIEW LOGIC --- //

    // 1. Sort teams so the best performing teams are at the top of the table!
    const tableTeams = useMemo(() => {
        return [...teams].sort((a, b) => {
            // Primary sort: Overall Place
            const placeA = results?.places?.find(p => p.teamId === a.id)?.place || 999;
            const placeB = results?.places?.find(p => p.teamId === b.id)?.place || 999;
            if (placeA !== placeB) return placeA - placeB;

            // Secondary sort: Number of Wins
            const winsA = results?.nominations?.filter(n => n.teamId === a.id && n.winner).length || 0;
            const winsB = results?.nominations?.filter(n => n.teamId === b.id && n.winner).length || 0;
            if (winsA !== winsB) return winsB - winsA;

            // Tertiary sort: Number of Nominations
            const nomsA = results?.nominations?.filter(n => n.teamId === a.id && !n.winner).length || 0;
            const nomsB = results?.nominations?.filter(n => n.teamId === b.id && !n.winner).length || 0;
            return nomsB - nomsA;
        });
    }, [teams, results]);

    // Filter teams to ONLY those who actually won or were nominated for something (for the Team Cards)
    const cookingTeams = useMemo(() => {
        return tableTeams.filter(team => {
            const hasPlace = results?.places?.some(p => p.teamId === team.id);
            const hasNom = results?.nominations?.some(n => n.teamId === team.id);
            const hasRobotGame = results?.robotGameEntries?.some(e => e.teamId === team.id && e.rank && e.rank <= 3);
            return hasPlace || hasNom || hasRobotGame;
        });
    }, [tableTeams, results]);

    const AdvancingBadge = ({ advancing, size = 'md' }: { advancing?: boolean, size?: any }) => {
        if (!advancing) return null;

        if (competition.nextCompetition) {
            return (
                <Badge
                    component={Link}
                    to={getCompetitionLink(competition.nextCompetition)}
                    color="green"
                    variant="light"
                    size={size}
                    rightSection={<IconArrowUpRight size={14} />}
                    style={{ cursor: 'pointer' }}
                    onClick={(e) => e.stopPropagation()} // <-- Crucial: stops the Row/Card from also being clicked!
                >
                    {t("app.competition.awards.qualified")}
                </Badge>
            );
        }

        return (
            <Badge color="green" variant="light" size={size} rightSection={<IconArrowUpRight size={14} />}>
                {t("app.competition.awards.qualified")}
            </Badge>
        );
    };

    // Helper to render the correct icon in the table cell
    const renderTableCell = (teamId: number, category: string) => {
        const nom = results?.nominations?.find(n => n.teamId === teamId && n.category === category);
        if (category === 'ROBOT_GAME') {
            const rgEntry = results?.robotGameEntries?.find(e => e.teamId === teamId);
            if (rgEntry && rgEntry.rank && rgEntry.rank >= 2 && rgEntry.rank <= 3) {
                return (
                    <Tooltip label={t("app.competition.awards.tooltip.place_ROBOT_GAME", {place: t("app.competition.awards.place", { count: rgEntry.rank, ordinal: true })})}>
                        <Badge size={"lg"} leftSection={<IconMedal size={16}></IconMedal>} color={categoryConfig[category].color} variant={"light"}>
                            {rgEntry.rank}
                        </Badge>
                    </Tooltip>
                );
            }
        }
        if (!nom) return null;

        const color = categoryConfig[category].color;

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

    const renderTablePlaceCell = (placeObj: CompetitionPlaceDto | undefined, style: 'full' | 'compact' = 'compact', size: number = 16) => {
        if (!placeObj) return <Text c="dimmed">-</Text>;

        const color = placeObj.place === 1 ? 'red' : placeObj.place === 2 ? 'gray' : placeObj.place === 3 ? 'orange' : 'gray';
        const icon = placeObj.place === 1 ? <IconTrophy size={size} /> : placeObj.place === 2 || placeObj.place === 3 ? <IconMedal size={size} /> : <IconHash size={size} />;
        const variant = placeObj.place === 1 ? "outline" : "light"
        const badgeSize = size > 16 ? "xl" : "lg";

        if (style === 'full') {
            return (
                <Badge size={badgeSize} leftSection={icon} color={color} variant={variant} tt={"none"}>
                    {t("app.competition.awards.place", {count: placeObj.place, ordinal: true})}
                </Badge>
            );
        }

        return (
            <Tooltip label={t("app.competition.awards.place", { count: placeObj.place, ordinal: true, context: placeObj.place })}>
                <Badge size={badgeSize} leftSection={icon} color={color} variant={variant}>
                   {placeObj.place}
                </Badge>
            </Tooltip>
        );
    }

    const TeamAwardCard = ({ team }: { team: SeasonTeamDto }) => {
        const { place, awards } = getTeamAchievements(team.id!);
        const winnerAwards = awards.filter(a => a.winner);
        const nominations = awards.filter(a => !a.winner);
        const rgPlace = results?.robotGameEntries?.find(e => e.teamId === team.id)?.rank;

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
                        <Text fw={700} lineClamp={1} size={"lg"}>{team.name}</Text>
                        <Group gap="xs" align="center" mt={2}>
                            <Text size="xs" c="dimmed">#{team.fllId}</Text>
                            <AdvancingBadge advancing={place?.advancing} size="xs" />
                        </Group>
                    </Box>
                    {place && renderTablePlaceCell(place, "full")}
                </Group>

                <Divider my="sm" variant="dashed" />

                <Stack gap="xs">
                    {winnerAwards.map(nom => {
                        const config = categoryConfig[nom.category!];
                        return (
                            <Group key={nom.category} gap="sm" wrap="nowrap">
                                <ThemeIcon size={28} radius="xl" color={config.color} variant="outline">
                                    <IconTrophy size={16} />
                                </ThemeIcon>
                                <Box>
                                    <Text size="sm" fw={600} lh={1.2} c={config.color}>
                                        {t('app.competition.awards.category', {context: nom.category})}
                                    </Text>
                                    <Text size="xs" c={'dimmed'} fw={700}>
                                        {t("app.competition.awards.winner")}
                                    </Text>
                                </Box>
                            </Group>
                        );
                    })}
                    {nominations.map(nom => {
                        const config = categoryConfig[nom.category!];
                        return (
                            <Group key={nom.category} gap="sm" wrap="nowrap">
                                <ThemeIcon size={28} radius="xl" color={config.color} variant="subtle">
                                     <IconStar size={16} />
                                </ThemeIcon>
                                <Box>
                                    <Text size="sm" fw={600} lh={1.2} c={config.color}>
                                        {t('app.competition.awards.category', {context: nom.category})}
                                    </Text>
                                    <Text size="xs" c={ 'dimmed'}  fw={700}>
                                        {t("app.competition.awards.nominated")}
                                    </Text>
                                </Box>
                            </Group>
                        );
                    })}
                    {rgPlace && rgPlace >= 2 && rgPlace <= 3 && (
                        <Group key={"ROBOT_GAME"} gap="sm" wrap="nowrap">
                            <ThemeIcon size={28} radius="xl" color={categoryConfig["ROBOT_GAME"].color} variant={"subtle"}>
                                <IconMedal size={16} />
                            </ThemeIcon>
                            <Box>
                                <Text size="sm" fw={600} lh={1.2} c={categoryConfig["ROBOT_GAME"].color}>
                                    {t('app.competition.awards.category', {context: "ROBOT_GAME"})}
                                </Text>
                                <Text size="xs" c={'dimmed'} fw={700}>
                                    {t("app.competition.awards.place", { count: rgPlace, ordinal: true })}
                                </Text>
                            </Box>
                        </Group>
                    )}
                </Stack>
            </Card>
        )
    };

    if (!results) {
        return <Text c="dimmed" ta="center" py="xl">{t("app.competition.awards.empty")}</Text>;
    }


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
                                    <Tooltip label="Kachelansicht">
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconLayoutGrid size={18} /></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            },
                            {
                                value: 'teams',
                                label: (
                                    <Tooltip label="Matrixansicht">
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconUser size={18} /></Center>
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
                                            {renderTablePlaceCell(p, "compact", 22)}

                                            {/* 2. Team Info & Badge Grouped Tightly */}
                                            <Box style={{ flex: 1, minWidth: 0 }}> {/* minWidth: 0 prevents long text from breaking the card layout on mobile */}
                                                <Box lh={1.4} mb={2}>
                                                    {/* component="span" forces the text to flow like words in a paragraph */}
                                                    <Text component="span" fw={600} size="md" mr="sm">
                                                        {team.name}
                                                    </Text>
                                                    {/* display: 'inline-block' allows the badge to act like a giant word that can wrap safely to the next line */}
                                                    <span style={{ display: 'inline-block', verticalAlign: 'middle' }}>
                                                        <AdvancingBadge advancing={p.advancing} />
                                                    </span>
                                                </Box>
                                                <Text size="sm" c="dimmed">Team #{team.fllId}</Text>
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
                            <AwardCard category="RESEARCH" />
                            <AwardCard category="CORE_VALUES" />
                            <AwardCard category="ROBOT_DESIGN" />
                            <AwardCard category="ROBOT_GAME" />
                            <AwardCard category="COACHING" />
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
                                    <Table.Th ta="center">{t("app.competition.awards.category", {context: "CORE_VALUES"})}</Table.Th>
                                    <Table.Th ta="center">{t("app.competition.awards.category", {context: "ROBOT_DESIGN"})}</Table.Th>
                                    <Table.Th ta="center">{t("app.competition.awards.category", {context: "ROBOT_GAME"})}</Table.Th>
                                    <Table.Th ta="center">{t("app.competition.awards.category", {context: "COACHING"})}</Table.Th>
                                </Table.Tr>
                            </Table.Thead>
                            <Table.Tbody>
                                {cookingTeams.map((team) => {
                                    const placeObj = results.places?.find(p => p.teamId === team.id);

                                    return (
                                        <Table.Tr
                                            key={team.id}
                                            onClick={() => navigate(getTeamLink(team))}
                                            style={{ cursor: 'pointer' }}
                                        >
                                            <Table.Td>
                                                <Text fw={600}>{team.name}</Text>
                                                <Group gap="xs" align="center">
                                                    <Text size="xs" c="dimmed">[{team.fllId}]</Text>
                                                    <AdvancingBadge advancing={placeObj?.advancing} size="xs" />
                                                </Group>
                                            </Table.Td>

                                            <Table.Td>
                                                <Center>
                                                    {renderTablePlaceCell(placeObj, "compact")}
                                                </Center>
                                            </Table.Td>

                                            <Table.Td><Center>{renderTableCell(team.id!, 'RESEARCH')}</Center></Table.Td>
                                            <Table.Td><Center>{renderTableCell(team.id!, 'CORE_VALUES')}</Center></Table.Td>
                                            <Table.Td><Center>{renderTableCell(team.id!, 'ROBOT_DESIGN')}</Center></Table.Td>
                                            <Table.Td><Center>{renderTableCell(team.id!, 'ROBOT_GAME')}</Center></Table.Td>
                                            <Table.Td><Center>{renderTableCell(team.id!, 'COACHING')}</Center></Table.Td>
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