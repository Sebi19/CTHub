import {useAuth} from "./features/auth/AuthContext.tsx";
import {Navigate, Route, Routes} from "react-router-dom";
import App from "./App.tsx";
import {LoginPage} from "./features/auth/LoginPage.tsx";
import {CompetitionDetailPage} from "./features/competition/CompetitionDetailPage.tsx";
import {RobotGameLeaderboardPage} from "./features/robotGameLeaderboard/RobotGameLeaderboardPage.tsx";
import {SeasonTeamDetailPage} from "./features/seasonTeam/SeasonTeamDetailPage.tsx";
import {TeamProfileDetailPage} from "./features/teamProfile/TeamProfileDetailPage.tsx";
import {NotFoundPage} from "./features/error/NotFoundPage.tsx";

export function AppRoutes() {
    const {isAuthenticated} = useAuth();

    return (
        <Routes>
            {/* PARENT ROUTE: App (The Shell)
                           All nested routes render inside App's <Outlet />
                        */}
            <Route path="/" element={<App/>}>

                {/* Index: Redirect to rootbots */}
                <Route index element={<Navigate to="/rootbots/2025-26" replace/>}/>

                <Route path="/login" element={!isAuthenticated ? <LoginPage/> : <Navigate to="/"/>}/>

                {/* Public Pages */}
                <Route path="leaderboard" element={<RobotGameLeaderboardPage/>}/>

                <Route path="competition/:seasonId/:urlPart" element={<CompetitionDetailPage/>}/>

                <Route path="team/:seasonId/:fllId" element={<SeasonTeamDetailPage/>}/>

                <Route path="/:teamProfileUrl" element={<TeamProfileDetailPage/>}/>

                <Route path="/:teamProfileUrl/:seasonId" element={<TeamProfileDetailPage/>}/>

                <Route path="*" element={<NotFoundPage />} />

                {/* Example Protected Page (Add later) */}
                {/* <Route path="my-team" element={<ProtectedRoute><MyTeamPage /></ProtectedRoute>} /> */}

            </Route>
        </Routes>
    );
}