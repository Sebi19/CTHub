import { useMemo, useEffect, useState } from 'react';
import { MantineReactTable, useMantineReactTable, type MRT_ColumnDef } from 'mantine-react-table';
import {Badge, Text, Anchor, Box, useMantineColorScheme } from '@mantine/core';
import {type OverallRobotGameEntryDto, type SeasonTeamDto} from '../../api/generated';
import { client } from '../../api';
import {useTranslation} from "react-i18next";
import { MRT_Localization_EN } from 'mantine-react-table/locales/en/index.cjs';
import { MRT_Localization_DE } from 'mantine-react-table/locales/de/index.cjs';
import {getCompetitionLink, getTeamLink} from "../../utils/routingUtils.ts";
import {Link} from "react-router-dom";

const tableLocales = {
    en: MRT_Localization_EN,
    de: MRT_Localization_DE,
};

const COLOR_QUALIFIED_DARK_MODE = 'rgba(46, 204, 113, 0.15)'
const COLOR_NOT_QUALIFIED_DARK_MODE = 'rgba(231, 76, 60, 0.15)'
const ROW_HOVER_DARK_MODE = 'rgba(255, 255, 255, 0.1)'
const ROW_HOVER_LIGHT_MODE = 'rgba(0, 0, 0, 0.05)'
const MAX_ROWS = 10000;


export const RobotGameLeaderboard = () => {
    const { i18n, t } = useTranslation();
    const currentLang = i18n.resolvedLanguage || 'de'; // Default to German if not resolved
    const currentTableLocale = useMemo(() => tableLocales[currentLang as keyof typeof tableLocales], [currentLang]);


    const [data, setData] = useState<OverallRobotGameEntryDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const colorScheme = useMantineColorScheme();

    useEffect(() => {
        client.api.getOverallRobotGameLeaderboard()
            .then((res) => {
                setData(res.data);
                setIsLoading(false);
            })
            .catch(() => setIsLoading(false));
    }, []);

    const formatName = (team: SeasonTeamDto): string => {
        return `${team.name} [${team.fllId}]`;
    }

    // 1. Helper for Score Columns 📏
    // This allows us to apply the "narrow" logic to all score columns at once
    const scoreColProps = useMemo<Partial<MRT_ColumnDef<OverallRobotGameEntryDto>>>(
        () => ({
            size: 50,      // Target width
                minSize: 40,   // ALLOW it to go this small (Crucial!)
                maxSize: 60,
                enableResizing: false,
                Header: ({ column }) => (
                <Box style={{ textAlign: 'center', width: '100%' }}>{column.columnDef.header}</Box>
            ),
                Cell: ({ cell }) => (
                <Text size="sm">{cell.getValue<number>()}</Text>
            )
        }), []);

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
                header: t('app.overall_robotgame.table.country'),
                size: 60,
                minSize: 60,
                Cell: ({ cell }) => (
                    <Badge variant="outline" color="gray">{cell.getValue<string>()}</Badge>
                ),
            },
            {
                accessorFn: (row) => row.team,
                header: t('app.overall_robotgame.table.team'),
                size: 220, // Give the name some room
                minSize: 150,
                Cell: ({ row }) => {
                    const team = row.original.team;
                    return (
                        <Anchor
                            component={Link}
                            to={getTeamLink(team)}
                            size="sm"
                            underline="hover"
                            c="inherit"
                            onClick={(e) => e.stopPropagation()}
                        >
                            <Text fw={600} size="sm" truncate>{formatName(team)}</Text>
                        </Anchor>
                    );
                }
            },
            {
                id: 'qualified',
                accessorFn: (row) => row.qualified ? 'Ja' : 'Nein',
                header: t('app.overall_robotgame.table.qualified'),
                size: 220, // Give the name some room
                minSize: 150,
                Cell: ({ row }) => (
                    <Text fw={600} size="sm" truncate>{row.original.qualified ? 'Ja' : 'Nein'}</Text>
                )
            },
            {
                // Explicit ID for sorting logic
                id: 'location',
                accessorFn: (row) => row.competition,
                header: t('app.overall_robotgame.table.location'),
                size: 150,
                Cell: ({ row }) => {
                    const competition = row.original.competition;
                    return (
                        <Anchor
                            component={Link}
                            to={getCompetitionLink(competition)}
                            size="sm"
                            underline="hover"
                            onClick={(e) => e.stopPropagation()}
                        >
                            {competition.name}
                        </Anchor>
                    );
                },
            },
            // --- SCORE COLUMNS ---
            {
                accessorKey: 'bestScore',
                header: t('app.overall_robotgame.table.best'),
                ...scoreColProps, // Apply our narrow styles
                Cell: ({ cell }) => <Text fw={700} c="blue">{cell.getValue<number>()}</Text>,
            },
            { accessorKey: 'medianScore', header: t('app.overall_robotgame.table.median'), ...scoreColProps },
            { accessorKey: 'averageScore', header: t('app.overall_robotgame.table.average'), ...scoreColProps },
            { accessorKey: 'worstScore', header: t('app.overall_robotgame.table.worst'), ...scoreColProps },
            { accessorKey: 'preliminaryRound1', header: t('app.overall_robotgame.table.preOne'), ...scoreColProps },
            { accessorKey: 'preliminaryRound2', header: t('app.overall_robotgame.table.preTwo'), ...scoreColProps },
            { accessorKey: 'preliminaryRound3', header: t('app.overall_robotgame.table.preThree'), ...scoreColProps },
            { accessorKey: 'bestPreliminaryScore', header: t('app.overall_robotgame.table.bestPre'), ...scoreColProps, size: 70 }, // Slightly wider header
            { accessorKey: 'quarterFinal', header: t('app.overall_robotgame.table.qf'), ...scoreColProps },
            { accessorKey: 'semiFinal', header: t('app.overall_robotgame.table.sf'), ...scoreColProps },
            { accessorKey: 'final1', header: t('app.overall_robotgame.table.fOne'), ...scoreColProps },
            { accessorKey: 'final2', header: t('app.overall_robotgame.table.fTwo'), ...scoreColProps },
        ],
        [scoreColProps, t]
    );

    const table = useMantineReactTable({
        columns,
        data,
        state: { isLoading },
        localization: currentTableLocale,
        enableRowActions: false,

        // 2. STICKY HEADERS & COLUMNS 📌
        enableStickyHeader: true, // Header stays fixed at top
        enableColumnPinning: true, // Enable the "Freeze" feature

        enableDensityToggle: false, // Disable density toggle

        mantinePaginationProps: {
            rowsPerPageOptions: ['5', '10', '25', '50', '100', { value: String(MAX_ROWS), label: t('app.overall_robotgame.table.all') }] as unknown as string[],
        },

        initialState: {
            density: 'xs',
            pagination: { pageSize: MAX_ROWS, pageIndex: 0 },
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
            style: { padding: '5px', justifyContent: 'start'},
        },
        mantineTableHeadCellProps: {
            style: { padding: '5px', justifyContent: 'center' },
        },

        mantineTableBodyRowProps: ({ row }) => ({
            style: {
                backgroundColor: row.original.qualified
                    ? COLOR_QUALIFIED_DARK_MODE
                    : COLOR_NOT_QUALIFIED_DARK_MODE,
                '--mrt-row-hover-background-color': colorScheme.colorScheme === 'dark'
                    ? ROW_HOVER_DARK_MODE
                    : ROW_HOVER_LIGHT_MODE, // A subtle dark tint for light mode
            },
        }),
    });

    return <MantineReactTable table={table} />;
};