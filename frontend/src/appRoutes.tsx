import {useAuth} from "./features/auth/AuthContext.tsx";
import {Navigate, Route, Routes} from "react-router-dom";
import App from "./App.tsx";
import {LoginPage} from "./features/auth/LoginPage.tsx";
import {RegisterPage} from "./features/auth/RegisterPage.tsx";
import {RegistrationPendingPage} from "./features/auth/RegistrationPendingPage.tsx";
import {VerifyEmailPage} from "./features/auth/VerifyEmailPage.tsx";
import {CompleteProfilePage} from "./features/auth/CompleteProfilePage.tsx";
import {ForgotPasswordPage} from "./features/auth/ForgotPasswordPage.tsx";
import {ResetPasswordPage} from "./features/auth/ResetPasswordPage.tsx";
import {LoginLinkPage} from "./features/auth/LoginLinkPage.tsx";
import {ProtectedRoute} from "./features/auth/ProtectedRoute.tsx";
import {ProfileCompletionRedirect} from "./features/auth/ProfileCompletionRedirect.tsx";
import {CompetitionDetailPage} from "./features/competition/detail/CompetitionDetailPage.tsx";
import {RobotGameLeaderboardPage} from "./features/robotGameLeaderboard/RobotGameLeaderboardPage.tsx";
import {SeasonTeamDetailPage} from "./features/seasonTeam/SeasonTeamDetailPage.tsx";
import {TeamProfileDetailPage} from "./features/teamProfile/TeamProfileDetailPage.tsx";
import {NotFoundPage} from "./features/error/NotFoundPage.tsx";
import {SeasonOverview} from "./features/competition/overview/SeasonOverview.tsx";

export function AppRoutes() {
    const {isAuthenticated} = useAuth();

    return (
        <>
            <ProfileCompletionRedirect/>
            <Routes>
            {/* PARENT ROUTE: App (The Shell)
                           All nested routes render inside App's <Outlet />
                        */}
            <Route path="/" element={<App/>}>

                {/* Index: Redirect to dach final */}
                <Route index element={<Navigate to="/competitions/2026-27" replace/>}/>

                <Route path="/login" element={!isAuthenticated ? <LoginPage/> : <Navigate to="/"/>}/>

                <Route path="/register" element={!isAuthenticated ? <RegisterPage/> : <Navigate to="/"/>}/>

                <Route path="/registration-pending" element={<RegistrationPendingPage/>}/>

                <Route path="/verify-email" element={<VerifyEmailPage/>}/>

                <Route path="/login-link" element={<LoginLinkPage/>}/>

                <Route path="/forgot-password" element={<ForgotPasswordPage/>}/>

                <Route path="/reset-password" element={<ResetPasswordPage/>}/>

                <Route
                    path="/complete-profile"
                    element={<ProtectedRoute><CompleteProfilePage/></ProtectedRoute>}
                />

                {/* Public Pages */}
                <Route path="leaderboard" element={<RobotGameLeaderboardPage/>}/>

                <Route path="leaderboard/:seasonId" element={<RobotGameLeaderboardPage/>}/>

                <Route path="competitions" element={<SeasonOverview/>}/>

                <Route path="competitions/:seasonId" element={<SeasonOverview/>}/>

                <Route path="competition/:seasonId/:urlPart" element={<CompetitionDetailPage/>}/>

                <Route path="team/:seasonId/:fllId" element={<SeasonTeamDetailPage/>}/>

                <Route path="/:teamProfileUrl" element={<TeamProfileDetailPage/>}/>

                <Route path="/:teamProfileUrl/:seasonId" element={<TeamProfileDetailPage/>}/>

                <Route path="*" element={<NotFoundPage />} />

            </Route>
            </Routes>
        </>
    );
}