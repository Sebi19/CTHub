import {Title, Box, Select, Group} from '@mantine/core';
import { useDocumentTitle } from "@mantine/hooks";
import {useTranslation} from "react-i18next";
import {RobotGameLeaderboard} from "./RobotGameLeaderboard.tsx";
import {useAppContext} from "../../hooks/AppContext.tsx";
import {useNavigate, useParams} from "react-router-dom";
import {useEffect} from "react";

export const RobotGameLeaderboardPage = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { seasonId } = useParams<{ seasonId: string }>();

    const {
        activeSeason,
        setActiveSeason,
        globalDefaultSeason,
        availableSeasonIds,
        seasonsLoaded
    } = useAppContext();

    useEffect(() => {
        // If there is a seasonId in the URL, tell the global context about it
        if (seasonId) {
            setActiveSeason(seasonId);
        }

        // Cleanup: When the user leaves the Season Overview pages entirely,
        // revert the global context back to the default (e.g. for the home page)
        return () => {
            setActiveSeason(globalDefaultSeason);
        };
    }, [seasonId, setActiveSeason, globalDefaultSeason]);

    useEffect(() => {
        if (seasonsLoaded && !seasonId && activeSeason) {
            navigate(`/leaderboard/${activeSeason}`, { replace: true });
        }
    }, [seasonId, activeSeason, seasonsLoaded, navigate]);

    const targetSeasonId = seasonId || activeSeason;

    useDocumentTitle(t('app.overall_robotgame.doc_title'))


    return (
        // 1. CALCULATED HEIGHT: Viewport (100dvh) minus Header (60px)
        //    This creates the "App Mode" feel just for this route.
        <Box
            h="100%"
            p="md"
            style={{ display: 'flex', flexDirection: 'column' }}
        >

            <Group justify="space-between" align="flex-end" mb="md">
                {/* Title Area */}
                <Box p="md" pb="xs">
                    <Title order={2}>{t("app.overall_robotgame.title")}</Title>
                </Box>

                {/* Dropdown to change the active season via URL routing */}
                <Select
                    label={t('app.competition.overview.selectSeason') || "Season"}
                    value={targetSeasonId}
                    onChange={(nextSeason) => {
                        if (nextSeason) {
                            navigate(`/leaderboard/${nextSeason}`); // Adjust this path match to your router setup
                        }
                    }}
                    data={availableSeasonIds.map(id => ({ value: id, label: id }))}
                    w={150}
                />
            </Group>


            {/* Table Wrapper: Fills the remaining space in our calculated box */}
            <Box style={{ flex: 1, minHeight: 0 }}>
                <RobotGameLeaderboard seasonId={targetSeasonId}/>
            </Box>
        </Box>
    );
};