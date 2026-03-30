    import type {TeamCompetitionRecordDto} from "../../../api/generated.ts";
    import {useTranslation} from "react-i18next";
    import {Box, Group, Paper, SimpleGrid, ThemeIcon, Title, Text, Tooltip, ScrollArea} from "@mantine/core";
    import {IconCalendar, IconMathAvg, IconRobot, IconTrophy} from "@tabler/icons-react";
    import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip as RechartsTooltip, ReferenceLine } from 'recharts';
    import {getCompetitionTypeColor, getFormattedCompetitionDate} from "../../../utils/competitionUtils.ts";
    import {useId} from "react";
    import {useElementSize} from "@mantine/hooks";
    import {useCarouselScrollShield} from "../../../hooks/useCarouselScrollShield.ts";
    interface TeamRobotGameStatsProps {
        records: TeamCompetitionRecordDto[];
    }

    const CustomDot = (props: any) => {
        const { cx, cy, payload, value, active } = props;

        // Hide the dot on the vertical dividers
        if (value === null || value === undefined) return null;

        // Conditionally swap colors
        const fillColor = payload.subtle ? 'var(--mantine-color-gray-4)' : payload.isPlayoff ? 'var(--mantine-color-violet-5)' : 'var(--mantine-color-blue-6)';

        const radius = active ? 8 : 6;
        const strokeColor = active ? 'white' : 'var(--mantine-color-body)';
        const strokeWidth = active ? 3 : 2;

        return (
            <circle
                cx={cx}
                cy={cy}
                r={radius}
                fill={fillColor}
                stroke={strokeColor}
                strokeWidth={strokeWidth}/>
        );
    };

    const CustomLabel = (props: any) => {
        const { x, y, value, index, chartData, hideLabels } = props;

        const {t} = useTranslation();

        const dataPoint = chartData[index];

        const roundName = dataPoint?.roundName;

        if (hideLabels || value === null || value === undefined) return null;

        return (
            <>
                <text x={x} y={y - 28} fill="var(--mantine-color-dimmed)" fontSize={10} textAnchor="middle">
                    {t('app.common.robot_game.round_short', { context: roundName.toLowerCase()})}
                </text>
                <text x={x} y={y - 12} fill="var(--mantine-color-text)" fontSize={14} fontWeight={700} textAnchor="middle">
                    {Math.round(value)}
                </text>
            </>
        );
    };

    function ChartTooltip({payload, label, chartData}: any) {
        const data = payload?.[0]?.payload || chartData.find((d: any) => d.runLabel === label);

        const {t} = useTranslation();

        if (!data) return null;

        // 2. If it's our divider point, show a special, clean tooltip!
        if (data.isDivider) {
            return (
                <Paper px="md" py="sm" withBorder shadow="md" radius="md">
                    <Group gap={2}>
                        <IconCalendar size={14} style={{ color: 'var(--mantine-color-dimmed)' }} />
                        <Text  size="xs" c="dimmed">{getFormattedCompetitionDate(data.competition)}</Text>
                    </Group>
                    <Text fw={700} size="sm">{data.competitionName}</Text>
                </Paper>
            );
        }

        return (
            <Paper px="md" py="sm" withBorder shadow="md" radius="md">
                <Group gap={2}>
                    <IconCalendar size={14} style={{ color: 'var(--mantine-color-dimmed)' }} />
                    <Text  size="xs" c="dimmed">{getFormattedCompetitionDate(data.competition)}</Text>
                </Group>
                <Text fw={700} size="sm">{data.competitionName}</Text>
                <Text fw={500} size="xs" c="dimmed" mb={8}>{t("app.common.robot_game.round_long", {context: data.roundName.toLowerCase()})}</Text>

                <Group gap="xs">
                    {data.isPlayoff ? <IconTrophy size={16} color="var(--mantine-color-violet-5)"/> : <IconRobot size={16} color="var(--mantine-color-blue-6)"/>}
                    <Text c={data.isPlayoff ? 'violet.5' : 'blue.6'} fw={700}>
                        {Math.round(data.score)}
                    </Text>
                </Group>
            </Paper>
        );
    }

    interface LineLimit {
        color: string;
        stop: number;
        key: string;
    }

    export const TeamRobotgameStats = ({ records }: TeamRobotGameStatsProps) => {
        const { t } = useTranslation();

        const rawId = useId();
        const gradientId = `colorSplit-${rawId.replace(/:/g, '')}`;

        const { ref, width } = useElementSize();

        const { ref: scrollRef, width: scrollWidth } = useElementSize();

        let highestScore = 0;
        let lowestScore = Infinity;
        let totalScore = 0;
        let runCount = 0;
        const chartData: any[] = [];
        const verticalDividers: any[] = []; // NEW: Array to hold our competition boundaries
        let previousCompName = '';

        let hasPlayoffRun = false;

        const competitionCount = records.length;
        const compCounts: number[] = [];

        const seasonPerfectScore = records.length > 0 ? records[0].competition.season.maxPoints ?? 0 : 0;

        records.forEach((record, index) => {
            const rg = record.robotGame;
            if (!rg) return;
            compCounts.push(0);

            const shortCompName = record.competition.name.split(' ').slice(0, 2).join(' ');

            const addRun = (key: keyof typeof rg, label: string) => {
                const score = rg[key] as number | null;

                if (score !== null && score !== undefined) {
                    highestScore = Math.max(highestScore, score);
                    lowestScore = Math.min(lowestScore, score);
                    totalScore += score;
                    runCount++;
                    compCounts[index]++;
                    let isPlayoff = ['r16', 'qf', 'sf', 'f1', 'f2'].includes(key);
                    if (isPlayoff) {
                        hasPlayoffRun = true;
                    }

                    const runLabel = `${label} - ${shortCompName}`;

                    // NEW: If this is the first run we are adding for a NEW competition,
                    // log it as a vertical divider!
                    if (previousCompName !== shortCompName) {
                        chartData.push({
                            runLabel: shortCompName, // Empty label for the divider entry
                            score: null, // No score for the divider entry
                            isDivider: true, // Custom flag to identify dividers in the data
                            competitionName: record.competition.name,
                            competition: record.competition
                        })
                        verticalDividers.push({
                            x: shortCompName, // Setting 'x' makes it a vertical line instead of horizontal!
                            color: 'gray.4',
                            label: record.competition.name,
                            labelPosition: 'insideTopLeft',
                            compIndex: index,
                        });
                        previousCompName = shortCompName; // Update tracker so we only draw one line per comp
                    } else if (previousCompName === '') {
                        // Set the initial tracker on the very first run
                        previousCompName = shortCompName;
                    }

                    const renderScore = runCount === 1 ? score + 0.001 : score;


                    chartData.push({
                        runLabel: runLabel,
                        score: renderScore, // Just one unified score property now!
                        isDivider: false,
                        isPlayoff: isPlayoff,
                        subtle: !isPlayoff && score < rg.bestPr,
                        roundName: label,
                        competitionName: record.competition.name,
                        competition: record.competition,
                    });
                }
            };

            // Chronological order
            addRun('pr1', 'PR1');
            addRun('pr2', 'PR2');
            addRun('pr3', 'PR3');
            addRun('r16', 'R16');
            addRun('qf', 'QF');
            addRun('sf', 'SF');
            addRun('f1', 'F1');
            addRun('f2', 'F2');
        });

        if (runCount === 0) return null;

        const averageScore = Math.round(totalScore / runCount);

        const minTick = Math.max(0, Math.floor((lowestScore - 20) / 50) * 50);
        const maxShownScore = Math.min(seasonPerfectScore, Math.ceil((highestScore + 30) / 50) * 50);
        const maxTick = maxShownScore == seasonPerfectScore ? seasonPerfectScore + 30 : Math.ceil((highestScore + 20) / 50) * 50;

        const yTicks = [];
        for (let i = minTick; i < maxShownScore; i += 50) {
            yTicks.push(i);
        }
        yTicks.push(maxShownScore)

        const totalSections = runCount + competitionCount - 2;
        const sections = compCounts.reduce((acc, count) => {
            const last = acc[acc.length - 1];
            acc.push(last + count + 1);
            return acc;
        }, [-1]);

        sections[0] = 0;
        sections[sections.length - 1] = totalSections

        const lineLimits: LineLimit[] = [];

        records.forEach((record, index) => {
            const color = getCompetitionTypeColor(record.competition.type);
            const sectionStart = sections[index];
            const startPercentage = (sectionStart / totalSections) * 100;
            const sectionEnd = sections[index + 1];
            const endPercentage = (sectionEnd / totalSections) * 100;

            lineLimits.push({
                color: color,
                stop: startPercentage,
                key: `${record.competition.id}-start`
            });
            lineLimits.push({
                color: color,
                stop: endPercentage,
                key: `${record.competition.id}-end`
            });
        });

        const pixelsPerSection = width > 0 ? width / Math.max(1, totalSections) : Infinity;
        const hideLabels = false; //width > 0 && pixelsPerSection < 45;

        const MIN_PIXELS_PER_POINT = 45;
        const BUFFER_PIXELS_PER_POINT = 5;

        const preferredMinWidth = (chartData.length * MIN_PIXELS_PER_POINT) + 60;

        let dynamicMinWidth = preferredMinWidth;

        if (preferredMinWidth > scrollWidth) {
            const minimalMinWidth = (chartData.length * (MIN_PIXELS_PER_POINT - BUFFER_PIXELS_PER_POINT)) + 60;
            if (minimalMinWidth > scrollWidth) {
                dynamicMinWidth = preferredMinWidth;
            } else {
                dynamicMinWidth = scrollWidth;
            }
        }

        useCarouselScrollShield<HTMLDivElement>(scrollRef);


        return (
            <Box mt="xl">
                <Title order={3} mb="md">{t("app.season_team.detail.robot_game_stats.title")}</Title>
                {/* The "Hero" Metrics */}
                <SimpleGrid cols={{ base: 1, sm: 3 }} mb="xl">
                    <Paper withBorder p="md" radius="md">
                        <Group justify="space-between">
                            <Text size="xs" c="dimmed" fw={700} tt="uppercase">{t("app.season_team.detail.robot_game_stats.highscore")}</Text>
                            <ThemeIcon color="yellow" variant="light" size="lg"><IconTrophy size={20} /></ThemeIcon>
                        </Group>
                        <Text size="xl" fw={700} mt="sm">{highestScore}</Text>
                    </Paper>

                    <Paper withBorder p="md" radius="md">
                        <Group justify="space-between">
                            <Text size="xs" c="dimmed" fw={700} tt="uppercase">{t("app.season_team.detail.robot_game_stats.average_score")}</Text>
                            <ThemeIcon color="blue" variant="light" size="lg"><IconMathAvg size={20} /></ThemeIcon>
                        </Group>
                        <Text size="xl" fw={700} mt="sm">{averageScore}</Text>
                    </Paper>

                    <Paper withBorder p="md" radius="md">
                        <Group justify="space-between">
                            <Text size="xs" c="dimmed" fw={700} tt="uppercase">{t("app.season_team.detail.robot_game_stats.official_run_count")}</Text>
                            <ThemeIcon color="gray" variant="light" size="lg"><IconRobot size={20} /></ThemeIcon>
                        </Group>
                        <Text size="xl" fw={700} mt="sm">{runCount}</Text>
                    </Paper>
                </SimpleGrid>

                <ScrollArea type={"auto"} ref={scrollRef} style={{ width: '100%' }} mb="md">
                    <Paper withBorder p="md" radius="md" ref={ref} miw={dynamicMinWidth}>
                        <Box h={320}>
                            <ResponsiveContainer width="100%" height="100%">
                                <LineChart data={chartData} margin={{top: 30, right: 30, left: 0, bottom: 20}}>
                                    <defs>
                                        <linearGradient id={gradientId} x1="0" y1="0" x2="1" y2="0">
                                            {lineLimits.map((lineLimit) => {
                                                return (
                                                    <stop offset={`${lineLimit.stop}%`} stopColor={`var(--mantine-color-${lineLimit.color}-5)`} key={lineLimit.key}/>
                                                )
                                            })}
                                        </linearGradient>
                                    </defs>
                                    {/* The Axes */}
                                    <XAxis dataKey="runLabel" hide/>
                                    <YAxis
                                        domain={[minTick, maxTick]}
                                        ticks={yTicks} // Force it to use our beautiful [200, 250, 300...] array
                                        axisLine={false}
                                        tickLine={false}
                                        tick={{fill: 'var(--mantine-color-dimmed)', fontSize: 12}}
                                        width={40}
                                    />

                                    {/* Your Custom Tooltip */}
                                    <RechartsTooltip filterNull={false} content={(props) => <ChartTooltip {...props} chartData={chartData}/>}
                                             cursor={{stroke: 'var(--mantine-color-dark-3)', strokeWidth: 2}}/>

                                    <ReferenceLine
                                        y={seasonPerfectScore}
                                        stroke="var(--mantine-color-yellow-5)"
                                        strokeDasharray="4 4"
                                        label={(props: any) => (
                                            <text
                                                y={props.viewBox.y - 18} // Initial Y position
                                                fill="var(--mantine-color-yellow-5)"
                                                fontSize={9} // Slightly smaller for better fit
                                                fontWeight={800}
                                                style={{ pointerEvents: 'none' }} // Ensure it doesn't block dot hovers
                                            >
                                                <tspan x={props.viewBox.x + 5} dy="0">{t("app.season_team.detail.robot_game_stats.perfect_score.line_one")}</tspan>
                                                <tspan x={props.viewBox.x + 5} dy="11">{t("app.season_team.detail.robot_game_stats.perfect_score.line_two")}</tspan>
                                            </text>
                                        )}
                                    />

                                    {/* Vertical Dividers */}
                                    {verticalDividers.map((div, i) => {
                                        const span = compCounts[div.compIndex] + 1; // +1 for the divider itself
                                        const availablePixels = (span * pixelsPerSection) - 40;
                                        const maxChars = Math.floor(availablePixels / 8);

                                        let displayLabel = div.label;

                                        if (maxChars < 5) {
                                            displayLabel = '';
                                        } else if (displayLabel.length > maxChars) {
                                            // Truncate and add ellipsis, taking the 3 dots into account
                                            displayLabel = `${displayLabel.substring(0, maxChars - 3)}...`;
                                        }

                                        return (
                                            <ReferenceLine
                                                key={i}
                                                x={div.x}
                                                stroke="var(--mantine-color-gray-5)"
                                                label={(props) => (
                                                    <Tooltip label={div.label}>
                                                        <text
                                                            x={props.viewBox.x + 8}
                                                            y={props.viewBox.y - 16}
                                                            fill="var(--mantine-color-dimmed)"
                                                            fontSize={14}
                                                        >
                                                            {displayLabel}
                                                        </text>
                                                    </Tooltip>
                                                )}
                                            />
                                        );
                                    })}

                                    {/* The Actual Line */}
                                    <Line
                                        type="linear"
                                        dataKey="score"
                                        stroke={`url(#${gradientId})`}
                                        strokeWidth={3}
                                        connectNulls // Keeps your curve smooth over the vertical dividers!

                                        // 1. YOUR CUSTOM DOT
                                        dot={<CustomDot/>}
                                        activeDot={<CustomDot active />}

                                        // 2. YOUR CUSTOM LABELS
                                        label={<CustomLabel chartData={chartData} hideLabels={hideLabels}/>}
                                    />
                                </LineChart>
                            </ResponsiveContainer>
                        </Box>
                        <Group justify="center" gap="xl" mt="md" pb="xs">
                            <Group gap={6}>
                                <Box w={10} h={10} style={{ borderRadius: '50%' }} bg="blue.6" />
                                <Text size="xs" c="dimmed" fw={500}>{t("app.season_team.detail.robot_game_stats.legend.best_pr")}</Text>
                            </Group>

                            <Group gap={6}>
                                <Box w={10} h={10} style={{ borderRadius: '50%' }} bg="gray.4" />
                                <Text size="xs" c="dimmed" fw={500}>{t("app.season_team.detail.robot_game_stats.legend.other_pr")}</Text>
                            </Group>

                            {hasPlayoffRun && (
                                <Group gap={6}>
                                    <Box w={10} h={10} style={{ borderRadius: '50%' }} bg="violet.5" />
                                    <Text size="xs" c="dimmed" fw={500}>{t("app.season_team.detail.robot_game_stats.legend.playoffs")}</Text>
                                </Group>
                            )}
                        </Group>
                    </Paper>
                </ScrollArea>
            </Box>
        );
    }