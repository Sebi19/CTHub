import {useEffect, useState} from 'react';
import {ActionIcon, Avatar, Badge, Center, Group, Highlight, Loader, Menu, Pill, rem, Text} from '@mantine/core';
import {Spotlight, spotlight} from '@mantine/spotlight';
import {IconCalendar, IconFilter, IconRobot, IconSearch, IconUser} from '@tabler/icons-react';
import {useNavigate} from 'react-router-dom';
import {useDebouncedValue} from '@mantine/hooks';
import {client} from '../../api';
import {type SearchResultItemDto, SearchResultTypeDto} from "../../api/generated.ts";
import {SeasonBadge} from "../common/season/SeasonBadge.tsx";
import {CompetitionTypeBadge} from "../common/competition/CompetitionTypeBadge.tsx";
import {getFormattedCompetitionDate} from "../../utils/competitionUtils.ts";
import {CompetitionTypeIcon} from "../common/competition/CompetitionTypeIcon.tsx";
import { useTranslation } from 'react-i18next';
import { useAppContext } from '../../hooks/AppContext.tsx';

export function GlobalSearchSpotlight() {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const [searchQuery, setSearchQuery] = useState('');
    const [debouncedQuery] = useDebouncedValue(searchQuery, 300);

    // In the future, we will initialize this from your Router/Context
    const { activeSeason } = useAppContext();
    const [seasonContext, setSeasonContext] = useState<string | undefined>(activeSeason);

    useEffect(() => {
        setSeasonContext(activeSeason);
    }, [activeSeason]);

    const [searchResults, setSearchResults] = useState<SearchResultItemDto[]>([]);
    const [isSearching, setIsSearching] = useState(false);

    useEffect(() => {
        if (debouncedQuery.trim().length < 2) {
            setSearchResults([]);
            return;
        }

        let ignore = false;
        setIsSearching(true);

        client.api.globalSearch({ q: debouncedQuery, seasonId: seasonContext })
            .then((res) => {
                if (!ignore) setSearchResults(res.data);
            })
            .catch((err) => {
                if (!ignore) {
                    console.error("Global search failed:", err);
                    setSearchResults([]);
                }
            })
            .finally(() => {
                if (!ignore) setIsSearching(false);
            });

        return () => { ignore = true; };
    }, [debouncedQuery, seasonContext]);

    const handleRemoveSeason = () => setSeasonContext(undefined);

    // The dynamic left section: Pill, Spinner, or Search Icon
    const searchLeftSection = seasonContext ? (
        <Group gap={0} wrap="nowrap" px="xs">
            <Pill withRemoveButton onRemove={handleRemoveSeason} size="md" style={{ cursor: 'pointer' }}>
                {seasonContext}
            </Pill>
            {isSearching && <Loader size="xs" ml="xs" color="gray" />}
        </Group>
    ) : isSearching ? (
        <Loader size="xs" color="gray" />
    ) : (
        <IconSearch style={{ width: rem(18), height: rem(18) }} stroke={1.5} />
    );

    const searchRightSection = (
        <Menu shadow="md" width={150} position="bottom-end" withinPortal zIndex={1000000}>
            <Menu.Target>
                <ActionIcon variant="subtle" color="gray">
                    <IconFilter style={{ width: rem(18), height: rem(18) }} />
                </ActionIcon>
            </Menu.Target>
            <Menu.Dropdown>
                <Menu.Label>{t("app.search.select_season")}</Menu.Label>
                {['2025-26', '2024-25', '2023-24', '2022-23'].map((season) => (
                    <Menu.Item key={season} onClick={() => setSeasonContext(season)}>
                        {season}
                    </Menu.Item>
                ))}
            </Menu.Dropdown>
        </Menu>
    );

    return (
        <Spotlight.Root
            shortcut={['mod + K', '/']}
            query={searchQuery}
            onQueryChange={setSearchQuery}
            onSpotlightClose={() => {
                setSearchQuery('');
                setSearchResults([]);
                setSeasonContext(activeSeason);
            }}
        >
            <Spotlight.Search
                //value={searchQuery}
                //onChange={(event) => setSearchQuery(event.currentTarget.value)}
                onKeyDown={(e) => {
                    if (e.key === 'Backspace' && searchQuery === '' && seasonContext) {
                        setSeasonContext(undefined);
                    }
                }}
                placeholder={seasonContext ? t("app.search.placeholder_season") : t("app.search.placeholder_global")}
                leftSection={searchLeftSection}
                leftSectionWidth={seasonContext ? (isSearching ? 140 : 110) : 40}
                leftSectionPointerEvents="all"
                rightSection={searchRightSection}
                rightSectionPointerEvents="all"
            />

            <Spotlight.ActionsList>
                {/* 1. Empty / Initial States */}
                {!seasonContext && searchQuery.trim().length === 0 && (
                    <Spotlight.ActionsGroup label={t("app.search.season_filter_label")}>
                        {['2025-26', '2024-25', '2023-24', '2022-23'].map((season) => (
                            <Spotlight.Action
                                key={season}
                                closeSpotlightOnTrigger={false}
                                onClick={() => {
                                    setSeasonContext(season);
                                    setSearchQuery('');
                                }}
                            >
                                <Group gap="sm">
                                    <IconFilter style={{ width: rem(18), height: rem(18) }} color="var(--mantine-color-dimmed)" />
                                    <Text fw={500}>{season}</Text>
                                </Group>
                            </Spotlight.Action>
                        ))}
                    </Spotlight.ActionsGroup>
                )}

                {searchQuery.trim().length === 0 && (
                    <Spotlight.Empty>
                        <Center c="dimmed" p="xl">{t("app.search.instructions")}</Center>
                    </Spotlight.Empty>
                )}

                {searchQuery.trim().length >= 2 && searchResults.length === 0 && !isSearching && (
                    <Spotlight.Empty>
                        <Center c="dimmed" p="xl">{t("app.search.no_results", {query: searchQuery})}</Center>
                    </Spotlight.Empty>
                )}

                {/* 2. The Unified Search Results Loop */}
                {searchResults.map((result, index) => {

                    // --- COMPETITIONS ---
                    if (result.type === SearchResultTypeDto.COMPETITION && result.competition) {
                        const comp = result.competition;
                        return (
                            <Spotlight.Action
                                key={index}
                                onClick={() => {
                                    navigate(`/competition/${comp.season.id}/${comp.urlPart}`);
                                    spotlight.close();
                                }}
                            >
                                <Group wrap="nowrap" w="100%">
                                    {/* 1. The Icon (Moved inside the layout) */}
                                        <CompetitionTypeIcon type={comp.type} size={36} />

                                    {/* 2. The Content */}
                                    <div style={{ flex: 1 }}>
                                        <Group justify="space-between" wrap="nowrap">
                                            <Highlight highlight={searchQuery} fw={500} truncate="end">{comp.name}</Highlight>

                                            <Group gap="xs" wrap="nowrap">
                                                {/* Show Season ID if we are searching globally! */}
                                                {!seasonContext && (
                                                    <SeasonBadge season={comp.season} variant="light" short/>
                                                )}
                                                {comp.date &&
                                                    <Group gap={2}>
                                                        <IconCalendar size={14} style={{ color: 'var(--mantine-color-dimmed)' }} />
                                                        <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap' }}>{getFormattedCompetitionDate(comp)}</Text>
                                                    </Group>
                                                }
                                            </Group>
                                        </Group>

                                        {/* Competition Type / Country Subtitle */}
                                        <Text size="xs" c="dimmed" mt={2}>
                                            <CompetitionTypeBadge type={comp.type} variant="light" size="xs" />
                                        </Text>
                                    </div>
                                </Group>
                            </Spotlight.Action>
                        );
                    }

                    // --- TEAMS ---
                    if (result.type === SearchResultTypeDto.SEASON_TEAM && result.seasonTeam) {
                        const team = result.seasonTeam;

                        // Safely combine city and institution for the subtitle
                        const subtitle = [team.institution, team.city].filter(Boolean).join(' • ');

                        return (
                            <Spotlight.Action
                                key={index}
                                onClick={() => {
                                    navigate(`/team/${team.season.id}/${team.fllId}`);
                                    spotlight.close();
                                }}
                            >
                                <Group wrap="nowrap" w="100%">
                                    <Avatar src={team.seasonTeamProfile?.seasonAvatarUrl} size={36} radius="xl" color="blue">
                                        <IconRobot/>
                                    </Avatar>
                                    <div style={{ flex: 1 }}>
                                        <Group justify="space-between" wrap="nowrap">
                                            <Highlight highlight={searchQuery} fw={500} truncate="end">{team.name}</Highlight>

                                            <Group gap="xs" wrap="nowrap">
                                                {!seasonContext && (
                                                    <SeasonBadge season={team.season} variant="light" short/>
                                                )}
                                                <Highlight highlight={searchQuery} component={Badge} variant="light" color="gray">
                                                    {`#${team.fllId}`}
                                                </Highlight>
                                            </Group>
                                        </Group>

                                        {/* City & Institution Subtitle */}
                                        {subtitle && (
                                            <Highlight highlight={searchQuery} size="xs" c="dimmed" mt={2} truncate="end">
                                                {subtitle}
                                            </Highlight>
                                        )}
                                    </div>
                                </Group>
                            </Spotlight.Action>
                        );
                    }

                    // --- TEAM PROFILES ---
                    if (result.type === SearchResultTypeDto.TEAM_PROFILE && result.teamProfile) {
                        const profile = result.teamProfile;
                        return (
                            <Spotlight.Action
                                key={index}
                                onClick={() => {
                                    navigate(`/${profile.profileUrl}`);
                                    spotlight.close();
                                }}
                            >
                                <Group wrap="nowrap" w="100%">
                                    <Avatar src={profile.avatarUrl} size={36} radius="xl" color="blue">
                                        <IconUser />
                                    </Avatar>

                                    <div style={{ flex: 1 }}>
                                        <Group justify="space-between" wrap="nowrap">
                                            <Highlight highlight={searchQuery} fw={500} truncate="end">{profile.profileName}</Highlight>
                                            <Badge variant="dot" color="blue" size="sm">{t("app.search.result.profile")}</Badge>
                                        </Group>
                                    </div>
                                </Group>
                            </Spotlight.Action>
                        );
                    }

                    return null;
                })}
            </Spotlight.ActionsList>
        </Spotlight.Root>
    );
}