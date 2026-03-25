import {Badge, Box, Button, Collapse, Group, Stack, Text, Timeline, Title} from "@mantine/core";
import {IconBuildingBank, IconChevronDown, IconChevronUp, IconExternalLink, IconMapPin} from "@tabler/icons-react";
import {parseTeamLink} from "../../utils/linkUtils.tsx";
import {CompetitionRecordCard} from "./CompetitionRecordCard.tsx";
import type {SeasonTeamDetailsDto} from "../../api/generated.ts";
import dayjs from "dayjs";
import {SeasonBadge} from "../common/season/SeasonBadge.tsx";
import {useTranslation} from "react-i18next";
import {CompetitionTypeBadge} from "../common/competition/CompetitionTypeBadge.tsx";
import {CompetitionTypeIcon} from "../common/competition/CompetitionTypeIcon.tsx";
import {SeasonTeamAvatar} from "../common/team/avatar/SeasonTeamAvatar.tsx";
import {useState} from "react";
import {TeamRobotgameStats} from "../common/team/TeamRobotgameStats.tsx";

interface SeasonTeamDetailsProps {
    teamDetails: SeasonTeamDetailsDto;
    hideSeasonBadge?: boolean;
}
export const SeasonTeamDetails = ({teamDetails, hideSeasonBadge}: SeasonTeamDetailsProps) => {
    const {t} = useTranslation();
    // Sort competitions by date (oldest first, so it reads like a journey)
    const sortedRecords = [...(teamDetails.competitionRecords)].sort((a, b) =>
        dayjs(a.competition?.date).diff(dayjs(b.competition?.date))
    );

    const [expanded, setExpanded] = useState(false);
    const INITIAL_COUNT = 3;
    const hasMore = teamDetails.links.length > INITIAL_COUNT;

    // Determine which links to show immediately vs. which to hide
    const visibleLinks = teamDetails.links.slice(0, INITIAL_COUNT).map(
        link => parseTeamLink(link.url, link.label)
    )
    const hiddenLinks = teamDetails.links.slice(INITIAL_COUNT).map(
        link => parseTeamLink(link.url, link.label)
    )

    return (
        <Box>
            <Group justify="space-between" align="flex-start" mb="xl">
                <Box flex={{base: '1 0 100%', sm: '1 1 min-content'}} miw={0}>
                    <Group gap="xs" mb="sm">
                        {!hideSeasonBadge && (
                                <SeasonBadge season={teamDetails.season} hideIfActive/>
                        )}

                        <Badge size="lg" variant="light" color="blue">#{teamDetails.fllId}</Badge>

                        {teamDetails.country && (
                            <Badge variant="outline" color="gray">
                                {teamDetails.country}
                            </Badge>
                        )}
                    </Group>
                    <Group wrap='wrap'>
                        <SeasonTeamAvatar
                            team={teamDetails}
                            w={{ base: 80, sm: 120 }}
                            h={{ base: 80, sm: 120 }}
                            style={{
                                flexShrink: 1, // 1. Permission to shrink when space gets tight!
                                minWidth: 80,  // 2. The hard floor. Stop shrinking at 80px.
                                minHeight: 80, // (Keep it a perfect circle)
                            }}
                            hideNoProfile/>
                        <Stack gap={0} flex="1 1 0%" style={{
                            // This is the magic key. It creates a ceiling for the flex item's
                            // automatic minimum width calculation.
                            maxWidth: '100%'
                        }}>
                            <Title
                                order={1}
                                mb="xs"
                                style={{
                                    overflowWrap: 'break-word',
                                }}>
                                {teamDetails.name}
                            </Title>
                            <Group gap="xs" c="dimmed" wrap='wrap'>
                                {teamDetails.institution && (
                                    <Group gap="xs" wrap='nowrap'>
                                        <IconBuildingBank size={16} style={{ flexShrink: 0 }} />
                                        <Text>{teamDetails.institution}</Text>
                                    </Group>
                                )}
                                {teamDetails.city && (
                                    <Group gap="xs" wrap='nowrap'>
                                        <IconMapPin size={16} style={{ flexShrink: 0 }} />
                                        <Text>{teamDetails.city}</Text>
                                    </Group>
                                )}
                            </Group>
                        </Stack>
                    </Group>

                </Box>

                <Stack
                    gap={8}
                    pos="sticky"
                    top={80}
                    flex={{base: '1', sm: 'initial'}}
                    maw={{ base: '100%', sm: 350 }}
                    style={{zIndex: 10}}
                    mb="sm"
                >
                    {visibleLinks.map((link, index) => {
                        return (
                            <Button
                                key={index}
                                component="a"
                                href={link.cleanUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                variant="light"
                                color={link.color}
                                leftSection={link.icon}
                                rightSection={<IconExternalLink size={14} opacity={0.5}/>}
                            >
                                <Text truncate='end' inherit>
                                    {link.label}
                                </Text>
                            </Button>
                        );
                    })}

                    <Collapse in={expanded}>
                        <Stack gap={8}>
                            {hiddenLinks.map((link, index) => {
                                return (
                                    <Button
                                        key={index + INITIAL_COUNT}
                                        component="a"
                                        href={link.cleanUrl}
                                        target="_blank"
                                        variant="light"
                                        color={link.color}
                                        leftSection={link.icon}
                                        rightSection={<IconExternalLink size={14} opacity={0.5} />}
                                    >
                                        <Text truncate='end' inherit>
                                            {link.label}
                                        </Text>
                                    </Button>
                                );
                            })}
                        </Stack>
                    </Collapse>

                    {/* Toggle Button */}
                    {hasMore && (
                        <Button
                            variant="subtle"
                            color="gray"
                            size="xs"
                            onClick={() => setExpanded((v) => !v)}
                            leftSection={expanded ? <IconChevronUp size={14} /> : <IconChevronDown size={14} />}
                        >
                            {expanded ? t('app.season_team.detail.links.show_less') : t('app.season_team.detail.links.show_more', {count: hiddenLinks.length})}
                        </Button>
                    )}
                </Stack>
            </Group>

            {/* The Season Journey */}
            <Title order={3} mb="md">{t("app.season_team.detail.season_journey")}</Title>
            <Stack gap="md">
                {sortedRecords.length === 0 ? (
                    <Text c="dimmed">{t("app.season_team.detail.no_competitions")}</Text>
                ) : (
                    <>
                        <Timeline bulletSize={40} lineWidth={2} visibleFrom="xs">
                            {sortedRecords.map(record=> (
                                <Timeline.Item
                                    key={record.competition.id}
                                    title={
                                        <CompetitionTypeBadge type={record.competition.type} mb="xs"/>
                                    }
                                    bullet={
                                        <CompetitionTypeIcon size={40} type={record.competition.type} />
                                    }
                                >
                                    <CompetitionRecordCard key={record.competition.id} record={record} />
                                </Timeline.Item>
                            ))}
                        </Timeline>
                        <Box hiddenFrom="xs">
                            {sortedRecords.map(record=> (
                                <Box key={`box-${record.competition.id}`}>
                                    <CompetitionTypeBadge key={`badge-${record.competition.id}`} type={record.competition.type} mb="xs"/>
                                    <CompetitionRecordCard key={`card-${record.competition.id}`} record={record} mb={"lg"} />
                                </Box>
                            ))}
                        </Box>
                    </>

                )}
            </Stack>
            
            <TeamRobotgameStats records={sortedRecords}/>
        </Box>
    )
}