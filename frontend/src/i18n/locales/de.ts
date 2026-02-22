import type {DeepReplace} from "../utils.ts";

export const de = {
    app: {
        header: {
            leaderboard: "Rangliste",
            login: "Anmelden",
            toggleTheme: "Farbschema wechseln",
            toggleLanguage: "Sprache wechseln",
            currentLanguage: "DE",
        },
        sidebar: {
            menu: "Menü",
            login: "Anmelden",
            currentLanguage: "Deutsch",
            toggleTheme: "Farbschema wechseln",
        },
        login: {
            doc_title: "Anmelden | CTH",
            greeting: "Willkommen bei Challenge Team Hub",
            email_too_short: "E-Mail Adresse zu kurz",
            password_too_short: "Passwort zu kurz",
            invalid_credentials: "E-Mail Adresse oder Passwort ungültig",
            error: "Fehler bei der Anmeldung:",
            email: "E-Mail Adresse",
            password: "Passwort",
            placeholder: {
                email: "E-Mail...",
                password: "Passwort...",
            },
            login_button: "Anmelden",
        },
        overall_robotgame: {
            title: "🏆 Robot-Game Rangliste DACH-Region",
            doc_title: "DACH Punkte | CTH",
            table: {
                country: "Land",
                team: "Team",
                qualified: "Qual.",
                location: "Ort",
                best: "Max",
                median: "Median",
                average: "Avg",
                worst: "Min",
                preOne: "VR1",
                preTwo: "VR2",
                preThree: "VR3",
                bestPre: "Beste VR",
                qf: "VF",
                sf: "HF",
                fOne: "F1",
                fTwo: "F2",
                all: "Alle",
          },
        },
    },
} as const;

export type I18nLocale = DeepReplace<typeof de, [string, string]>;