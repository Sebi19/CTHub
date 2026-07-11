import { Table, Anchor, Group, Text, Progress } from '@mantine/core';
import { IconCalendar } from '@tabler/icons-react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { getCompetitionLink } from '../../../utils/routingUtils.ts';
import { getCompetitionTypeColor, getFormattedCompetitionDate } from '../../../utils/competitionUtils.ts';
import type {CompetitionShortInfoDto} from "../../../api/generated.ts";
import {CompetitionTypeBadge} from "../../common/competition/CompetitionTypeBadge.tsx";
import {CountryBadge} from "../../common/CountryBadge.tsx";

interface CompetitionTableProps {
    competitions: CompetitionShortInfoDto[];
}

export const CompetitionsTable = ({ competitions }: CompetitionTableProps) => {
    const { t } = useTranslation();
    const navigate = useNavigate();

    return (
        <Table striped highlightOnHover verticalSpacing="sm">
            <Table.Thead>
                <Table.Tr>
                    <Table.Th>{t("app.competition.previous.header.name")}</Table.Th>
                    <Table.Th>{t("app.competition.previous.header.date")}</Table.Th>
                    <Table.Th>{t("app.competition.previous.header.type")}</Table.Th>
                    <Table.Th>{t("app.competition.previous.header.country")}</Table.Th>
                    <Table.Th style={{ width: '180px' }}>{t("app.competition.overview.capacity")}</Table.Th>
                </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
                {competitions.map((comp) => {
                    const hasCapacity = comp.maxTeamCount !== undefined && comp.maxTeamCount > 0;
                    const registered = comp.registeredTeamCount || 0;
                    const max = comp.maxTeamCount || 0;
                    const capacityPercentage = hasCapacity ? (registered / max) * 100 : 0;
                    const themeColor = getCompetitionTypeColor(comp.type);

                    return (
                        <Table.Tr
                            key={comp.id}
                            onClick={() => navigate(getCompetitionLink(comp))}
                            style={{ cursor: 'pointer' }}
                        >
                            <Table.Td>
                                <Anchor
                                    component={Link}
                                    to={getCompetitionLink(comp)}
                                    c="inherit"
                                    underline="hover"
                                    fw={600}
                                >
                                    {comp.name}
                                </Anchor>
                            </Table.Td>
                            <Table.Td>
                                <Group gap="xs" c="dimmed" wrap="nowrap">
                                    <IconCalendar size={16} />
                                    <Text size="sm">{getFormattedCompetitionDate(comp)}</Text>
                                </Group>
                            </Table.Td>
                            <Table.Td>
                                <CompetitionTypeBadge type={comp.type} />
                            </Table.Td>
                            <Table.Td>
                                <CountryBadge country={comp.country} />
                            </Table.Td>
                            <Table.Td onClick={(e) => e.stopPropagation()}>
                                {hasCapacity ? (
                                    <Group gap="xs" wrap="nowrap" style={{ width: '100%' }}>
                                        <Progress value={capacityPercentage} color={themeColor} size="xs" style={{ flexGrow: 1 }} />
                                        <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap',  minWidth: "45px" }} ta="right">
                                            {comp.registeredTeamCount}/{comp.maxTeamCount}
                                        </Text>
                                    </Group>
                                ) : (
                                    <Text c="dimmed" size="xs">-</Text>
                                )}
                            </Table.Td>
                        </Table.Tr>
                    );
                })}
            </Table.Tbody>
        </Table>
    );
};