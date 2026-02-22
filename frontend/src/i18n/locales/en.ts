import type {I18nLocale} from "./de.ts";

export const en: I18nLocale = {
    app: {
        header: {
            leaderboard: "Leaderboard",
            login: "Login",
            toggleTheme: "Change Theme",
            toggleLanguage: "Change Language",
            currentLanguage: "EN",
        },
        sidebar: {
            menu: "Menu",
            login: "Login",
            currentLanguage: "English",
            toggleTheme: "Change Color Scheme",
        },
        login: {
            doc_title: "Login | CTH",
            greeting: "Welcome to Challenge Team Hub",
            email_too_short: "E-Mail too short",
            password_too_short: "Password too short",
            invalid_credentials: "Invalid E-Mail or password",
            error: "Error during login:",
            email: "E-Mail",
            password: "Password",
            placeholder: {
                email: "E-Mail...",
                password: "Password...",
            },
            login_button: "Login",
        },
        overall_robotgame: {
            title: "🏆 Robot-Game Leaderboard DACH-Region",
            doc_title: "DACH Scores | CTH",
            table: {
                country: "Country",
                team: "Team",
                qualified: "Qual.",
                location: "Location",
                best: "Max",
                median: "Median",
                average: "Avg",
                worst: "Min",
                preOne: "PR1",
                preTwo: "PR2",
                preThree: "PR3",
                bestPre: "Best PR",
                qf: "QF",
                sf: "SF",
                fOne: "F1",
                fTwo: "F2",
                all: "All",
            },
        },
    }
}