import {useEffect, useState} from 'react';
import {ActionIcon, Badge, Center, Group, Highlight, Loader, Menu, Pill, rem, Text} from '@mantine/core';
import {Spotlight, spotlight} from '@mantine/spotlight';
import {IconCalendar, IconFilter, IconSearch} from '@tabler/icons-react';
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
import {SeasonTeamAvatar} from "../common/team/avatar/SeasonTeamAvatar.tsx";
import {ProfileAvatar} from "../common/team/avatar/ProfileAvatar.tsx";

export function GlobalSearchSpotlight() {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const [searchQuery, setSearchQuery] = useState('');
    const [debouncedQuery] = useDebouncedValue(searchQuery, 300);

    // In the future, we will initialize this from your Router/Context
    const { activeSeason, availableSeasonIds } = useAppContext();
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
        <Menu shadow="md" width={150} position="bottom-end" withinPortal zIndex={1001}>
            <Menu.Target>
                <ActionIcon variant="subtle" color="gray">
                    <IconFilter style={{ width: rem(18), height: rem(18) }} />
                </ActionIcon>
            </Menu.Target>
            <Menu.Dropdown>
                <Menu.Label>{t("app.search.select_season")}</Menu.Label>
                {availableSeasonIds.map((season) => (
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
            zIndex={1000} // Fix 1: Lower z-index so tooltips (1100) work
            onSpotlightClose={() => {
                setSearchQuery('');
                setSearchResults([]);
                setSeasonContext(activeSeason);
            }}
        >
            <Spotlight.Search
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
                        {availableSeasonIds.map((season) => (
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

                    // The "Grid Clamp": forces the middle column to occupy only the remaining space
                    const gridItemStyle: React.CSSProperties = {
                        display: 'grid',
                        gridTemplateColumns: 'auto 1fr', // Column 1: Avatar/Icon, Column 2: Content
                        alignItems: 'center',
                        gap: 'var(--mantine-spacing-sm)',
                        width: '100%',
                        overflow: 'hidden',
                    };

                    const textContainerStyle: React.CSSProperties = {
                        minWidth: 0, // CRITICAL: Allows the grid cell to shrink below content size
                        overflow: 'hidden',
                    };

                    // --- COMPETITIONS ---
                    if (result.type === SearchResultTypeDto.COMPETITION && result.competition) {
                        const comp = result.competition;
                        return (
                            <Spotlight.Action key={index} onClick={() => { navigate(`/competition/${comp.season.id}/${comp.urlPart}`); spotlight.close(); }}>
                                <div style={gridItemStyle}>
                                    <CompetitionTypeIcon type={comp.type} size={36} />

                                    <div style={textContainerStyle}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                                            <Highlight
                                                highlight={searchQuery}
                                                fw={500}
                                                truncate="end"
                                                style={{ flex: 1, minWidth: 0 }}
                                            >
                                                {comp.name}
                                            </Highlight>

                                            <Group gap="xs" wrap="nowrap" style={{ flexShrink: 0 }}>
                                                {!seasonContext && <SeasonBadge season={comp.season} variant="light" short/>}
                                                {comp.date && (
                                                    <Group gap={2}>
                                                        <IconCalendar size={14} style={{ color: 'var(--mantine-color-dimmed)' }} />
                                                        <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap' }}>{getFormattedCompetitionDate(comp)}</Text>
                                                    </Group>
                                                )}
                                            </Group>
                                        </div>
                                        <Text size="xs" c="dimmed" mt={2} truncate="end">
                                            <CompetitionTypeBadge type={comp.type} variant="light" size="xs" />
                                        </Text>
                                    </div>
                                </div>
                            </Spotlight.Action>
                        );
                    }

                    // --- TEAMS ---
                    if (result.type === SearchResultTypeDto.SEASON_TEAM && result.seasonTeam) {
                        const team = result.seasonTeam;

                        // Safely combine city and institution for the subtitle
                        const subtitle = [team.institution, team.city].filter(Boolean).join(' • ');

                        return (
                            <Spotlight.Action key={index} onClick={() => { navigate(`/team/${team.season.id}/${team.fllId}`); spotlight.close(); }}>
                                <div style={gridItemStyle}>
                                    <SeasonTeamAvatar team={team} size={36}/>

                                    <div style={textContainerStyle}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                                            <Highlight
                                                highlight={searchQuery}
                                                fw={500}
                                                truncate="end"
                                                style={{ flex: 1, minWidth: 0 }}
                                            >
                                                {team.name}
                                            </Highlight>

                                            <Group gap="xs" wrap="nowrap" style={{ flexShrink: 0 }}>
                                                {!seasonContext && <SeasonBadge season={team.season} variant="light" short/>}
                                                <Highlight highlight={searchQuery} component={Badge} variant="light">
                                                    {`#${team.fllId}`}
                                                </Highlight>
                                            </Group>
                                        </div>

                                        {subtitle && (
                                            <Highlight
                                                highlight={searchQuery}
                                                size="xs"
                                                c="dimmed"
                                                mt={2}
                                                truncate="end"
                                                style={{ display: 'block' }}
                                            >
                                                {subtitle}
                                            </Highlight>
                                        )}
                                    </div>
                                </div>
                            </Spotlight.Action>
                        );
                    }

                    // --- TEAM PROFILES ---
                    if (result.type === SearchResultTypeDto.TEAM_PROFILE && result.teamProfile) {
                        const profile = result.teamProfile;
                        return (
                            <Spotlight.Action key={index} onClick={() => { navigate(`/${profile.profileUrl}`); spotlight.close(); }}>
                                <div style={gridItemStyle}>
                                    <ProfileAvatar avatarUrl={profile.avatarUrl} size={36}/>
                                    <div style={textContainerStyle}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                                            <Highlight highlight={searchQuery} fw={500} truncate="end" style={{ flex: 1, minWidth: 0 }}>
                                                {profile.profileName}
                                            </Highlight>
                                            <Badge variant="dot" color="blue" size="sm"  style={{ flexShrink: 0 }}>
                                                {t("app.search.result.profile")}
                                            </Badge>
                                        </div>
                                    </div>
                                </div>
                            </Spotlight.Action>
                        );
                    }
                    return null;
                })}
            </Spotlight.ActionsList>
        </Spotlight.Root>
    );
}