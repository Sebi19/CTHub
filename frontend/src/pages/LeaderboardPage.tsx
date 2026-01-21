// pages/LeaderboardPage.tsx
import { Title, Box } from '@mantine/core';
import { RobotGameLeaderboard } from '../components/RobotGameLeaderboard';

export const LeaderboardPage = () => {
    return (
        // 1. CALCULATED HEIGHT: Viewport (100dvh) minus Header (60px)
        //    This creates the "App Mode" feel just for this route.
        <Box
            h="100%"
            p="md"
            style={{ display: 'flex', flexDirection: 'column' }}
        >

            {/* Title Area */}
            <Box p="md" pb="xs">
                <Title order={2}>🏆 Global Robot Game Leaderboard</Title>
            </Box>

            {/* Table Wrapper: Fills the remaining space in our calculated box */}
            <Box style={{ flex: 1, minHeight: 0 }}>
                <RobotGameLeaderboard />
            </Box>
        </Box>
    );
};