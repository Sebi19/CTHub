import {useEffect, useMemo, useState} from 'react';
import {useLocation, useNavigate, useParams} from 'react-router-dom';
import {
    Box,
    Center,
    Container,
    Group,
    Loader,
    Paper,
    SegmentedControl,
    Select,
    SimpleGrid,
    Stack,
    Text,
    Title,
    Tooltip,
    TooltipGroup
} from '@mantine/core';
import {IconCalendar, IconLayoutGrid, IconList, IconTag} from '@tabler/icons-react';
import {useTranslation} from 'react-i18next';
import {useDocumentTitle, useSessionStorage} from '@mantine/hooks';
import dayjs from 'dayjs';
import {CompetitionCard} from "../common/CompetitionCard.tsx";
import {CompetitionsTable} from "../common/CompetitionsTable.tsx";
import {client} from "../../../api.ts";
import {NotFoundPage} from "../../error/NotFoundPage.tsx";
import {ServerErrorPage} from "../../error/ServerErrorPage.tsx";
import {getCompetitionsListLink, navigateBack} from "../../../utils/routingUtils.ts";
import {CompetitionType} from "../../../api/generated.ts";
import {SeasonBadge} from "../../common/season/SeasonBadge.tsx";
import {useAppContext} from "../../../hooks/AppContext.tsx";

