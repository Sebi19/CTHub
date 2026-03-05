import { useMemo } from 'react';
import { MantineReactTable, useMantineReactTable, type MRT_ColumnDef } from 'mantine-react-table';
import { Text, Stack, ThemeIcon } from '@mantine/core';
import { IconTrophy, IconMedal } from '@tabler/icons-react';
import { type CompetitionRobotGameEntryDto, type SeasonTeamDto } from '../../api/generated';

interface Props {
    scores: CompetitionRobotGameEntryDto[];
    teams: SeasonTeamDto[];
}

// We create a combined type for the table row
type TableRowData = CompetitionRobotGameEntryDto & {
    teamName: string;
    fllId: number | string;
};

export const CompetitionRobotGameTab = ({ scores, teams }: Props) => {

    // 1. Join the scores with the team data
    const tableData = useMemo<TableRowData[]>(() => {
        if (!scores) return [];
        return scores.map(score => {
            const team = teams.find(t => t.id === score.teamId);
            return {
                ...score,
                teamName: team?.name || 'Unbekanntes Team',
                fllId: team?.fllId || '-',
            };
        });
    }, [scores, teams]);

    // 2. Helper to determine if a column should be visible (only show if at least one team has a score > 0)
    const hasScore = (key: keyof CompetitionRobotGameEntryDto) => {
        return tableData.some(row => row[key] != null && (row[key] as number) > 0);
    };

    // 3. Define Columns
    const columns = useMemo<MRT_ColumnDef<TableRowData>[]>(
        () => [
            {
                accessorKey: 'rank',
                header: 'Rang',
                size: 80,
                Cell: ({ cell }) => {
                    const rank = cell.getValue<number>();
                    if (!rank) return null;

                    // Render cool icons for Top 3!
                    if (rank === 1) return <ThemeIcon color="yellow" variant="light"><IconTrophy size={16} /></ThemeIcon>;
                    if (rank === 2) return <ThemeIcon color="gray" variant="light"><IconMedal size={16} /></ThemeIcon>;
                    if (rank === 3) return <ThemeIcon color="orange" variant="light"><IconMedal size={16} /></ThemeIcon>;

                    return <Text fw={600} c="dimmed" ta="center" w={28}>{rank}</Text>;
                },
            },
            {
                id: 'team', // Custom ID since we are combining two fields
                header: 'Team',
                size: 250,
                accessorFn: (row) => `${row.teamName} ${row.fllId}`, // For sorting/searching
                Cell: ({ row }) => (
                    <Stack gap={0}>
                        <Text fw={600}>{row.original.teamName}</Text>
                        <Text size="xs" c="dimmed">[{row.original.fllId}]</Text>
                    </Stack>
                ),
            },
            {
                accessorKey: 'pr1',
                header: 'PR I',
                size: 50,
            },
            {
                accessorKey: 'pr2',
                header: 'PR II',
                size: 50,
            },
            {
                accessorKey: 'pr3',
                header: 'PR III',
                size: 50,
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
        []
    );

    // 4. Configure the Table
    const table = useMantineReactTable({
        columns,
        data: tableData,
        enableColumnActions: false,
        enablePagination: false,
        enableBottomToolbar: false,
        enableDensityToggle: false,
        // Dynamically hide knockout rounds if they haven't happened yet!
        initialState: {
            density: 'xs',
            sorting: [{ id: 'rank', desc: false }],
            columnVisibility: {
                r16: hasScore('r16'),
                qf: hasScore('qf'),
                sf: hasScore('sf'),
                f1: hasScore('f1'),
                f2: hasScore('f2'),
            }
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

    return <MantineReactTable table={table} />;
};