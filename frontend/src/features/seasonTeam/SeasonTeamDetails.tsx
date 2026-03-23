import {Badge, Box, Button, Group, Stack, Text, Timeline, Title} from "@mantine/core";
import {IconBuildingBank, IconExternalLink, IconMapPin} from "@tabler/icons-react";
import {parseTeamLink} from "../../utils/linkUtils.tsx";
import {CompetitionRecordCard} from "./CompetitionRecordCard.tsx";
import type {SeasonTeamDetailsDto} from "../../api/generated.ts";
import dayjs from "dayjs";
import {SeasonBadge} from "../common/season/SeasonBadge.tsx";
import {useTranslation} from "react-i18next";
import {CompetitionTypeBadge} from "../common/competition/CompetitionTypeBadge.tsx";
import {CompetitionTypeIcon} from "../common/competition/CompetitionTypeIcon.tsx";
import {SeasonTeamAvatar} from "../common/team/avatar/SeasonTeamAvatar.tsx";

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

    return (
        <>
            <Group justify="space-between" align="flex-start" mb="xl">
                <Box flex={{base: '1 0 100%', xs: '1 1 min-content'}} miw={0}>
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
                    <Group>
                        <SeasonTeamAvatar team={teamDetails} size={120} hideNoProfile/>
                        <Stack gap={0}>
                            <Title order={1} mb="xs">{teamDetails.name}</Title>
                            <Group gap="xs" c="dimmed">
                                {teamDetails.institution && (
                                    <>
                                        <IconBuildingBank size={16} />
                                        <Text>{teamDetails.institution}</Text>
                                    </>
                                )}
                                {teamDetails.city && (
                                    <>
                                        <IconMapPin size={16} />
                                        <Text>{teamDetails.city}</Text>
                                    </>
                                )}
                            </Group>
                        </Stack>
                    </Group>

                </Box>

                <Stack
                    gap={8}
                    pos="sticky"
                    top={80}
                    flex={{base: '1', xs: 'initial'}}
                    style={{zIndex: 10}}
                    mb="sm"
                >
                    {teamDetails.links.map((link, index) => {
                        const parsed = parseTeamLink(link.url, link.label);

                        return (
                            <Button
                                key={index}
                                component="a"
                                href={parsed.cleanUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                variant="light"
                                color={parsed.color}
                                leftSection={parsed.icon}
                                rightSection={<IconExternalLink size={14} opacity={0.5}/>}
                            >
                                {parsed.label}
                            </Button>
                        );
                    })}
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
        </>
    )
}