import React from 'react'
import ReactDOM from 'react-dom/client'
import { MantineProvider, createTheme } from '@mantine/core';
import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom'

// 1. Import Core Mantine Styles
import '@mantine/core/styles.css';
import '@mantine/dates/styles.css'; // If using date pickers
import 'mantine-react-table/styles.css'; // MRT Styles

import App from './App.tsx'
import {LeaderboardPage} from "./pages/LeaderboardPage.tsx";

const theme = createTheme({
    /** Put your mantine theme override here */
    primaryColor: 'blue',
});

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <MantineProvider theme={theme} defaultColorScheme={"dark"}>
            <BrowserRouter>
                <Routes>
                    {/* PARENT ROUTE: App (The Shell)
                   All nested routes render inside App's <Outlet />
                */}
                    <Route path="/" element={<App />}>

                        {/* Index: Redirect to robotgame */}
                        <Route index element={<Navigate to="/robotgame" replace />} />

                        {/* Public Pages */}
                        <Route path="robotgame" element={<LeaderboardPage />} />

                        {/* Example Protected Page (Add later) */}
                        {/* <Route path="my-team" element={<ProtectedRoute><MyTeamPage /></ProtectedRoute>} /> */}

                    </Route>
                </Routes>
            </BrowserRouter>
        </MantineProvider>
    </React.StrictMode>,
)