import { useMemo, useEffect, useState } from 'react';
import { MantineReactTable, useMantineReactTable, type MRT_ColumnDef } from 'mantine-react-table';
import { Badge, Text, Anchor, Box } from '@mantine/core';
import { type OverallRobotGameEntryDto } from '../api/generated';
import { client } from '../api';

// @ts-ignore

const COLOR_QUALIFIED = 'rgba(46, 204, 113, 0.15)'
const COLOR_NOT_QUALIFIED = 'rgba(231, 76, 60, 0.15)'

export const RobotGameLeaderboard = () => {
    const [data, setData] = useState<OverallRobotGameEntryDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        client.api.getOverallRobotGameLeaderboard()
            .then((res) => {
                setData(res.data);
                setIsLoading(false);
            })
            .catch(() => setIsLoading(false));
    }, []);

    const getLink = (urlPart: string) => {
        return `https://www.first-lego-league.org/de/challenge-2025-26/${urlPart}`;
    };

    // 1. Helper for Score Columns 📏
    // This allows us to apply the "narrow" logic to all score columns at once
    const scoreColProps: Partial<MRT_ColumnDef<OverallRobotGameEntryDto>> = {
        size: 50,      // Target width
        minSize: 40,   // ALLOW it to go this small (Crucial!)
        maxSize: 60,
        enableResizing: false,
        Header: ({ column }) => (
            <Box style={{ textAlign: 'center', width: '100%' }}>{column.columnDef.header}</Box>
        ),
        Cell: ({ cell }) => (
            <Text size="sm">{cell.getValue<number>()}</Text>
        ),
    };

    const columns = useMemo<MRT_ColumnDef<OverallRobotGameEntryDto>[]>(
        () => [
            {
                accessorKey: 'rank',
                header: '#',
                size: 30,     // Very narrow
                minSize: 30,  // Override default limit
                maxSize: 50,
                enableResizing: false,
                Cell: ({ cell }) => (
                    <Text fw={700}>{cell.getValue<number>()}</Text>
                )
            },
            {
                accessorKey: 'country',
                header: 'Land',
                size: 60,
                minSize: 60,
                Cell: ({ cell }) => (
                    <Badge variant="outline" color="gray">{cell.getValue<string>()}</Badge>
                ),
            },
            {
                accessorKey: 'teamName',
                header: 'Team',
                size: 220, // Give the name some room
                minSize: 150,
                Cell: ({ row }) => (
                    <Text fw={600} size="sm" truncate>{row.original.teamName}</Text>
                )
            },
            {
                // Explicit ID for sorting logic
                id: 'location',
                accessorFn: (row) => row.competition,
                header: 'Ort',
                size: 150,
                Cell: ({ row }) => (
                    <Anchor
                        href={getLink(row.original.competitionUrlPart ?? "")}
                        target="_blank"
                        rel="noreferrer"
                        size="sm"
                        underline="hover"
                        onClick={(e) => e.stopPropagation()}
                    >
                        {row.original.competition}
                    </Anchor>
                ),
            },
            // --- SCORE COLUMNS ---
            {
                accessorKey: 'bestScore',
                header: 'Best',
                ...scoreColProps, // Apply our narrow styles
                Cell: ({ cell }) => <Text fw={700} c="blue">{cell.getValue<number>()}</Text>,
            },
            { accessorKey: 'medianScore', header: 'Med', ...scoreColProps },
            { accessorKey: 'averageScore', header: 'Avg', ...scoreColProps },
            { accessorKey: 'worstScore', header: 'Worst', ...scoreColProps },
            { accessorKey: 'preliminaryRound1', header: 'VR1', ...scoreColProps },
            { accessorKey: 'preliminaryRound2', header: 'VR2', ...scoreColProps },
            { accessorKey: 'preliminaryRound3', header: 'VR3', ...scoreColProps },
            { accessorKey: 'bestPreliminaryScore', header: 'Beste VR', ...scoreColProps, size: 70 }, // Slightly wider header
            { accessorKey: 'quarterFinal', header: 'QF', ...scoreColProps },
            { accessorKey: 'semiFinal', header: 'SF', ...scoreColProps },
            { accessorKey: 'final1', header: 'F1', ...scoreColProps },
            { accessorKey: 'final2', header: 'F2', ...scoreColProps },
        ],
        []
    );

    const table = useMantineReactTable({
        columns,
        data,
        state: { isLoading },
        enableRowActions: false,

        // 2. STICKY HEADERS & COLUMNS 📌
        enableStickyHeader: true, // Header stays fixed at top
        enableColumnPinning: true, // Enable the "Freeze" feature

        initialState: {
            density: 'xs',
            pagination: { pageSize: 50, pageIndex: 0 },
        },

        // 3. STICKY SCROLLBAR (Max Height) 📏
        mantinePaperProps: {
            style: {
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
            },
        },
        mantineTableContainerProps: {
            style: {
                flex: 1,
                minHeight: 0,
            },
        },

        // Reduce cell padding to make it truly compact
        mantineTableBodyCellProps: {
            style: { padding: '4px', justifyContent: 'start'},
        },
        mantineTableHeadCellProps: {
            style: { padding: '4px', justifyContent: 'center' },
        },

        mantineTableBodyRowProps: ({ row }) => ({
            style: {
                backgroundColor: row.original.qualified
                    ? COLOR_QUALIFIED
                    : COLOR_NOT_QUALIFIED,
            },
        }),
    });

    return <MantineReactTable table={table} />;
};