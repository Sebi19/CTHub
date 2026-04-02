import {
    Container,
    Title,
    Text,
    Group,
    Stack,
    Paper,
    Center,
    Loader,
    ThemeIcon
} from '@mantine/core';
import {IconBuildingBank, IconCalendarStats, IconMapPin} from '@tabler/icons-react';
import {useLocation, useNavigate, useParams} from 'react-router-dom';
import {SeasonTeamDetails} from "../seasonTeam/SeasonTeamDetails.tsx";
import {useEffect, useMemo, useState} from "react";
import type {TeamProfileDetailsDto} from "../../api/generated.ts";
import {useDocumentTitle} from "@mantine/hooks";
import {client} from "../../api.ts";
import {useTranslation} from "react-i18next";
import dayjs from "dayjs";
import {getCompetitionsListLink, navigateBack} from "../../utils/routingUtils.ts";
import {ProfileAvatar} from "../common/team/avatar/ProfileAvatar.tsx";
import {type SwipeableTabItem, SwipeableTabs} from "../common/layout/SwipeableTabs.tsx";
import {NotFoundPage} from "../error/NotFoundPage.tsx";
import {ServerErrorPage} from "../error/ServerErrorPage.tsx";
import {NavigateBackButton} from "../common/navigation/NavigateBackButton.tsx";
import {useAppContext} from "../../hooks/AppContext.tsx";

export const TeamProfileDetailPage = () => {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const location = useLocation();
    const { teamProfileUrl, seasonId } = useParams();
    const { setActiveSeason, globalDefaultSeason } = useAppContext();

    const [profile, setProfile] = useState<TeamProfileDetailsDto | null>(null);
    const [errorCode, setErrorCode] = useState<number | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const [activeTab, setActiveTab] = useState<string>(seasonId || 'profile');

    useEffect(() => {
        if (seasonId) {
            setActiveSeason(seasonId);
        }

        return () => {
            setActiveSeason(globalDefaultSeason);
        };
    }, [seasonId, setActiveSeason, globalDefaultSeason]);

    useEffect(() => {
        const urlTab = seasonId || 'profile';
        if (activeTab !== urlTab) {
            setActiveTab(urlTab);
        }
    }, [seasonId]);

    useEffect(() => {
        setProfile(null);
        setErrorCode(null);
        setIsLoading(true);
        if (!teamProfileUrl) return;

        // Hook this up to your generated API client method!
        client.api.getTeamProfileDetails(teamProfileUrl)
            .then((res) => {
                setProfile(res.data);
                setIsLoading(false);
            })
            .catch((error) => {
                const status = error.response?.status || 500;

                setErrorCode(status);
                setIsLoading(false);
            });
    }, [teamProfileUrl]);

    useDocumentTitle(t('app.team_profile.detail.doc_title', {profileName: profile?.profileName || ''}))

    const tabItems = useMemo<SwipeableTabItem[]>(() => {
        if (!profile) return [];

        // Sort seasons descending (newest first)
        const sortedSeasons = [...profile.seasons].sort((a, b) =>
            dayjs(b.season.startYear).diff(dayjs(a.season.startYear))
        );

        // Grab the most recent season to show their current city/institution
        const latestSeason = sortedSeasons[0];

        // TODO: Extract into a seperate component
        const teamProfileTab = (
            <Paper shadow="md" radius="lg" p="xl" mb="xl" withBorder>
                <Group wrap="nowrap" align="flex-start">
                    <ProfileAvatar
                        avatarUrl={profile.avatarUrl}
                        size={120}
                        radius="50%"
                        color="initials"
                    />

                    <Stack gap="xs">
                        <Title order={1}>{profile.profileName}</Title>

                        {latestSeason && (
                            <Group gap="xl" c="dimmed" mt="xs">
                                <Group gap="xs" justify="center">
                                    <IconBuildingBank size={18} />
                                    <Text size="sm">{latestSeason.institution}</Text>
                                </Group>
                                <Group gap="xs" justify="center">
                                    <IconMapPin size={18} />
                                    <Text size="sm">{latestSeason.city}, {latestSeason.country}</Text>
                                </Group>
                            </Group>
                        )}

                        {/* A fun little summary stat */}
                        <Group gap="xs">
                            <ThemeIcon variant={"light"} color={"blue"} radius="md">
                                <IconCalendarStats size={16} opacity={0.6} />
                            </ThemeIcon>
                            <Text size="sm" fw={500}>{t("app.team_profile.detail.seasons_on_record", {count: profile.seasons.length})}</Text>
                        </Group>
                    </Stack>
                </Group>
            </Paper>
        );

        return [
            {
                value: 'profile',
                label: t('app.team_profile.detail.profile_tab'),
                content: teamProfileTab
            },
            ...sortedSeasons.map((season) => ({
                value: season.season.id,
                label: `${season.season.id} (${season.season.name})`,
                content: <SeasonTeamDetails teamDetails={season} hideSeasonBadge={true} />
            })),
        ];
    }, [profile, t]);

    const handleTabChange = (newTab: string | null) => {
        if (!newTab) return;

        // Update the UI instantly so Embla can animate without stuttering
        setActiveTab(newTab);

        if (newTab === 'profile') {
            setActiveSeason(globalDefaultSeason);
        } else {
            setActiveSeason(newTab);
        }

        const nextUrl = newTab === 'profile'
            ? `/${teamProfileUrl}`
            : `/${teamProfileUrl}/${newTab}`;

        window.history.replaceState(null, '', nextUrl);
    };

    const handleBackNavigation = () => {
        navigateBack(location, navigate, getCompetitionsListLink())
    }


    if (isLoading) return <Center h="50vh"><Loader /></Center>;

    if (errorCode === 404) {
        return <NotFoundPage handleBackNavigation={handleBackNavigation} />;
    }

    // Catch 500s, 502s, network timeouts, etc.
    if (errorCode || !profile) {
        return <ServerErrorPage handleBackNavigation={handleBackNavigation} />;
    }

    return (
        <Container size="xl" py="xl">
            <NavigateBackButton handleBackNavigation={handleBackNavigation} />

            <SwipeableTabs value={activeTab} onChange={handleTabChange} items={tabItems} />
        </Container>
    );
};