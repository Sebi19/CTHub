import { useMemo } from 'react';
import {
    SimpleGrid,
    Text,
    Group,
    Stack,
    SegmentedControl,
    Box,
    Center,
    Tooltip,
    TooltipGroup,
} from '@mantine/core';
import { IconLayoutGrid, IconList } from '@tabler/icons-react';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import type {CompetitionDetailDto} from '../../../api/generated.ts';
import {useSessionStorage} from "@mantine/hooks";
import {CompetitionCard} from "../common/CompetitionCard.tsx";
import {CompetitionsTable} from "../common/CompetitionsTable.tsx";

interface Props {
    competition: CompetitionDetailDto;
}

export const CompetitionPreviousTab = ({ competition }: Props) => {
    const competitions = competition.previousCompetitions || [];
    const { t } = useTranslation();

    const [viewMode, setViewMode] = useSessionStorage<string | undefined>({
        key: `competition-teams-view-${competition.season.id}-${competition.urlPart}`,
        defaultValue: 'grid',
    });

    // 1. Sort competitions chronologically by date!
    const sortedCompetitions = useMemo(() => {
        return [...competitions].sort((a, b) => {
            if (!a.date) return 1; // Push items without dates to the bottom
            if (!b.date) return -1;
            return dayjs(a.date).valueOf() - dayjs(b.date).valueOf();
        });
    }, [competitions]);

    if (sortedCompetitions.length === 0) {
        return <Text c="dimmed" ta="center" py="xl">{t("app.competition.previous.empty", {context: competition.type})}</Text>;
    }

    return (
        <Stack gap="md">
            {/* View Mode Toggle */}
            <Group justify="space-between" align="center">
                <TooltipGroup openDelay={500} closeDelay={100}>
                    <SegmentedControl
                        visibleFrom="sm"
                        value={viewMode}
                        onChange={(val) => setViewMode(val as 'grid' | 'table')}
                        styles={{ label: { padding: 0 } }}
                        data={[
                            {
                                value: 'grid',
                                label: (
                                    <Tooltip label={t("app.competition.previous.tooltip.grid")}>
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconLayoutGrid size={18} /></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            },
                            {
                                value: 'table',
                                label: (
                                    <Tooltip label={t("app.competition.previous.tooltip.table")}>
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconList size={18} /></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            },
                        ]}
                    />
                </TooltipGroup>
            </Group>

            {/* GRID VIEW (Always visible on mobile, hidden on desktop if table is selected) */}
            <Box hiddenFrom={viewMode === 'table' ? 'sm' : undefined}>
                <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
                    {sortedCompetitions.map((comp) => (
                        <CompetitionCard competition={comp} key={comp.id}/>
                    ))}
                </SimpleGrid>
            </Box>

            {/* TABLE VIEW (Hidden on mobile, visible on desktop if selected) */}
            {viewMode === 'table' && (
                <Box visibleFrom="sm">
                    <CompetitionsTable competitions={sortedCompetitions} />
                </Box>
            )}
        </Stack>
    );
};