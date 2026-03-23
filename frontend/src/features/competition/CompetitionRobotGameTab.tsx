import {useEffect, useMemo, useRef, useState} from 'react';
import {MantineReactTable, useMantineReactTable, type MRT_ColumnDef, type MRT_SortingState} from 'mantine-react-table';
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
    SegmentedControl, SimpleGrid
} from '@mantine/core';
import {IconTrophy, IconMedal, IconLayoutBoard, IconLayoutGrid, IconList, IconHash} from '@tabler/icons-react';
import {type CompetitionDetailDto, type CompetitionRobotGameEntryDto, type SeasonTeamDto} from '../../api/generated';
import {Link, useNavigate} from "react-router-dom";
import {getTeamLink} from "../../utils/routingUtils.ts";
import {getTeamAchievements} from "../../utils/competitionUtils.ts";
import {SeasonTeamAvatar} from "../common/team/avatar/SeasonTeamAvatar.tsx";
import {useTranslation} from "react-i18next";
import {useSessionStorage} from "@mantine/hooks";
import {TeamRobotgameOverview} from "../common/team/TeamRobotgameOverview.tsx";

interface Props {
    competition: CompetitionDetailDto;
}

type ViewMode = 'categories' | 'teams' | 'table';

// We create a combined type for the table row
type TableRowData = CompetitionRobotGameEntryDto & {
    team: SeasonTeamDto;
    teamName: string;
    fllId: string;
};

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

    // 1. Join the scores with the team data
    const tableData = useMemo<TableRowData[]>(() => {
        if (!scores) return [];

        return scores.reduce<TableRowData[]>((acc, score) => {
            const team = teams.find(t => t.id === score.teamId);

            // If a team is found, build the object and push it to the accumulator
            if (team) {
                acc.push({
                    ...score,
                    team: team,
                    teamName: team.name, // No need for fallbacks now
                    fllId: team.fllId,
                });
            }

            // If no team is found, we just return the accumulator unchanged
            return acc;
        }, []);
    }, [scores, teams]);

    // 2. Helper to determine if a column should be visible (only show if at least one team has a score > 0)
    const hasScore = (key: keyof CompetitionRobotGameEntryDto) => {
        return tableData.some(row => row[key] != null && (row[key] as number) > 0);
    };

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

    const PrelimRank = ({rank, subtle = false}: {rank: number, subtle?: boolean}) => {
        const color = subtle ? 'var(--mantine-color-blue-4)' : 'var(--mantine-color-blue-6)';
        const textColor = subtle ? "dimmed" : "inherit";

        return (
            <Group gap={4}>
                <IconHash size={18} color={color}/>
                <Text fw={subtle ? "inherit" : 700} size="lg" c={textColor}>{rank || '-'}</Text>
            </Group>
        );
    }

    const PlayoffRank = ({rank, subtle = false}: {rank: number, subtle?: boolean}) => {
        const color = subtle ? 'var(--mantine-color-blue-4)' : 'var(--mantine-color-blue-6)';
        const textColor = subtle ? "dimmed" : "inherit";

        const winnerIcon = <IconTrophy size={18} color={color} />
        const medalIcon = <IconMedal size={18} color={color} />
        const otherIcon = <IconHash size={18} color={color} />

        const icon = rank === 1 ? winnerIcon : (rank <= 3 ? medalIcon : otherIcon);

        return (
            <Group gap={4}>
                {icon}
                <Text fw={subtle ? "inherit" : 700} c={textColor} size="lg">{rank || '-'}</Text>
            </Group>
        );
    }

        const [sorting, setSorting] = useState<MRT_SortingState>([{ id: 'rank', desc: false }]);


    // 3. Define Columns
    const columns = useMemo<MRT_ColumnDef<TableRowData>[]>(
        () => [
            {
                accessorKey: 'rank',
                header: 'Playoff',
                size: 80,
                Cell: ({ cell }) => {
                    const rank = cell.getValue<number>();
                    const sortedByPlayoff = sorting.find(s => s.id === 'rank');

                    return <PlayoffRank rank={rank} subtle={!sortedByPlayoff} />
                },
            },
            {
                accessorKey: 'prelimRank',
                header: 'Vorrunde',
                size: 80,
                Cell: ({ cell }) => {
                    const prelimRank = cell.getValue<number>();
                    if (!prelimRank) return null;

                    const sortedByPrelim = sorting.find(s => s.id === 'prelimRank');

                    return <PrelimRank rank={prelimRank} subtle={!sortedByPrelim} />
                },
            },
            {
                id: 'team', // Custom ID since we are combining two fields
                header: 'Team',
                size: 250,
                accessorFn: (row) => `${row.fllId} ${row.teamName}`, // For sorting/searching
                Cell: ({ row }) => (
                    <Group gap="xs" wrap="nowrap">
                        <SeasonTeamAvatar team={row.original.team}/>
                        <Stack gap={0}>
                            <Anchor
                                component={Link}
                                to={getTeamLink(row.original.team)}
                                c="inherit" // Inherit text color so it doesn't look like a standard blue link
                                underline="hover" // Only underline when they hover exactly over the text
                                fw={600}
                            >
                                {row.original.teamName}
                            </Anchor>
                            <Text size="xs" c="dimmed">[{row.original.fllId}]</Text>
                        </Stack>
                    </Group>
                ),
            },
            {
                accessorKey: 'pr1',
                header: 'PR1',
                size: 50,
                minSize: 25,
                mantineTableHeadCellProps: {
                    style: {
                        minWidth: '25px', // THE FIX: Nukes MRT's injected 120px rule
                        width: '50px'
                    }
                },
                mantineTableBodyCellProps: {
                    style: {
                        minWidth: '25px', // Nukes it on the body cells too
                        width: '50px'
                    }
                },
                Cell: ({ cell, row }) => {
                    const score = cell.getValue<number>();
                    const isBest = score === row.original.bestPr;
                    return (
                        <Text c={isBest ? "inherit" : "dimmed"} fw={isBest ? 700 : "inherit"}>{score}</Text>
                    )
                }
            },
            {
                accessorKey: 'pr2',
                header: 'PR2',
                size: 50,
                Cell: ({ cell, row }) => {
                    const score = cell.getValue<number>();
                    const isBest = score === row.original.bestPr;
                    return (
                        <Text c={isBest ? "inherit" : "dimmed"} fw={isBest ? 700 : "inherit"}>{score}</Text>
                    )
                }
            },
            {
                accessorKey: 'pr3',
                header: 'PR3',
                size: 50,
                Cell: ({ cell, row }) => {
                    const score = cell.getValue<number>();
                    const isBest = score === row.original.bestPr;
                    return (
                        <Text c={isBest ? "inherit" : "dimmed"} fw={isBest ? 700 : "inherit"}>{score}</Text>
                    )
                }
            },
            {
                accessorKey: 'bestPr',
                header: 'Best PR',
                size: 70,
                mantineTableBodyCellProps: {
                    style: { backgroundColor: 'var(--mantine-color-blue-light)' } // Highlights the column like the official site!
                },
                Cell: ({ cell }) => <Text fw={700}>{cell.getValue<number>()}</Text>,
            },
            {
                accessorKey: 'r16',
                header: 'R16',
                size: 50,
            },
            {
                accessorKey: 'qf',
                header: 'QF',
                size: 50,
            },
            {
                accessorKey: 'sf',
                header: 'SF',
                size: 50,
            },
            {
                accessorKey: 'f1',
                header: 'F I',
                size: 50,
            },
            {
                accessorKey: 'f2',
                header: 'F II',
                size: 50,
            },
        ],
        [sorting]
    );


    // 4. Configure the Table
    const table = useMantineReactTable({
        columns,
        data: tableData,
        defaultColumn: {
            minSize: 20, //allow columns to get smaller than default
            size: 20,
        },
        enableColumnActions: false,
        enablePagination: false,
        enableBottomToolbar: false,
        enableDensityToggle: false,
        onSortingChange: setSorting,
        layoutMode: 'semantic',
        // Dynamically hide knockout rounds if they haven't happened yet!
        initialState: {
            density: 'xs',
            columnVisibility: {
                r16: hasScore('r16'),
                qf: hasScore('qf'),
                sf: hasScore('sf'),
                f1: hasScore('f1'),
                f2: hasScore('f2'),
            }
        },
        state: {
            sorting: sorting
        },

        mantineTableHeadCellProps: {
            style: {
                // 1. Tell the cell itself not to hold onto extra space
                width: '1%',
                whiteSpace: 'nowrap',

                // 2. Target the MRT internal flex container
                '& > div': {
                    minWidth: '0 !important',
                    flex: '1 1 auto', // Allow it to shrink (1) and grow (1)
                },
            },
        },
        mantineTableBodyCellProps: {
            style: {
                // 1. Tell the cell itself not to hold onto extra space
                width: '1%',
                whiteSpace: 'nowrap',

                // 2. Target the MRT internal flex container
                '& > div': {
                    minWidth: '0 !important',
                    flex: '1 1 auto', // Allow it to shrink (1) and grow (1)
                },
            },
        },
        mantineTableProps: {
            striped: true,
            highlightOnHover: true,
            style: {
                width: '100%', // Tell it to stretch and take up all available space
                // THE ACTUAL FIX: Nuke MRT's hidden CSS variables globally
                '--mrt-th-min-width': '0px',
                '--mrt-td-min-width': '0px',
                '& th, & td': {
                    // Allow them to be small, but 'fixed' layout will expand them
                    // proportionally to fill the 100% width of the table.
                    width: 'auto !important',
                    minWidth: 'unset !important',
                },
            } as React.CSSProperties,
        },
        mantinePaperProps: {
            shadow: 'none',
            withBorder: false,
        },
    });

    const scrollRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const boxEl = scrollRef.current;
        if (!boxEl) return;

        let startX = 0;
        let startY = 0;
        let activeScroller: HTMLElement | null = null;

        const handleTouchStart = (e: TouchEvent) => {
            startX = e.touches[0].clientX;
            startY = e.touches[0].clientY;

            // 1. Find the exact element that is natively scrolling
            // (MRT generates internal containers, so we traverse up from the touch target to find it)
            let node = e.target as HTMLElement;
            activeScroller = null;

            while (node && node !== document.body) {
                if (node.scrollWidth > node.clientWidth) {
                    const style = window.getComputedStyle(node);
                    if (style.overflowX === 'auto' || style.overflowX === 'scroll') {
                        activeScroller = node;
                        break;
                    }
                }
                if (node === boxEl) break; // Don't look higher than our wrapper
                node = node.parentElement as HTMLElement;
            }

            // Fallback to the wrapper Box if no internal scroller was found
            if (!activeScroller) activeScroller = boxEl;
        };

        const handleTouchMove = (e: TouchEvent) => {
            if (!activeScroller) return;

            const currentX = e.touches[0].clientX;
            const currentY = e.touches[0].clientY;
            const deltaX = startX - currentX;
            const deltaY = startY - currentY;

            // If the user is scrolling vertically, ignore it completely
            if (Math.abs(deltaY) > Math.abs(deltaX)) return;

            // 2. The Boundary Math
            const { scrollLeft, scrollWidth, clientWidth } = activeScroller;

            const isAtLeftEdge = scrollLeft <= 0;
            // We use <= 1 instead of === 0 to account for sub-pixel zoom rounding bugs in mobile browsers!
            const isAtRightEdge = Math.abs(scrollWidth - clientWidth - scrollLeft) <= 1;

            // 3. The Smart Shield Logic
            if (deltaX > 0 && !isAtRightEdge) {
                // Swiping left (scrolling table to the right), and NOT at the right edge
                e.stopPropagation();
            } else if (deltaX < 0 && !isAtLeftEdge) {
                // Swiping right (scrolling table to the left), and NOT at the left edge
                e.stopPropagation();
            }

            // IF we reach this point, they ARE at the edge and pushing against it.
            // We do NOTHING. The event bubbles up to Embla, and Embla seamlessly grabs the slide!
        };

        // We use passive: true because we aren't cancelling native scrolling, just managing bubbling
        boxEl.addEventListener('touchstart', handleTouchStart, { passive: true });
        boxEl.addEventListener('touchmove', handleTouchMove, { passive: true });

        return () => {
            boxEl.removeEventListener('touchstart', handleTouchStart);
            boxEl.removeEventListener('touchmove', handleTouchMove);
        };
    }, []);

    return (
        <Stack gap="xl" mt="md">
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

            {(viewMode === 'teams' || viewMode === 'table') && (
                <Box hiddenFrom={viewMode === 'table' ? "sm" : undefined}>
                    <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
                        {sortedTeams.map(team => (
                            <TeamRobotGameCard key={team.id} team={team} />
                        ))}
                    </SimpleGrid>
                </Box>
            )}

            {viewMode === 'table' && (
                    <div style={{width: '100%', overflow: 'hidden', display: 'block'}}>
                        <MantineReactTable table={table}/>
                    </div>
                )}
        </Stack>

    );
};