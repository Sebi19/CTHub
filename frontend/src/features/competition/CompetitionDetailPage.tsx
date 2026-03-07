import {useEffect, useMemo, useRef, useState} from 'react';
import {Link, useParams} from 'react-router-dom';
import {
    Container,
    Title,
    Text,
    Group,
    Badge,
    Loader,
    Tabs,
    Button,
    Center,
    Stack,
    Alert,
    Anchor,
    Card, SimpleGrid, Box, ScrollArea
} from '@mantine/core';
import {
    IconCalendar,
    IconMapPin,
    IconExternalLink,
    IconRobot,
    IconTrophy,
    IconUsers,
    IconInfoCircle, IconUser, IconArrowRight, IconSitemap
} from '@tabler/icons-react';

// Import our new tab components
import type {CompetitionDetailDto} from "../../api/generated.ts";
import {client} from "../../api.ts";
import {CompetitionTeamsTab} from "./CompetitionTeamsTab.tsx";
import {useTranslation} from "react-i18next";
import dayjs from "dayjs";
import {CompetitionRobotGameTab} from "./CompetitionRobotGameTab.tsx";
import {CompetitionAwardsTab} from "./CompetitionAwardsTab.tsx";
import {useDocumentTitle, useSessionStorage} from "@mantine/hooks";
import {CompetitionPreviousTab} from "./CompetitionPreviousTab.tsx";
import {getCompetitionTypeColor} from "../../utils/competitionUtils.ts";
import {getCompetitionLink} from "../../utils/routingUtils.ts";
import {Carousel, type Embla} from "@mantine/carousel";

