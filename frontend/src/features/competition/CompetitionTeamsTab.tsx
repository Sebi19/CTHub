import {
    SimpleGrid,
    Card,
    Text,
    Badge,
    Group,
    Stack,
    Tooltip,
    ActionIcon,
    Menu,
    Center,
    SegmentedControl, Table, Box, TooltipGroup, Anchor
} from '@mantine/core';
import { type CompetitionDetailDto, type SeasonTeamDto } from '../../api/generated';
import {
    IconMapPin,
    IconExternalLink,
    IconBuildingBank,
    IconInfoCircle,
    IconLayoutGrid,
    IconList
} from '@tabler/icons-react';
import {useTranslation} from "react-i18next";
import {Link, useNavigate} from "react-router-dom";
import {getTeamLink} from "../../utils/routingUtils.ts";
import {useSessionStorage} from "@mantine/hooks";
import {parseTeamLink} from "../../utils/linkUtils.tsx";
import {SeasonTeamAvatar} from "../common/team/avatar/SeasonTeamAvatar.tsx";

interface Props {
    competition: CompetitionDetailDto;
}

export const CompetitionTeamsTab = ({ competition }: Props) => {
    const { t } = useTranslation();

    const navigate = useNavigate();

    const [viewMode, setViewMode] = useSessionStorage<string | undefined>({
        key: `competition-teams-view-${competition.season.id}-${competition.urlPart}`,
        defaultValue: 'grid',
    });

    // Safety check just in case
    const teams = competition.registeredTeams;

    const teamCount = teams.length;

    if (teamCount === 0) {
        return <Text c="dimmed" ta="center" py="xl">{t("app.competition.teams.empty")}</Text>;
    }

    const TeamExtraLinksMenu = ({ team }: { team: SeasonTeamDto }) => {
        if (team.links.length === 0) return null;

        return (
            <Box onClick={(e) => e.stopPropagation()}>
                <Menu position="bottom-end" shadow="md" width={300} withArrow>
                    <Menu.Target>
                        <Tooltip label={t("app.competition.teams.tooltip.links")}>
                            <ActionIcon variant="transparent" size="sm">
                                <IconInfoCircle size={18} />
                            </ActionIcon>
                        </Tooltip>
                    </Menu.Target>

                    <Menu.Dropdown>
                        <Menu.Label>Team Links</Menu.Label>
                        {team.links.map((link, index) => {
                            const parsed = parseTeamLink(link.url, link.label);

                            return (
                                <Menu.Item
                                    key={index}
                                    component="a"
                                    href={parsed.cleanUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    color={parsed.color !== "gray" ? parsed.color : undefined}
                                    leftSection={parsed.icon}
                                    rightSection={<IconExternalLink size={14} opacity={0.4} />}
                                >
                                    <Text size="sm" style={{overflowWrap: 'anywhere'}}>
                                        {parsed.label}
                                    </Text>
                                </Menu.Item>
                            )
                        })}
                    </Menu.Dropdown>
                </Menu>
            </Box>
        );
    };

    return (
        <Stack gap="md">
            <Group justify="space-between" align="center">
                <TooltipGroup openDelay={500} closeDelay={100}>
                    <SegmentedControl
                        visibleFrom="sm"
                        value={viewMode}
                        onChange={(val) => setViewMode(val as 'grid' | 'table')}
                        styles={{
                            label: { padding: 0 }, // Remove the dead zone padding
                        }}
                        data={[
                            {
                                value: 'grid',
                                label: (
                                    <Tooltip label={t("app.competition.teams.tooltip.grid")}>
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconLayoutGrid size={18}/></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            },
                            {
                                value: 'table',
                                label: (
                                    <Tooltip label={t("app.competition.teams.tooltip.table")}>
                                        <Box px="xs" py="calc(var(--mantine-spacing-xs) / 2)" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                                            <Center><IconList size={18}/></Center>
                                        </Box>
                                    </Tooltip>
                                )
                            },
                        ]}
                    />
                </TooltipGroup>
            </Group>

            {/* Responsive grid: 1 col on mobile, 2 on tablet, 3 on desktop */}
            <Box hiddenFrom={viewMode === 'table' ? 'sm' : undefined}>
                <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
                    {teams.map((team: SeasonTeamDto) => (
                        <TooltipGroup openDelay={500} closeDelay={100} key={team.id}>
                            <Card
                                key={team.id}
                                shadow="sm"
                                padding="lg"
                                radius="md"
                                withBorder
                                onClick={() => navigate(getTeamLink(team))}
                                style={{
                                    cursor: 'pointer',
                                    transition: 'transform 0.2s ease, box-shadow 0.2s ease'
                                }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.transform = 'translateY(-2px)';
                                    e.currentTarget.style.boxShadow = 'var(--mantine-shadow-md)';
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = 'var(--mantine-shadow-sm)';
                                }}
                            >
                                <Group justify="space-between" mb="xs" align="flex-start" wrap={"nowrap"}>
                                    <Group gap="xs">
                                        <SeasonTeamAvatar team={team} size={"md"}/>
                                        <Stack gap={0}>
                                            <Group gap={"xs"}>
                                                <Anchor
                                                    component={Link}
                                                    to={getTeamLink(team)}
                                                    c="inherit"
                                                    underline="hover"
                                                    // Prevent the Anchor click from bubbling up and triggering the Card's onClick twice
                                                    onClick={(e) => e.stopPropagation()}
                                                >
                                                    <Text fw={600} truncate>{team.name}</Text>
                                                </Anchor>
                                                <TeamExtraLinksMenu team={team} />
                                            </Group>
                                            <Text size="xs" c="dimmed">#{team.fllId}</Text>
                                        </Stack>
                                    </Group>
                                    {competition.type === 'FINAL' && team.country && (
                                        <Tooltip label={t("app.competition.teams.country", {context: team.country})}>
                                            <Badge color='gray' variant='light'>
                                                {team.country}
                                            </Badge>
                                        </Tooltip>
                                    )}
                                </Group>

                                {/* Institution (School/Club) */}
                                {team.institution && (
                                    <Tooltip label={t("app.competition.teams.tooltip.institution")}>
                                        <Group gap="xs" c="dimmed" mt="xs" style={{ width: 'fit-content' }}>
                                            <IconBuildingBank size={16} />
                                            <Text size="sm">{team.institution}</Text>
                                        </Group>
                                    </Tooltip>
                                )}

                                {/* City */}
                                <Tooltip label={t("app.competition.teams.tooltip.city")}>
                                    <Group gap="xs" c="dimmed" mt="sm" style={{ width: 'fit-content' }}>
                                        <IconMapPin size={16}/>
                                        <Text size="sm">{team.city || 'Unknown City'}</Text>
                                    </Group>
                                </Tooltip>
                            </Card>
                        </TooltipGroup>
                    ))}
                </SimpleGrid>
            </Box>

            {viewMode === 'table' && (
                <Box visibleFrom="sm">
                    <Table striped highlightOnHover verticalSpacing="sm">
                        <Table.Thead>
                            <Table.Tr>
                                <Table.Th>{t("app.competition.teams.table.id")}</Table.Th>
                                {competition.type === 'FINAL' && (
                                    <Table.Th>{t("app.competition.teams.table.country")}</Table.Th>
                                )}
                                <Table.Th>{t("app.competition.teams.table.name")}</Table.Th>
                                <Table.Th>{t("app.competition.teams.table.institution")}</Table.Th>
                                <Table.Th>{t("app.competition.teams.table.city")}</Table.Th>
                                <Table.Th ta="right">{t("app.competition.teams.table.links")}</Table.Th>
                            </Table.Tr>
                        </Table.Thead>
                        <Table.Tbody>
                            {teams.map((team) => (
                                <TooltipGroup openDelay={500} closeDelay={100} key={team.id}>
                                    <Table.Tr
                                        key={team.id}
                                        onClick={() => navigate(getTeamLink(team))}
                                        style={{ cursor: 'pointer' }}
                                    >
                                        <Table.Td><Text fw={500} c="dimmed">{team.fllId}</Text></Table.Td>
                                        {competition.type === 'FINAL' && team.country && (
                                            <Table.Td>
                                                <Tooltip label={t("app.competition.teams.country", {context: team.country})}>
                                                    <Badge color='gray' variant='light'>
                                                        {team.country}
                                                    </Badge>
                                                </Tooltip>
                                            </Table.Td>
                                        )}
                                        <Table.Td>
                                            <Group gap="xs">
                                                <SeasonTeamAvatar team={team} size={32} />
                                                <Anchor
                                                    component={Link}
                                                    to={getTeamLink(team)}
                                                    c="inherit" // Inherit text color so it doesn't look like a standard blue link
                                                    underline="hover" // Only underline when they hover exactly over the text
                                                    fw={600}
                                                >
                                                    {team.name}
                                                </Anchor>
                                            </Group>
                                        </Table.Td>
                                        <Table.Td>{team.institution}</Table.Td>
                                        <Table.Td>{team.city}</Table.Td>
                                        <Table.Td>
                                            <Group gap="xs" mr="3" justify="flex-end" wrap="nowrap">
                                                <TeamExtraLinksMenu team={team} />
                                            </Group>
                                        </Table.Td>
                                    </Table.Tr>
                                </TooltipGroup>
                            ))}
                        </Table.Tbody>
                    </Table>
                </Box>
            )}
        </Stack>
    );
};