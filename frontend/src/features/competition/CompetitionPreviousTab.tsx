import { useMemo } from 'react';
import {
    SimpleGrid,
    Card,
    Text,
    Group,
    Badge,
    Stack,
    SegmentedControl,
    Box,
    Center,
    Tooltip,
    TooltipGroup,
    Table, Anchor
} from '@mantine/core';
import {Link, useNavigate} from 'react-router-dom';
import { IconCalendar, IconLayoutGrid, IconList } from '@tabler/icons-react';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import type {CompetitionDetailDto} from '../../api/generated.ts';
import {getCompetitionLink} from "../../utils/routingUtils.ts";
import {getCompetitionTypeColor} from "../../utils/competitionUtils.ts";
import {useSessionStorage} from "@mantine/hooks";

interface Props {
    competition: CompetitionDetailDto;
}

export const CompetitionPreviousTab = ({ competition }: Props) => {
    const competitions = competition.previousCompetitions || [];
    const { t } = useTranslation();
    const navigate = useNavigate();

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
                        <Card
                            key={comp.id}
                            onClick={() => navigate(getCompetitionLink(comp))}
                            withBorder
                            shadow="sm"
                            radius="md"
                            p="md"
                            style={{
                                cursor: 'pointer',
                                transition: 'transform 0.2s ease, box-shadow 0.2s ease'
                            }}
                            onMouseEnter={(e) => {
                                e.currentTarget.style.transform = 'translateY(-2px)';
                                e.currentTarget.style.boxShadow = 'var(--mantine-shadow-md)';
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.transform = 'none';
                                e.currentTarget.style.boxShadow = 'var(--mantine-shadow-sm)';
                            }}
                        >
                            <Stack gap="sm">
                                <Group justify="space-between" wrap="nowrap">
                                    <Badge color={getCompetitionTypeColor(comp.type)}>
                                        {t('app.competition.detail.type', {context: comp.type})}
                                    </Badge>
                                    {comp.country && (
                                        <Badge variant="outline" color="gray">{comp.country}</Badge>
                                    )}
                                </Group>

                                <Text fw={700} size="lg" lineClamp={2}>{comp.name}</Text>

                                {comp.date && (
                                    <Group gap="xs" c="dimmed">
                                        <IconCalendar size={16} />
                                        <Text size="sm">{dayjs(comp.date).format('L')}</Text>
                                    </Group>
                                )}
                            </Stack>
                        </Card>
                    ))}
                </SimpleGrid>
            </Box>

            {/* TABLE VIEW (Hidden on mobile, visible on desktop if selected) */}
            {viewMode === 'table' && (
                <Box visibleFrom="sm">
                    <Table striped highlightOnHover verticalSpacing="sm">
                        <Table.Thead>
                            <Table.Tr>
                                <Table.Th>{t("app.competition.previous.header.name")}</Table.Th>
                                <Table.Th>{t("app.competition.previous.header.date")}</Table.Th>
                                <Table.Th>{t("app.competition.previous.header.type")}</Table.Th>
                                <Table.Th>{t("app.competition.previous.header.country")}</Table.Th>
                            </Table.Tr>
                        </Table.Thead>
                        <Table.Tbody>
                            {sortedCompetitions.map((comp) => (
                                <Table.Tr
                                    key={comp.id}
                                    onClick={() => navigate(getCompetitionLink(comp))}
                                    style={{ cursor: 'pointer' }}
                                >
                                    <Table.Td>
                                        <Anchor
                                            component={Link}
                                            to={getCompetitionLink(comp)}
                                            c="inherit" // Inherit text color so it doesn't look like a standard blue link
                                            underline="hover" // Only underline when they hover exactly over the text
                                            fw={600}
                                        >
                                            {comp.name}
                                        </Anchor>
                                    </Table.Td>
                                    <Table.Td>
                                        {comp.date ? (
                                            <Group gap="xs" c="dimmed" wrap="nowrap">
                                                <IconCalendar size={16} />
                                                <Text size="sm">{dayjs(comp.date).format('L')}</Text>
                                            </Group>
                                        ) : (
                                            <Text c="dimmed">-</Text>
                                        )}
                                    </Table.Td>
                                    <Table.Td>
                                        <Badge color={getCompetitionTypeColor(comp.type)} variant="light">
                                            {t('app.competition.detail.type', {context: comp.type})}
                                        </Badge>
                                    </Table.Td>
                                    <Table.Td>
                                        {comp.country ? (
                                            <Badge variant="outline" color="gray">{comp.country}</Badge>
                                        ) : (
                                            <Text c="dimmed">-</Text>
                                        )}
                                    </Table.Td>
                                </Table.Tr>
                            ))}
                        </Table.Tbody>
                    </Table>
                </Box>
            )}
        </Stack>
    );
};