export const CompetitionDetailPage = () => {
    const { seasonId, urlPart } = useParams();
    const { i18n, t } = useTranslation();

    const [competition, setCompetition] = useState<CompetitionDetailDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [activeTab, setActiveTab] = useSessionStorage<string | null>({
        key: `competition-tab-${seasonId}-${urlPart}`,
        defaultValue: 'teams',
    });

    const [embla, setEmbla] = useState<Embla | null>(null);
    const isInitialMount = useRef(true);
    const tabsViewportRef = useRef<HTMLDivElement>(null);

    const availableTabs = useMemo(() => {
        if (!competition) return [];
        const tabs = ['teams'];
        if (competition.results) tabs.push('awards', 'robot-game');
        if (competition.previousCompetitions?.length) tabs.push('previous-competitions');
        return tabs;
    }, [competition]);

    useEffect(() => {
        if (!embla) return;

        // 1. The core function to sync height
        const syncHeight = () => {
            const index = embla.selectedScrollSnap();
            const activeSlide = embla.slideNodes()[index];
            const viewport = embla.rootNode();

            if (activeSlide && viewport) {
                const contentHeight = activeSlide.getBoundingClientRect().height;
                viewport.style.height = `${contentHeight}px`;
                viewport.style.transition = 'height 0.3s ease-in-out';
            }
        };

        // 2. The Smart Observer: Watches for internal layout changes
        const observer = new ResizeObserver(() => {
            // This fires whenever any slide changes size (e.g., toggling Matrix/Cards)
            syncHeight();
        });

        // Start observing every slide
        embla.slideNodes().forEach((slide) => observer.observe(slide));

        // 3. Keep the original 'select' event for tab-syncing and initial height
        const onSelect = () => {
            const index = embla.selectedScrollSnap();
            setActiveTab(availableTabs[index]);
            syncHeight();
        };

        embla.on('select', onSelect);

        // Initial set
        setTimeout(syncHeight, 100);

        return () => {
            embla.off('select', onSelect);
            observer.disconnect(); // Clean up the observer
        };
    }, [embla, availableTabs, setActiveTab]);


    useEffect(() => {
        // A tiny timeout ensures Mantine has finished updating the DOM's data-active attribute
        const index = availableTabs.indexOf(activeTab || 'teams');
        if (embla && index !== -1) {
            if (isInitialMount.current) {
                // 2. On first load: Jump instantly to the saved tab without animation
                embla.scrollTo(index, true);
                isInitialMount.current = false;
            } else {
                // 3. On user clicks: Animate smoothly!
                embla.scrollTo(index);
            }
        }
        const timeoutId = setTimeout(() => {
            // Find whichever tab Mantine currently marks as active
            const activeElement = document.querySelector('[role="tab"][data-active="true"]') as HTMLElement;
            const viewport = tabsViewportRef.current;

            if (activeElement && viewport) {
                // Get the physical dimensions and positions of both the tab and the scroll container
                const tabRect = activeElement.getBoundingClientRect();
                const viewportRect = viewport.getBoundingClientRect();

                // Calculate how far the tab is from the left edge of the visible scroll area
                const tabLeftRelativeToViewport = tabRect.left - viewportRect.left;

                // Calculate where the tab *should* be to be perfectly centered
                const centerOffset = (viewportRect.width / 2) - (tabRect.width / 2);

                // Scroll the container horizontally by the difference!
                viewport.scrollBy({
                    left: tabLeftRelativeToViewport - centerOffset,
                    behavior: 'smooth'
                });
            }
        }, 50);

        return () => clearTimeout(timeoutId);
    }, [activeTab, seasonId, urlPart, embla]); // Runs every time the tab changes

    useDocumentTitle(t('app.competition.detail.doc_title', {competitionName: competition?.name || '', seasonId: competition?.season?.id || ''}))


    const teamCount = competition?.registeredTeams ? competition.registeredTeams.length : 0;

    const officialUrl = `https://www.first-lego-league.org/${i18n.resolvedLanguage}/challenge-${seasonId}/${urlPart}`;

    const cleanFormattedString = (str: string) => {
        if (!str) return '';
        return str
            .split('\n')                  // Split into an array at every newline
            .map((line: string) => line.trim())     // Remove leading/trailing spaces from each line
            .filter(Boolean)              // Drop any lines that are now empty strings
            .join('\n');                  // Stitch it back together with single newlines
    };

    useEffect(() => {
        if (!seasonId || !urlPart) return;

        // Use your generated client (adjust method name if different)
        client.api.getCompetitionDetails(seasonId, urlPart)
            .then((res) => {
                setCompetition(res.data);
                setIsLoading(false);
            })
            .catch(() => {
                setError(t('app.competition.detail.error_loading'));
                setIsLoading(false);
            });
    }, [seasonId, urlPart, t]);

    if (isLoading) return <Center h="50vh"><Loader size="lg" /></Center>;
    if (error || !competition) return <Center h="50vh"><Text c="red">{error}</Text></Center>;

    return (
        <Container size="xl" py="xl">

            {competition.active === false && (
                <Alert
                    variant="light"
                    color="red"
                    title={t('app.competition.detail.inactive_title')}
                    icon={<IconInfoCircle/>}
                    mb="xl"
                >
                    {t('app.competition.detail.inactive_message')}
                </Alert>
            )}

            {/* Header Section */}
            <Group justify="space-between" align="flex-start" mb="sm">
                <Box flex={{base: '1 0 100%', xs: '1 1 min-content'}} miw={0}>
                    <Group gap="xs" mb="sm">
                        <Badge color={getCompetitionTypeColor(competition.type!)}>
                            {t(`app.competition.detail.type`, {context: competition.type})}
                        </Badge>

                        {competition.country && (
                            <Badge variant="outline" color="gray">
                                {competition.country}
                            </Badge>
                        )}

                        {competition.season && competition.season.active === false && (
                            <Badge color="gray" variant="outline">
                                {t('app.competition.detail.season', {
                                    seasonName: competition.season.name,
                                    seasonId: competition.season.id
                                })}
                            </Badge>
                        )}
                    </Group>

                    <Title order={1} mb="sm">{competition.name}</Title>

                    <SimpleGrid cols={{base: 1, sm: 2, md: 3}} spacing="md" mb="sm"
                                w={{base: '100%', xs: 'fit-content'}}>

                        {/* Date Card */}
                        <Card withBorder radius="md" p="md" bg="transparent">
                            <Group gap="xs" mb="xs" c="dimmed">
                                <IconCalendar size={20}/>
                                <Text fw={500}>{t('app.competition.detail.date')}</Text>
                            </Group>
                            <Text>{dayjs(competition.date).format('L')}</Text>
                        </Card>

                        {/* Location Card */}
                        {competition.location && (
                            <Card withBorder radius="md" p="md" bg="transparent">
                                <Group gap="xs" mb="xs" c="dimmed">
                                    <IconMapPin size={20}/>
                                    <Text fw={500}>{t('app.competition.detail.location')}</Text>
                                </Group>
                                <Text style={{whiteSpace: 'pre-line'}}>
                                    {cleanFormattedString(competition.location)}
                                </Text>
                            </Card>
                        )}

                        {/* Contact Card */}
                        {competition.contactInfo && (competition.contactInfo.contactName || competition.contactInfo.contactEmail) && (
                            <Card withBorder radius="md" p="md" bg="transparent">
                                <Group gap="xs" mb="xs" c="dimmed">
                                    <IconUser size={20}/>
                                    <Text fw={500}>{t('app.competition.detail.contact')}</Text>
                                </Group>
                                <Stack gap={0}>
                                    {competition.contactInfo.contactName && (
                                        <Text
                                            style={{whiteSpace: 'pre-line'}}>{cleanFormattedString(competition.contactInfo.contactName)}</Text>
                                    )}
                                    {competition.contactInfo.contactEmail && (
                                        <Anchor href={`mailto:${competition.contactInfo.contactEmail}`} size="sm">
                                            {competition.contactInfo.contactEmail}
                                        </Anchor>
                                    )}
                                </Stack>
                            </Card>
                        )}
                    </SimpleGrid>
                </Box>

                {/* External Links */}
                <Stack
                    gap={8}
                    pos="sticky"
                    top={80}
                    flex={{base: '1', xs: 'initial'}}
                    style={{zIndex: 10}}
                    mb="sm"
                >
                    <Button
                        component="a"
                        href={officialUrl}
                        target="_blank"
                        variant="filled" // Stands out more than the 'light' ones below
                        color={getCompetitionTypeColor(competition.type!)} // Optional: theme it to match the badge!
                        rightSection={<IconExternalLink size={16}/>}
                    >
                        {t('app.competition.detail.official_link')}
                    </Button>

                    {competition.links?.map((link, index) => (
                        <Button
                            key={index}
                            component="a"
                            href={link.url}
                            target="_blank"
                            variant="light"
                            rightSection={<IconExternalLink size={16}/>}
                        >
                            {link.label || link.url}
                        </Button>
                    ))}
                    {competition.nextCompetition && (
                        <Button
                            component={Link}
                            to={getCompetitionLink(competition.nextCompetition)} // Adjust to your actual route!
                            variant="outline"
                            color={getCompetitionTypeColor(competition.nextCompetition.type!)}
                            leftSection={<IconArrowRight size={16}/>}
                        >
                            {competition.nextCompetition.name}
                        </Button>
                    )}
                </Stack>
            </Group>

            <style>{`
                @media (hover: none), (pointer: coarse) {
                    .mantine-Tabs-tab:hover {
                        background-color: transparent !important;
                    }
                }
            `}</style>

            {/* Tabs Section */}
            <Tabs value={activeTab} onChange={setActiveTab} mt="lg">
                <ScrollArea type="never" viewportRef={tabsViewportRef}>
                    <Tabs.List style={{
                        flexWrap: 'nowrap',
                        width: 'max-content',
                        minWidth: '100%'
                    }}>
                        <Tabs.Tab value="teams"
                                  leftSection={<IconUsers size={16}/>} style={{whiteSpace: 'nowrap'}}>
                            {t('app.competition.detail.tabs.teams', {teamCount})}
                        </Tabs.Tab>
                        {competition.results && (
                            <>
                                <Tabs.Tab value="awards" leftSection={<IconTrophy size={16}/>}
                                          style={{whiteSpace: 'nowrap'}}>
                                    {t('app.competition.detail.tabs.awards')}
                                </Tabs.Tab>
                                <Tabs.Tab value="robot-game" leftSection={<IconRobot size={16}/>}
                                          style={{whiteSpace: 'nowrap'}}>
                                    {t('app.competition.detail.tabs.robotgame')}
                                </Tabs.Tab>
                            </>
                        )}

                        {competition.previousCompetitions && competition.previousCompetitions.length > 0 && (
                            <Tabs.Tab value="previous-competitions" leftSection={<IconSitemap size={16}/>}
                                      style={{whiteSpace: 'nowrap'}}>
                                {t('app.competition.detail.tabs.previous_competitions', {context: competition.type})}
                            </Tabs.Tab>
                        )}
                    </Tabs.List>
                </ScrollArea>

                {/* Tab Panels */}
                <Carousel
                    getEmblaApi={setEmbla}
                    withIndicators={false}
                    withControls={false}
                    align={'start'}
                    containScroll={'keepSnaps'}
                    mt={"md"}
                    slideGap="lg"
                    draggable={typeof window !== 'undefined' && window.matchMedia('(pointer: coarse)').matches}
                    styles={{container: {alignItems: 'flex-start'}}}
                >
                    <Carousel.Slide miw={0}>
                        <CompetitionTeamsTab competition={competition}/>
                    </Carousel.Slide>
                    {competition.results && (
                        <Carousel.Slide miw={0}>
                            <CompetitionAwardsTab competition={competition}></CompetitionAwardsTab>
                        </Carousel.Slide>
                    )}
                    {competition.results && (
                        <Carousel.Slide miw={0}>
                            <CompetitionRobotGameTab teams={competition.registeredTeams ?? []}
                                                     scores={competition.results?.robotGameEntries ?? []}></CompetitionRobotGameTab>
                        </Carousel.Slide>
                    )}
                    {competition.previousCompetitions && competition.previousCompetitions.length > 0 && (
                        <Carousel.Slide miw={0}>
                            <CompetitionPreviousTab competition={competition}></CompetitionPreviousTab>
                        </Carousel.Slide>
                    )}
                </Carousel>
            </Tabs>
        </Container>
    );
};