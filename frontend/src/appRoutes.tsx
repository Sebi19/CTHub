import {useAuth} from "./features/auth/AuthContext.tsx";
import {Navigate, Route, Routes} from "react-router-dom";
import App from "./App.tsx";
import {LoginPage} from "./features/auth/LoginPage.tsx";
import {LeaderboardPage} from "./pages/LeaderboardPage.tsx";

export function AppRoutes() {
    const {isAuthenticated} = useAuth();

    return (
        <Routes>
            {/* PARENT ROUTE: App (The Shell)
                           All nested routes render inside App's <Outlet />
                        */}
            <Route path="/" element={<App/>}>

                {/* Index: Redirect to robotgame */}
                <Route index element={<Navigate to="/robotgame" replace/>}/>

                <Route path="/login" element={!isAuthenticated ? <LoginPage/> : <Navigate to="/robotgame"/>}/>

                {/* Public Pages */}
                <Route path="robotgame" element={<LeaderboardPage/>}/>

                {/* Example Protected Page (Add later) */}
                {/* <Route path="my-team" element={<ProtectedRoute><MyTeamPage /></ProtectedRoute>} /> */}

            </Route>
        </Routes>
    );
}