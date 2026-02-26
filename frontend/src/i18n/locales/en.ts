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
        competition: {
            detail: {
                error_loading: "Error loading competition details",
                inactive_title: "Competition Inactive",
                inactive_message: "This competition is currently not active. It may have been cancelled or the URL may have changed. Please check the official website for more information.",
                type: {
                    REGIONAL: "Regional Competition",
                    QUALIFICATION: "Qualification Competition",
                    FINAL: "DACH-Final",
                },
                tabs: {
                    teams: "Teams ({{teamCount}})",
                    awards: "Overall Ranking",
                    robotgame: "Robot-Game",
                },
                season: "{{seasonName}} - {{seasonId}}",
                official_link: "Official Competition Page",
                date: "Date",
                location: "Location",
                contact: "Contact",
            },
            teams: {
                title: "Registered Teams ({{registeredCount}}/{{maxCount}})",
                empty: "There are no teams registered for this competition yet.",
                country: {
                    DE: "Germany",
                    AT: "Austria",
                    CH: "Switzerland",
                },
                tooltip: {
                    links: "Links",
                    grid: "Cards",
                    table: "List",
                    city: "City",
                    institution: "Institution",
                },
                table: {
                    id: "ID",
                    country: "Country",
                    name: "Team Name",
                    institution: "Institution",
                    city: "City",
                    links: "Links",
                },
            },
            }
    }
}