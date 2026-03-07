import React, {useEffect} from 'react' // 1. Add useState
import ReactDOM from 'react-dom/client'
import { MantineProvider} from '@mantine/core'; // 2. Add Affix and Button
import { BrowserRouter } from 'react-router-dom'
import { ModalsProvider } from "@mantine/modals";
import './i18n/config.ts';

// Imports...
import '@mantine/core/styles.css';
import '@mantine/dates/styles.css';
import '@fontsource/geist-sans/400.css';
import '@fontsource/geist-sans/500.css';
import '@fontsource/geist-sans/600.css';
import '@fontsource/geist-sans/700.css';
import '@fontsource/geist-mono/400.css'
import 'mantine-react-table/styles.css';
import '@mantine/carousel/styles.css';
import { Notifications } from "@mantine/notifications";
import { AuthProvider } from "./features/auth/AuthContext.tsx";
import { AppRoutes } from "./appRoutes.tsx";
import { shadcnCssVariableResolver } from "./cssVariableResolver.ts";
import { shadcnTheme } from "./theme.ts";
import "./style.css";

// 3. Create a wrapper component
function RootWrapper() {
    const useShadcn = true;

    useEffect(() => {
        if (useShadcn) {
            document.body.setAttribute('data-theme-style', 'shadcn');
        } else {
            document.body.removeAttribute('data-theme-style');
        }
    }, [useShadcn]);

    return (
        <MantineProvider
            theme={useShadcn ? shadcnTheme : {}}
            // Make sure to drop the resolver when dropping the theme!
            cssVariablesResolver={useShadcn ? shadcnCssVariableResolver : undefined}
            defaultColorScheme={"dark"}
        >
            <Notifications position={"bottom-right"} />
            <ModalsProvider>
                <AuthProvider>
                    <BrowserRouter>
                        <AppRoutes />
                    </BrowserRouter>
                </AuthProvider>
            </ModalsProvider>
        </MantineProvider>
    );
}

// 5. Render the wrapper instead of the raw provider
ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <RootWrapper />
    </React.StrictMode>,
)