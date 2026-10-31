import {useAuth} from "./features/auth/AuthContext.tsx";
import {Navigate, Route, Routes} from "react-router-dom";
import App from "./App.tsx";
import {LoginPage} from "./features/auth/LoginPage.tsx";
import {CompleteProfilePage} from "./features/auth/CompleteProfilePage.tsx";
import {AuthLinkPage} from "./features/auth/AuthLinkPage.tsx";
import {PushApprovalPage} from "./features/auth/PushApprovalPage.tsx";
import {
    ACCOUNT_PAGE_PATH,
    AccountSecurityPage,
    CONFIRM_EMAIL_PATH
} from "./features/account/AccountSecurityPage.tsx";
import {ConfirmEmailPage} from "./features/account/ConfirmEmailPage.tsx";
import {ProtectedRoute} from "./features/auth/ProtectedRoute.tsx";
import {ProfileCompletionRedirect} from "./features/auth/ProfileCompletionRedirect.tsx";
import {CompetitionDetailPage} from "./features/competition/detail/CompetitionDetailPage.tsx";
import {RobotGameLeaderboardPage} from "./features/robotGameLeaderboard/RobotGameLeaderboardPage.tsx";
import {SeasonTeamDetailPage} from "./features/seasonTeam/SeasonTeamDetailPage.tsx";
import {TeamProfileDetailPage} from "./features/teamProfile/TeamProfileDetailPage.tsx";
import {NotFoundPage} from "./features/error/NotFoundPage.tsx";
import {SeasonOverview} from "./features/competition/overview/SeasonOverview.tsx";
import {NotificationsPage} from "./features/notifications/NotificationsPage.tsx";
import {NotificationSettingsPage} from "./features/notifications/NotificationSettingsPage.tsx";
import {NOTIFICATION_SETTINGS_PATH, NOTIFICATIONS_PAGE_PATH} from "./features/notifications/NotificationsContext.tsx";

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

                {/* Sign-up, e-mail verification and password reset all go through the login page now. */}
                {['/register', '/registration-pending', '/verify-email', '/login-link', '/forgot-password',
                    '/reset-password'].map((path) => (
                    <Route key={path} path={path} element={<Navigate to="/login" replace/>}/>
                ))}

                <Route path="/auth/link" element={<AuthLinkPage/>}/>

                <Route
                    path="/auth/approve/:attemptId"
                    element={<ProtectedRoute><PushApprovalPage/></ProtectedRoute>}
                />

                <Route
                    path={ACCOUNT_PAGE_PATH}
                    element={<ProtectedRoute><AccountSecurityPage/></ProtectedRoute>}
                />

                <Route path={CONFIRM_EMAIL_PATH} element={<ConfirmEmailPage/>}/>

                <Route
                    path="/complete-profile"
                    element={<ProtectedRoute><CompleteProfilePage/></ProtectedRoute>}
                />

                <Route
                    path={NOTIFICATIONS_PAGE_PATH}
                    element={<ProtectedRoute><NotificationsPage/></ProtectedRoute>}
                />
                <Route
                    path={NOTIFICATION_SETTINGS_PATH}
                    element={<ProtectedRoute><NotificationSettingsPage/></ProtectedRoute>}
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