export const SeasonOverview = () => {
    const { seasonId } = useParams<{ seasonId: string }>();
    const { t } = useTranslation();
    const navigate = useNavigate();
    const location = useLocation();
    const {
        getSeasonDto,
        activeSeason,
        setActiveSeason,
        globalDefaultSeason,
        availableSeasonIds,
        seasonsLoaded
    } = useAppContext();

    // Data State
    const [competitions, setCompetitions] = useState<any[]>([]);
    const [errorCode, setErrorCode] = useState<number | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    // View & Filter State
    const [viewMode, setViewMode] = useSessionStorage<'grid' | 'table'>({
        key: 'season-overview-view-mode',
        defaultValue: 'grid',
    });
    const [sortBy, setSortBy] = useState<string>('date');
    const [countryFilter, setCountryFilter] = useState<string>('ALL');
    const [typeFilter, setTypeFilter] = useState<string>('ALL');

    useEffect(() => {
        // If there is a seasonId in the URL, tell the global context about it
        if (seasonId) {
            setActiveSeason(seasonId);
        }

        // Cleanup: When the user leaves the Season Overview pages entirely,
        // revert the global context back to the default (e.g. for the home page)
        return () => {
            setActiveSeason(globalDefaultSeason);
        };
    }, [seasonId, setActiveSeason, globalDefaultSeason]);

    useEffect(() => {
        if (seasonsLoaded && !seasonId && activeSeason) {
            navigate(`/competitions/${activeSeason}`, { replace: true });
        }
    }, [seasonId, activeSeason, seasonsLoaded, navigate]);

    // Determine target season ID safely
    const targetSeasonId = seasonId || activeSeason;

    // Derived Season DTO from context (will compute correctly once seasonsLoaded is true)
    const season = useMemo(() => {
        return getSeasonDto(targetSeasonId);
    }, [targetSeasonId, seasonsLoaded, getSeasonDto]);

    useDocumentTitle(t('app.competition.overview.doc_title', { seasonId: season.id, seasonName: season.name}));

    // Fetch Competitions whenever the targeted season changes
    useEffect(() => {
        if (!targetSeasonId) return;
        setIsLoading(true);

        client.api.getCompetitionsForSeason(targetSeasonId)
            .then(res => {
                setCompetitions(res.data);
                setErrorCode(null);
            })
            .catch(error => {
                setErrorCode(error.response?.status || 500);
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [targetSeasonId]);

    // 1. Filter and 2. Sort the data
    const processedCompetitions = useMemo(() => {
        let result = [...competitions];

        // Apply Country Filter
        if (countryFilter !== 'ALL') {
            result = result.filter(comp => comp.country === countryFilter);
        }

        // Apply Type Filter
        if (typeFilter !== 'ALL') {
            result = result.filter(comp => comp.type === typeFilter);
        }

        // Apply Sorting
        result.sort((a, b) => {
            if (sortBy === 'date') {
                if (!a.date) return 1;
                if (!b.date) return -1;
                return dayjs(a.date).valueOf() - dayjs(b.date).valueOf();
            } else if (sortBy === 'name') {
                return a.name.localeCompare(b.name);
            }
            return 0;
        });

        return result;
    }, [competitions, countryFilter, typeFilter, sortBy]);

    const handleBackNavigation = () => {
        navigateBack(location, navigate, getCompetitionsListLink(seasonId))
    };

    if (isLoading) {
        return <Center py="xl"><Loader /></Center>;
    }

    if (errorCode === 404) {
        return <NotFoundPage handleBackNavigation={handleBackNavigation} />;
    }

    // Catch 500s, 502s, network timeouts, etc.
    if (errorCode) {
        return <ServerErrorPage handleBackNavigation={handleBackNavigation} />;
    }

    return (
        <Container size="xl" py="xl">
            <Stack gap="xl">
                <Group justify="space-between" align="flex-end" mb="md">
                    <Box>
                        <SeasonBadge season={season}/>
                        <Title order={2}>{t('app.competition.overview.title')}</Title>
                        <Text c="dimmed">
                            {t('app.competition.overview.eventsFound', { count: processedCompetitions.length })}
                        </Text>
                    </Box>

                    {/* Dropdown to change the active season via URL routing */}
                    <Select
                        label={t('app.competition.overview.selectSeason') || "Season"}
                        value={targetSeasonId}
                        onChange={(nextSeason) => {
                            if (nextSeason) {
                                navigate(`/competitions/${nextSeason}`); // Adjust this path match to your router setup
                            }
                        }}
                        data={availableSeasonIds.map(id => ({ value: id, label: id }))}
                        w={150}
                    />
                </Group>

                {/* CONTROL BAR */}
                <Paper withBorder p="sm" radius="md" bg="var(--mantine-color-body)">
                    <Group justify="space-between" align="center" gap="md">

                        {/* Left: View Toggle & Sorting */}
                        <Group gap="sm">
                            <TooltipGroup openDelay={500} closeDelay={100}>
                                <SegmentedControl
                                    value={viewMode}
                                    onChange={(val) => setViewMode(val as 'grid' | 'table')}
                                    styles={{ label: { padding: 0 } }}
                                    data={[
                                        {
                                            value: 'grid',
                                            label: (
                                                <Tooltip label={t("app.competition.previous.tooltip.grid") || 'Grid'}>
                                                    <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center' }}>
                                                        <IconLayoutGrid size={18} />
                                                    </Box>
                                                </Tooltip>
                                            )
                                        },
                                        {
                                            value: 'table',
                                            label: (
                                                <Tooltip label={t("app.competition.previous.tooltip.table") || 'Table'}>
                                                    <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center' }}>
                                                        <IconList size={18} />
                                                    </Box>
                                                </Tooltip>
                                            )
                                        },
                                    ]}
                                />
                            </TooltipGroup>

                            <Select
                                value={sortBy}
                                onChange={(val) => setSortBy(val || 'date')}
                                data={[
                                    { value: 'date', label: t('app.competition.overview.sort.date')},
                                    { value: 'name', label: t('app.competition.overview.sort.name')},
                                ]}
                                leftSection={sortBy === 'date' ? <IconCalendar size={16} /> : <IconTag size={16} />}
                                w={160}
                            />
                        </Group>

                        {/* Right: Quick Filters */}
                        <Group gap="sm">
                            <SegmentedControl
                                value={countryFilter}
                                onChange={setCountryFilter}
                                data={[
                                    { label: t('app.competition.overview.filter.all'), value: 'ALL' },
                                    { label: '🇩🇪 DE', value: 'DE' },
                                    { label: '🇦🇹 AT', value: 'AT' },
                                    { label: '🇨🇭 CH', value: 'CH' },
                                ]}
                            />
                            <SegmentedControl
                                value={typeFilter}
                                onChange={setTypeFilter}
                                data={[
                                    { label: t('app.competition.overview.filter.all'), value: 'ALL' },
                                    { label: t('app.competition.overview.filter.regional'), value: CompetitionType.REGIONAL },
                                    { label: t('app.competition.overview.filter.qualification'), value: CompetitionType.QUALIFICATION },
                                    { label: t('app.competition.overview.filter.final'), value: CompetitionType.FINAL },
                                ]}
                            />
                        </Group>
                    </Group>
                </Paper>

                {/* RENDER VIEW */}
                {processedCompetitions.length === 0 ? (
                    <Center py="xl">
                        <Text c="dimmed">{t('app.competition.overview.noResults')}</Text>
                    </Center>
                ) : (
                    <>
                        {viewMode === 'grid' ? (
                            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
                                {processedCompetitions.map((comp) => (
                                    <CompetitionCard key={comp.id} competition={comp} />
                                ))}
                            </SimpleGrid>
                        ) : (
                            <Box style={{ overflowX: 'auto' }}>
                                <CompetitionsTable competitions={processedCompetitions} />
                            </Box>
                        )}
                    </>
                )}
            </Stack>
        </Container>
    );
};