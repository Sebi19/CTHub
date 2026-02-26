import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
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
    Card, SimpleGrid, Box
} from '@mantine/core';
import {
    IconCalendar,
    IconMapPin,
    IconExternalLink,
    IconRobot,
    IconTrophy,
    IconUsers,
    IconInfoCircle, IconUser
} from '@tabler/icons-react';

// Import our new tab components
import type {CompetitionDetailDto} from "../../api/generated.ts";
import {client} from "../../api.ts";
import {CompetitionDetailTeamsTab} from "./CompetitionDetailTeamsTab.tsx";
import {useTranslation} from "react-i18next";
import dayjs from "dayjs";

export const CompetitionDetailPage = () => {
    const { seasonId, urlPart } = useParams();
    const { i18n, t } = useTranslation();

    const [competition, setCompetition] = useState<CompetitionDetailDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const teamCount = competition?.registeredTeams ? competition.registeredTeams.length : 0;

    const officialUrl = `https://www.first-lego-league.org/${i18n.language}/challenge-${seasonId}/${urlPart}`;

    const getCompetitionTypeColor = (type: string) => {
        switch (type.toLowerCase()) {
            case 'regional':
                return 'green';
            case 'qualification':
                return 'orange';
            case 'final':
                return 'red';
            default:
                return 'gray';
        }
    }

    const cleanLocationString = (loc: string) => {
        if (!loc) return '';
        return loc
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
                    icon={<IconInfoCircle />}
                    mb="xl"
                >
                    {t('app.competition.detail.inactive_message')}
                </Alert>
            )}

            {/* Header Section */}
                <Group justify="space-between" align="flex-start" mb="sm">
                <Box style={{ flex: '1 1 min-content', minWidth: 0 }}>
                    <Group gap="xs" mb="sm">
                        <Badge color={getCompetitionTypeColor(competition.type!)}>
                            {t(`app.competition.detail.type.${competition.type!}`)}
                        </Badge>

                        {competition.country && (
                            <Badge variant="outline" color="gray">
                                {competition.country}
                            </Badge>
                        )}

                        {competition.season && competition.season.active === false && (
                            <Badge color="gray" variant="outline">
                                {t('app.competition.detail.season', {seasonName: competition.season.name, seasonId: competition.season.id})}
                            </Badge>
                        )}
                    </Group>

                    <Title order={1} mb="sm">{competition.name}</Title>

                    <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md" mb="sm" w={{ base: '100%', xs: 'fit-content' }}>

                        {/* Date Card */}
                        <Card withBorder radius="md" p="md" bg="transparent">
                            <Group gap="xs" mb="xs" c="dimmed">
                                <IconCalendar size={20} />
                                <Text fw={500}>{t('app.competition.detail.date')}</Text>
                            </Group>
                            <Text>{dayjs(competition.date).format('L')}</Text>
                        </Card>

                        {/* Location Card */}
                        {competition.location && (
                            <Card withBorder radius="md" p="md" bg="transparent">
                                <Group gap="xs" mb="xs" c="dimmed">
                                    <IconMapPin size={20} />
                                    <Text fw={500}>{t('app.competition.detail.location')}</Text>
                                </Group>
                                <Text style={{ whiteSpace: 'pre-line' }}>
                                    {cleanLocationString(competition.location)}
                                </Text>
                            </Card>
                        )}

                        {/* Contact Card */}
                        {competition.contactInfo && (competition.contactInfo.contactName || competition.contactInfo.contactEmail) && (
                            <Card withBorder radius="md" p="md" bg="transparent">
                                <Group gap="xs" mb="xs" c="dimmed">
                                    <IconUser size={20} />
                                    <Text fw={500}>{t('app.competition.detail.contact')}</Text>
                                </Group>
                                <Stack gap={0}>
                                    {competition.contactInfo.contactName && (
                                        <Text>{competition.contactInfo.contactName}</Text>
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
                {competition.links && competition.links.length > 0 && (
                    <Stack
                        gap={5}
                        pos="sticky"
                        top={80}
                        flex={{base: '1', xs: 'initial'}}
                        style={{ zIndex: 10}}
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
                    </Stack>
                )}
            </Group>

            {/* Tabs Section */}
            <Tabs defaultValue="teams" mt="lg">
                <Tabs.List>
                    <Tabs.Tab value="teams" leftSection={<IconUsers size={16} />}>
                        {t('app.competition.detail.tabs.teams', {teamCount})}
                    </Tabs.Tab>
                    <Tabs.Tab value="awards" leftSection={<IconTrophy size={16} />} disabled={!competition.results}>
                        {t('app.competition.detail.tabs.awards')}
                    </Tabs.Tab>
                    <Tabs.Tab value="robot-game" leftSection={<IconRobot size={16} />} disabled={!competition.results}>
                        {t('app.competition.detail.tabs.robotgame')}
                    </Tabs.Tab>
                </Tabs.List>

                {/* Tab Panels */}
                <Tabs.Panel value="teams" pt="md">
                    <CompetitionDetailTeamsTab competition={competition} />
                </Tabs.Panel>

                <Tabs.Panel value="awards" pt="md">
                    {/* <CompetitionAwardsTab competition={competition} /> */}
                    <Text>This is the awards tab. All the overall results are here!</Text>
                </Tabs.Panel>

                <Tabs.Panel value="robot-game" pt="md">
                    {/* <CompetitionRobotGameTab competition={competition} /> */}
                    <Text>This is the robotgame tab. Here you would see the detailed robotgame results.</Text>
                </Tabs.Panel>
            </Tabs>
        </Container>
    );
};