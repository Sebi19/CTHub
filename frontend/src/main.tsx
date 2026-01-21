import React from 'react'
import ReactDOM from 'react-dom/client'
import { MantineProvider, createTheme } from '@mantine/core';
import { BrowserRouter } from 'react-router-dom'

// 1. Import Core Mantine Styles
import '@mantine/core/styles.css';
import '@mantine/dates/styles.css'; // If using date pickers
import 'mantine-react-table/styles.css'; // MRT Styles

import App from './App.tsx'

const theme = createTheme({
    /** Put your mantine theme override here */
    primaryColor: 'blue',
});

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <MantineProvider theme={theme} defaultColorScheme={"dark"}>
            <BrowserRouter>
                <App />
            </BrowserRouter>
        </MantineProvider>
    </React.StrictMode>,
)