import type {DeepReplace} from "../utils.ts";

export const de = {
    app: {
        common: {
            team: {
                avatar: {
                    noImage: "Kein Bild verfügbar",
                    noProfile: "Kein Profil verfügbar",
                }
            },
            robot_game: {
                round_short: "R",
                round_short_pr1: "VR1",
                round_short_pr2: "VR2",
                round_short_pr3: "VR3",
                round_short_r16: "AF",
                round_short_qf: "VF",
                round_short_sf: "HF",
                round_short_f1: "F1",
                round_short_f2: "F2",
                round_long: "Runde",
                round_long_pr1: "Vorrunde 1",
                round_long_pr2: "Vorrunde 2",
                round_long_pr3: "Vorrunde 3",
                round_long_r16: "Achtelfinale",
                round_long_qf: "Viertelfinale",
                round_long_sf: "Halbfinale",
                round_long_f1: "Finale 1",
                round_long_f2: "Finale 2",
            },
            navigation: {
                back: "Zurück",
            },
        },
        error: {
            error_404: {
                doc_title: "Seite nicht gefunden | CTH",
                title: "Seite nicht gefunden",
                description: "Leider existiert die Seite, die Sie suchen, nicht. Möglicherweise haben Sie die Adresse falsch eingegeben oder die Seite wurde auf eine andere URL verschoben.",
                back_home: "Zur Startseite",
            },
            error_500: {
                doc_title: "Serverfehler | CTH",
                title: "Etwas ist schiefgelaufen",
                description: "Unsere Server haben derzeit Probleme. Bitte versuchen Sie es später erneut oder kontaktieren Sie den Support, wenn das Problem weiterhin besteht.",
                retry: "Seite aktualisieren",
            },
        },
        header: {
            leaderboard: "Rangliste",
            login: "Anmelden",
            toggleTheme: "Farbschema wechseln",
            toggleLanguage: "Sprache wechseln",
            currentLanguage: "DE",
        },
        footer: {
            imprint: "Impressum",
            privacy: "Datenschutz",
            app_name: "Challenge Team Hub",
            copyright: "©{{year}} von Sebastian Schreitter",
            data_source: "Datenquelle: Öffentlich zugängliche Wettbewerbsergebnisse von HANDS on TECHNOLOGY e.V. (<a>www.first-lego-league.org</a>).",
        },
        sidebar: {
            menu: "Menü",
            login: "Anmelden",
            currentLanguage: "Deutsch",
            toggleTheme: "Farbschema wechseln",
        },
        search: {
            button_placeholder: "Suchen...",
            shortcut: "Strg+K",
            select_season: "Saison auswählen",
            placeholder_season: "In Saison suchen...",
            placeholder_global: "In allen Saisons suchen...",
            season_filter_label: "Nach Saison filtern",
            instructions: "Tippen, um nach Wettbewerben oder Teams zu suchen ...",
            no_results: "Keine Ergebnisse für \"{{query}}\" gefunden.",
            result: {
                profile: "Profil",
            },
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
        competition: {
            detail: {
                doc_title: "{{competitionName}} - {{seasonId}} | CTH",
                inactive_title: "Wettbewerb inaktiv",
                inactive_message: "Dieser Wettbewerb ist derzeit nicht aktiv. Es könnte sein dass er abgesagt wurde oder sich die URL geändert hat. Überprüfen sie die offizielle Website für weitere Informationen.",
                type: "Wettbewerb",
                type_REGIONAL: "Regionalwettbewerb",
                type_QUALIFICATION: "Qualifikationswettbewerb",
                type_FINAL: "DACH-Finale",
                tabs: {
                    teams: "Teams ({{teamCount}})",
                    awards: "Gesamtwertung",
                    robotgame: "Robot-Game",
                    previous_competitions: "Zugehörige Wettbewerbe",
                    previous_competitions_QUALIFICATION: "Regionalwettbewerbe",
                    previous_competitions_FINAL: "Qualifikationswettbewerbe",
                },
                season: "{{seasonId}} ({{seasonName}})",
                season_short: "{{seasonId}}",
                official_link: "Offizielle Wettbewerbsseite",
                date: "Datum",
                location: "Austragungsort",
                contact: "Kontakt",
            },
            teams: {
                title: "Registrierte Teams ({{registeredCount}}/{{maxCount}})",
                empty: "Es sind noch keine Teams für diesen Wettbewerb registriert.",
                country: "Unbekanntes Land",
                country_DE: "Deutschland",
                country_AT: "Österreich",
                country_CH: "Schweiz",
                tooltip: {
                    links: "Links",
                    grid: "Kacheln",
                    table: "Liste",
                    city: "Ort",
                    institution: "Institution",
                },
                table: {
                    id: "ID",
                    country: "Land",
                    name: "Teamname",
                    institution: "Institution",
                    city: "Ort",
                    links: "Links",
                },
            },
            awards: {
                title: "Gesamtwertung",
                title_awards: "Auszeichnungen",
                empty: "Es sind noch keine Ergebnisse für diesen Wettbewerb verfügbar.",
                category: "Kategorie",
                category_CHAMPION: "Champion",
                category_RESEARCH: "Forschung",
                category_CORE_VALUES: "Grundwerte",
                category_ROBOT_DESIGN: "Roboterdesign",
                category_ROBOT_GAME: "Robot-Game",
                category_COACHING: "Coaching",
                table: {
                    header_team: "Team",
                    header_place: "Platz",
                },
                tooltip: {
                    winner: "Pokal",
                    winner_RESEARCH: "Pokal Forschung",
                    winner_CORE_VALUES: "Pokal Grundwerte",
                    winner_ROBOT_DESIGN: "Pokal Roboterdesign",
                    winner_ROBOT_GAME: "Pokal Robot-Game",
                    winner_COACHING: "Pokal Coaching",
                    nominated: "Nominiert",
                    nominated_RESEARCH: "Nominiert Forschung",
                    nominated_CORE_VALUES: "Nominiert Grundwerte",
                    nominated_ROBOT_DESIGN: "Nominiert Roboterdesign",
                    place_ROBOT_GAME: "{{place}} Robot-Game",
                    view: {
                        categories: "Übersicht",
                        teams: "Kacheln",
                        matrix: "Tabelle",
                    }
                },
                qualified: "Qualifiziert",
                winner: "Pokal",
                nominated: "Nominiert",
                additional_nominations: "Weitere Nominierungen",
                additional_places: "Weitere Platzierungen",
                place_1: "Pokal Champion",
                place_ordinal_one: "{{count}}. Platz",
                place_ordinal_two: "{{count}}. Platz",
                place_ordinal_few: "{{count}}. Platz",
                place_ordinal_other: "{{count}}. Platz",
            },
            robot_game: {
                empty: "Es sind noch keine Ergebnisse für diesen Wettbewerb verfügbar.",
            },
            previous: {
                empty: "Es konnten keine vorherigen Wettbewerbe gefunden werden.",
                empty_QUALIFICATION: "Es konnten keine zugehörigen Regionalwettbewerbe gefunden werden.",
                empty_FINAL: "Es konnten keine zugehörigen Qualifikationswettbewerbe gefunden werden.",
                tooltip: {
                    grid: "Kacheln",
                    table: "Liste",
                },
                header: {
                    name: "Name",
                    date: "Datum",
                    type: "Typ",
                    country: "Land",
                }
            }
        },
        season_team: {
            detail: {
                doc_title: "{{teamName}} - {{seasonId}} | CTH",
                results_not_available: "Noch keine Ergebnisse vorhanden.",
                season_journey: "Saisonverlauf",
                no_competitions: "Es konnten keine Wettbewerbe für diese Saison gefunden werden.",
                awards: {
                    title: "Pokale und Nominierungen",
                    none: "Keine Pokale oder Nominierungen für diesen Wettbewerb.",
                },
                links: {
                    show_more_one: "{{count}} weiteren anzeigen",
                    show_more_other: "{{count}} weitere anzeigen",
                    show_less: "Weniger anzeigen",
                },
                robot_game: {
                    title: "Robot-Game",
                    none: "Keine Robot-Game Ergebnisse für diesen Wettbewerb.",
                    best_pr: "Beste Vorrunde",
                    points: "{{round}}: {{points}}",
                    points_empty: "{{round}}: -",
                    playoff_rank: "Playoff-Rang:",
                    prelim_rank: "Vorrunden-Rang:",
                    playoff_points: "{{round}}: {{points}}",
                    playoff_points_final: "{{round}}: {{points}}+{{additionalPoints}}",
                    pr1: "VR1",
                    pr2: "VR2",
                    pr3: "VR3",
                    r16: "AF",
                    qf: "VF",
                    sf: "HF",
                    f: "F",
                    no_playoffs: "Nicht für Playoffs qualifiziert",
                    points_tooltip: {
                        pr1: "Vorrunde 1",
                        pr2: "Vorrunde 2",
                        pr3: "Vorrunde 3",
                        r16: "Achtelfinale",
                        qf: "Viertelfinale",
                        sf: "Halbfinale",
                        f: "Finale",
                    }
                },
                robot_game_stats: {
                    title: "Robot-Game Statistik",
                    highscore: "Bestleistung",
                    average_score: "Durchschnitt",
                    official_run_count: "Offizielle Runs",
                    perfect_score: {
                      line_one: "PERFEKTE",
                      line_two: "PUNKTZAHL"
                    },
                    legend: {
                        best_pr: "Beste Vorrunde",
                        other_pr: "Andere Vorrunden",
                        playoffs: "Finalrunden",
                    }
                }
            }
        },
        team_profile: {
            detail: {
                doc_title: "{{profileName}} | CTH",
                profile_tab: "Profil",
                seasons_on_record_one: "{{count}} Saison verknüpft",
                seasons_on_record_other: "{{count}} Saisons verknüpft",
                competition_name_change: "Angetreten als <bold>{{seasonName}}</bold>",
            }
        }
    },
} as const;

export type I18nLocale = DeepReplace<typeof de, [string, string]>;