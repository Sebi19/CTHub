import {useEffect, useMemo, useState} from 'react';
import {Link, useLocation, useNavigate, useParams} from 'react-router-dom';
import {
    Container,
    Title,
    Text,
    Group,
    Badge,
    Loader,
    Button,
    Center,
    Stack,
    Alert,
    Anchor,
    Card, SimpleGrid, Box
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
import {CompetitionRobotGameTab} from "./CompetitionRobotGameTab.tsx";
import {CompetitionAwardsTab} from "./CompetitionAwardsTab.tsx";
import {useDocumentTitle, useSessionStorage} from "@mantine/hooks";
import {CompetitionPreviousTab} from "./CompetitionPreviousTab.tsx";
import {getCompetitionTypeColor, getFormattedCompetitionDate} from "../../utils/competitionUtils.ts";
import {getCompetitionLink, getCompetitionsListLink, navigateBack} from "../../utils/routingUtils.ts";
import { SeasonBadge } from '../common/season/SeasonBadge.tsx';
import {CompetitionTypeBadge} from "../common/competition/CompetitionTypeBadge.tsx";
import {type SwipeableTabItem, SwipeableTabs} from "../common/layout/SwipeableTabs.tsx";
import {NotFoundPage} from "../error/NotFoundPage.tsx";
import {ServerErrorPage} from "../error/ServerErrorPage.tsx";
import {NavigateBackButton} from "../common/navigation/NavigateBackButton.tsx";
import {useAppContext} from "../../hooks/AppContext.tsx";

export const CompetitionDetailPage = () => {
    const { seasonId, urlPart } = useParams();
    const { setActiveSeason, globalDefaultSeason } = useAppContext();
    const navigate = useNavigate();
    const location = useLocation();
    const { i18n, t } = useTranslation();

    const [competition, setCompetition] = useState<CompetitionDetailDto | null>(null);
    const [errorCode, setErrorCode] = useState<number | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        if (seasonId) {
            setActiveSeason(seasonId);
        }

        return () => {
            setActiveSeason(globalDefaultSeason);
        };
    }, [seasonId, setActiveSeason, globalDefaultSeason]);

    const teamCount = competition ? competition.registeredTeams.length : 0;

    useDocumentTitle(t('app.competition.detail.doc_title', {competitionName: competition?.name || '', seasonId: competition?.season.id || ''}))

    const tabItems = useMemo<SwipeableTabItem[]>(() => {
        if (!competition) return [];

        const items: SwipeableTabItem[] = [
            {
                value: 'teams',
                label: t('app.competition.detail.tabs.teams', { teamCount }),
                Icon: IconUsers,
                content: <CompetitionTeamsTab competition={competition} />
            }
        ];

        if (competition.results) {
            items.push({
                value: 'awards',
                label: t('app.competition.detail.tabs.awards'),
                Icon: IconTrophy,
                content: <CompetitionAwardsTab competition={competition} />
            });
            items.push({
                value: 'robot-game',
                label: t('app.competition.detail.tabs.robotgame'),
                Icon: IconRobot,
                content: <CompetitionRobotGameTab competition={competition} />
            });
        }

        if (competition.previousCompetitions?.length) {
            items.push({
                value: 'previous-competitions',
                label: t('app.competition.detail.tabs.previous_competitions', { context: competition.type, competitionCount: competition.previousCompetitions.length }),
                Icon: IconSitemap,
                content: <CompetitionPreviousTab competition={competition} />
            });
        }

        return items;
    }, [competition, t, teamCount]);

    const [activeTab, setActiveTab] = useSessionStorage<string | null>({
        key: `competition-tab-${seasonId}-${urlPart}`,
        defaultValue: null,
    });

    const currentTab = activeTab || (competition?.results ? 'awards' : 'teams');

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
            .catch((error) => {
                const status = error.response?.status || 500;

                setErrorCode(status);
                setIsLoading(false);
            });
    }, [seasonId, urlPart, t]);

    const handleBackNavigation = () => {
        navigateBack(location, navigate, getCompetitionsListLink(seasonId))
    };

    if (isLoading) return <Center h="50vh"><Loader size="lg" /></Center>;

    if (errorCode === 404) {
        return <NotFoundPage handleBackNavigation={handleBackNavigation} />;
    }

    // Catch 500s, 502s, network timeouts, etc.
    if (errorCode || !competition) {
        return <ServerErrorPage handleBackNavigation={handleBackNavigation} />;
    }

    return (
        <Container size="xl" py="xl">
            {/* Top Navigation */}
            <NavigateBackButton handleBackNavigation={handleBackNavigation} />

            {!competition.active && (
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
                        <SeasonBadge season={competition.season} hideIfActive/>

                        <CompetitionTypeBadge type={competition.type} />

                        {competition.country && (
                            <Badge variant="outline" color="gray">
                                {competition.country}
                            </Badge>
                        )}

                    </Group>

                    <Title order={1} mb="sm">{competition.name}</Title>

                    <SimpleGrid cols={{base: 1, sm: 2, md: 3}} spacing="md" mb="sm"
                                w={{base: '100%', xs: 'fit-content'}}>

                        {/* Date Card */}
                        {competition.date && (
                            <Card withBorder radius="md" p="md" bg="transparent">
                                <Group gap="xs" mb="xs" c="dimmed">
                                    <IconCalendar size={20}/>
                                    <Text fw={500}>{t('app.competition.detail.date')}</Text>
                                </Group>
                                    <Text>{getFormattedCompetitionDate(competition)}</Text>
                            </Card>
                        )}

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
                        color={getCompetitionTypeColor(competition.type)} // Optional: theme it to match the badge!
                        rightSection={<IconExternalLink size={16}/>}
                    >
                        {t('app.competition.detail.official_link')}
                    </Button>

                    {competition.links.map((link, index) => (
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
                            color={getCompetitionTypeColor(competition.nextCompetition.type)}
                            leftSection={<IconArrowRight size={16}/>}
                        >
                            {competition.nextCompetition.name}
                        </Button>
                    )}
                </Stack>
            </Group>

            <SwipeableTabs value={currentTab} onChange={setActiveTab} items={tabItems}/>
        </Container>
    );
};