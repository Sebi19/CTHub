import React from 'react'
import ReactDOM from 'react-dom/client'
import {createTheme, MantineProvider} from '@mantine/core';
import {BrowserRouter} from 'react-router-dom'
import {ModalsProvider} from "@mantine/modals";

// 1. Import Core Mantine Styles
import '@mantine/core/styles.css';
import '@mantine/dates/styles.css'; // If using date pickers
import 'mantine-react-table/styles.css'; // MRT Styles
import {Notifications} from "@mantine/notifications";
import {AuthProvider} from "./features/auth/AuthContext.tsx";
import {AppRoutes} from "./appRoutes.tsx";

const theme = createTheme({
    /** Put your mantine theme override here */
    primaryColor: 'blue',
});

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <MantineProvider theme={theme} defaultColorScheme={"dark"}>
            <Notifications position={"bottom-right"} />
            <ModalsProvider>
                <AuthProvider>
                    <BrowserRouter>
                        <AppRoutes />
                    </BrowserRouter>
                </AuthProvider>
            </ModalsProvider>
        </MantineProvider>
    </React.StrictMode>,
)