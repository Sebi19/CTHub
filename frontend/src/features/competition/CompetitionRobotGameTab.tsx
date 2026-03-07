import {useEffect, useMemo, useRef} from 'react';
import { MantineReactTable, useMantineReactTable, type MRT_ColumnDef } from 'mantine-react-table';
import {Text, Stack, ThemeIcon, Box, Anchor} from '@mantine/core';
import { IconTrophy, IconMedal } from '@tabler/icons-react';
import { type CompetitionRobotGameEntryDto, type SeasonTeamDto } from '../../api/generated';
import {Link} from "react-router-dom";
import {getTeamLink} from "../../utils/routingUtils.ts";

interface Props {
    scores: CompetitionRobotGameEntryDto[];
    teams: SeasonTeamDto[];
}

// We create a combined type for the table row
type TableRowData = CompetitionRobotGameEntryDto & {
    team: SeasonTeamDto | null;
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
                team: team || null, // Keep the full team object if we need it later
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
                        <Anchor
                            component={Link}
                            to={getTeamLink(row.original.team!)}
                            c="inherit" // Inherit text color so it doesn't look like a standard blue link
                            underline="hover" // Only underline when they hover exactly over the text
                            fw={600}
                        >
                            {row.original.teamName}
                        </Anchor>
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
        <Box
            ref={scrollRef}
            w="100%"     // Force it to exactly the container width
            miw={0}      // Break the flexbox stretch trap!
            style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}
        >
            <MantineReactTable table={table} />
        </Box>
    );
};