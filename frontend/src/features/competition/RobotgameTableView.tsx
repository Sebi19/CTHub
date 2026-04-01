import {useCarouselScrollShield} from "../../hooks/useCarouselScrollShield.ts";
import {Anchor, Group, ScrollArea, Stack, Text} from "@mantine/core";
import {MantineReactTable, type MRT_ColumnDef, type MRT_SortingState, useMantineReactTable} from "mantine-react-table";
import {useMemo, useState} from "react";
import {SeasonTeamAvatar} from "../common/team/avatar/SeasonTeamAvatar.tsx";
import {Link} from "react-router-dom";
import {getTeamLink} from "../../utils/routingUtils.ts";
import {IconHash, IconMedal, IconTrophy} from "@tabler/icons-react";
import type {CompetitionRobotGameEntryDto, SeasonTeamDto} from "../../api/generated.ts";
import {useTranslation} from "react-i18next";

// We create a combined type for the table row
type TableRowData = CompetitionRobotGameEntryDto & {
    team: SeasonTeamDto;
    teamName: string;
    fllId: string;
};

interface RobotGameTableViewProps {
    scores: CompetitionRobotGameEntryDto[];
    teams: SeasonTeamDto[];
}

export const RobotGameTableView = ({scores, teams}: RobotGameTableViewProps) => {
    const {t} = useTranslation();
    // Calling the hook here means it only mounts when the table mounts!
    const scrollRef = useCarouselScrollShield<HTMLDivElement>();

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
                header: t("app.competition.robot_game.header.playoff"),
                size: 80,
                Cell: ({ cell }) => {
                    const rank = cell.getValue<number>();
                    const sortedByPlayoff = sorting.find(s => s.id === 'rank');

                    return <PlayoffRank rank={rank} subtle={!sortedByPlayoff} />
                },
            },
            {
                accessorKey: 'prelimRank',
                header: t("app.competition.robot_game.header.prelim"),
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
                header: t("app.competition.robot_game.header.team"),
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
                header: t("app.competition.robot_game.header.pr1"),
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
                header: t("app.competition.robot_game.header.pr2"),
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
                header: t("app.competition.robot_game.header.pr3"),
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
                header: t("app.competition.robot_game.header.best_pr"),
                size: 70,
                mantineTableBodyCellProps: {
                    style: { backgroundColor: 'var(--mantine-color-blue-light)' } // Highlights the column like the official site!
                },
                Cell: ({ cell }) => <Text fw={700}>{cell.getValue<number>()}</Text>,
            },
            {
                accessorKey: 'r16',
                header: t("app.competition.robot_game.header.r16"),
                size: 50,
            },
            {
                accessorKey: 'qf',
                header: t("app.competition.robot_game.header.qf"),
                size: 50,
            },
            {
                accessorKey: 'sf',
                header: t("app.competition.robot_game.header.sf"),
                size: 50,
            },
            {
                accessorKey: 'f1',
                header: t("app.competition.robot_game.header.f1"),
                size: 50,
            },
            {
                accessorKey: 'f2',
                header: t("app.competition.robot_game.header.f2"),
                size: 50,
            },
        ],
        [sorting, t]
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

        mantineTableProps: {
            striped: true,
            highlightOnHover: true,
        },
        mantinePaperProps: {
            shadow: 'none',
            withBorder: false,
        },
    });

    return (
        <ScrollArea type={"auto"} ref={scrollRef} style={{ width: '100%' }}>
            <MantineReactTable table={table} />
        </ScrollArea>
    );
};