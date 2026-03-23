import {Badge, Box, Divider, Group, Text, Tooltip} from "@mantine/core";
import {IconCheck, IconHash, IconMedal, IconRobot, IconTrophy, IconX} from "@tabler/icons-react";
import {useTranslation} from "react-i18next";
import type {CompetitionRobotGameEntryDto} from "../../../api/generated.ts";

// TODO: Refactor

export const TeamRobotgameOverview = ({robotGameEntry}: {robotGameEntry: CompetitionRobotGameEntryDto}) => {
    const {t} = useTranslation();

    const prelims = [
        { key: 'pr1', score: robotGameEntry.pr1 },
        { key: 'pr2', score: robotGameEntry.pr2 },
        { key: 'pr3', score: robotGameEntry.pr3 },
    ] as const;

    const PrelimBadge = ({roundKey, score, bestScore} : {roundKey: string, score: number, bestScore: number}) => {
        const color = score === bestScore ? 'blue' : 'gray';
        return (
            <Tooltip label={t(`app.season_team.detail.robot_game.points_tooltip.${roundKey}` as any)}>
                <Badge variant="dot" color={color}>
                    {t('app.season_team.detail.robot_game.points', {
                        round: t(`app.season_team.detail.robot_game.${roundKey}` as any), // Type cast might be needed depending on your strict setup
                        points: score,
                        context: score === 0 ? 'empty' : undefined
                    })}
                </Badge>
            </Tooltip>
        )
    }

    const PrelimRank = ({rank}: {rank?: number}) => {
        if (!rank) return null;

        return (
            <Box>
                <Text size="xs" c="dimmed">{t('app.season_team.detail.robot_game.prelim_rank')}</Text>
                <Group gap={4}>
                    <IconHash size={18} color="var(--mantine-color-blue-6)" />
                    <Text fw={700} size="lg">{rank || '-'}</Text>
                </Group>
            </Box>
        );
    }

    const PlayoffRank = ({rank}: {rank: number}) => {
        const winnerIcon = <IconTrophy size={18} color="var(--mantine-color-blue-6)" />
        const medalIcon = <IconMedal size={18} color="var(--mantine-color-blue-6)" />
        const otherIcon = <IconHash size={18} color="var(--mantine-color-blue-6)" />

        const icon = rank === 1 ? winnerIcon : (rank <= 3 ? medalIcon : otherIcon);

        return (
            <Box>
                <Text size="xs" c="dimmed">{t('app.season_team.detail.robot_game.playoff_rank')}</Text>
                <Group gap={4}>
                    {icon}
                    <Text fw={700} size="lg">{rank || '-'}</Text>
                </Group>
            </Box>
        );
    }

    interface PlayoffRun {
        key: string;
        score?: number;
        additionalScore?: number;
        advanced: boolean;
    }

    const playoffs: PlayoffRun[] = [
        { key: 'r16', score: robotGameEntry.r16, advanced: robotGameEntry.qf !== null },
        { key: 'qf', score: robotGameEntry.qf, advanced: robotGameEntry.sf !== null },
        { key: 'sf', score: robotGameEntry.sf, advanced: robotGameEntry.f1 !== null },
        { key: 'f', score: robotGameEntry.f1, additionalScore: robotGameEntry.f2, advanced: robotGameEntry.rank === 1 },
    ].filter(run => run.score !== null);


    const PlayoffBadge = ({roundKey, score, additionalScore, advanced}: {roundKey: string, score?: number, additionalScore?: number, advanced: boolean}) => {
        return (
            <Tooltip label={t(`app.season_team.detail.robot_game.points_tooltip.${roundKey}` as any)}>
                <Badge variant={"light"} color={advanced ? 'green' : 'red'} rightSection={advanced ? <IconCheck size={14} /> : <IconX size={14} />}>
                    {t('app.season_team.detail.robot_game.playoff_points', {
                        round: t(`app.season_team.detail.robot_game.${roundKey}` as any), // Type cast might be needed depending on your strict setup
                        points: score,
                        additionalPoints: additionalScore,
                        context: roundKey === 'f' ? 'final' : (score === 0 ? 'empty' : undefined),
                    })}
                </Badge>
            </Tooltip>
        )
    };
    
    return (
        <Box>
            <Group gap="md" mb="xs">
                <Box>
                    <Text size="xs" c="dimmed">{t('app.season_team.detail.robot_game.best_pr')}</Text>
                    <Group gap={4}>
                        <IconRobot size={18} color="var(--mantine-color-blue-6)" />
                        <Text fw={700} size="lg">{robotGameEntry.bestPr}</Text>
                    </Group>
                </Box>
                <PrelimRank rank={robotGameEntry.prelimRank}/>
            </Group>

            {/* Mini table for prelim runs */}
            <Group gap="xs" mb="xs">
                {prelims.map(({ key, score }) => (
                    <PrelimBadge key={key} roundKey={key} score={score} bestScore={robotGameEntry.bestPr}/>
                ))}
            </Group>

            <Divider my="sm" variant="dashed" />

            {playoffs.length > 0 && (
                <>
                    <Group gap="md" mb="xs">
                        <PlayoffRank rank={robotGameEntry.rank} />
                    </Group>

                    <Group gap="xs">
                        {playoffs.map(({key, score, additionalScore, advanced}) => {
                            return <PlayoffBadge key={key} roundKey={key} advanced={advanced} score={score} additionalScore={additionalScore}/>
                        })}
                    </Group>
                </>
            )}

            {playoffs.length === 0 && (
                <Text size="sm" c="dimmed" fs="italic">
                    {t('app.season_team.detail.robot_game.no_playoffs')}
                </Text>
            )}

        </Box>
    )
}