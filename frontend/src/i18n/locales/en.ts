import type { I18nLocale} from "./de.ts";

export const en: I18nLocale = {
    app: {
        common: {
            team: {
                avatar: {
                    noImage: "No image available",
                    noProfile: "No profile available",
                }
            },
            robot_game: {
                round_short: "R",
                round_short_pr1: "PR1",
                round_short_pr2: "PR2",
                round_short_pr3: "PR3",
                round_short_r16: "R16",
                round_short_qf: "QF",
                round_short_sf: "SF",
                round_short_f1: "F1",
                round_short_f2: "F2",
                round_long: "Round",
                round_long_pr1: "Preliminary Round 1",
                round_long_pr2: "Preliminary Round 2",
                round_long_pr3: "Preliminary Round 3",
                round_long_r16: "Round of 16",
                round_long_qf: "Quarterfinals",
                round_long_sf: "Semifinals",
                round_long_f1: "Finals 1",
                round_long_f2: "Finals 2",
            },
            navigation: {
                back: "Back",
            }
        },
        error: {
            error_404: {
                doc_title: "404 Not Found | CTH",
                title: "Page Not Found",
                description: "The page you are looking for does not exist or has been moved. Please check the URL and try again.",
                back_home: "Back to Home",
            },
            error_500: {
                doc_title: "500 Server Error | CTH",
                title: "Something went wrong",
                description: "Our servers are currently experiencing issues. Please try again later or contact support if the problem persists.",
                retry: "Refresh page",
            },
        },
        header: {
            leaderboard: "Leaderboard",
            login: "Login",
            toggleTheme: "Change Theme",
            toggleLanguage: "Change Language",
            currentLanguage: "EN",
        },
        footer: {
            imprint: "Imprint",
            privacy: "Privacy Policy",
            app_name: "Challenge Team Hub",
            copyright: "©{{year}} by Sebastian Schreitter",
            data_source: "Data source: Publicly available competition results from HANDS on TECHNOLOGY e.V. (<a>www.first-lego-league.org</a>)."
        },
        sidebar: {
            menu: "Menu",
            login: "Login",
            currentLanguage: "English",
            toggleTheme: "Change Color Scheme",
        },
        search: {
            button_placeholder: "Search...",
            shortcut: "Ctrl+K",
            select_season: "Select season",
            placeholder_season: "Search in season...",
            placeholder_global: "Search across all seasons...",
            season_filter_label: "Filter by season",
            instructions: "Start typing to search for competitions or teams...",
            no_results: "No results found for '{{query}}'.",
            result: {
                profile: "Profile",
            },
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
                doc_title: "{{competitionName}} - {{seasonId}} | CTH",
                inactive_title: "Competition Inactive",
                inactive_message: "This competition is currently not active. It may have been cancelled or the URL may have changed. Please check the official website for more information.",
                type: "Competitions",
                type_REGIONAL: "Regional Competition",
                type_QUALIFICATION: "Qualification Competition",
                type_FINAL: "DACH-Final",
                tabs: {
                    teams: "Teams ({{teamCount}})",
                    awards: "Overall Ranking",
                    robotgame: "Robot-Game",
                    previous_competitions: "Related Competitions",
                    previous_competitions_QUALIFICATION: "Regional Competitions",
                    previous_competitions_FINAL: "Qualification Competitions",
                },
                season: "{{seasonId}} ({{seasonName}})",
                season_short: "{{seasonId}}",
                official_link: "Official Competition Page",
                date: "Date",
                location: "Location",
                contact: "Contact",
            },
            teams: {
                title: "Registered Teams ({{registeredCount}}/{{maxCount}})",
                empty: "There are no teams registered for this competition yet.",
                country: "Unknown Country",
                country_DE: "Germany",
                country_AT: "Austria",
                country_CH: "Switzerland",
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
            awards: {
                title: "Overall Ranking",
                title_awards: "Awards",
                empty: "There are no awards for this competition yet",
                category: "Category",
                category_CHAMPION: "Champion",
                category_RESEARCH: "Research",
                category_CORE_VALUES: "Core Values",
                category_ROBOT_DESIGN: "Robot Design",
                category_ROBOT_GAME: "Robot-Game",
                category_COACHING: "Coaching",
                table: {
                    header_team: "Team",
                    header_place: "Place",
                },
                tooltip: {
                    winner: "Award",
                    winner_RESEARCH: "Award Research",
                    winner_CORE_VALUES: "Award Core Values",
                    winner_ROBOT_DESIGN: "Award Robot Design",
                    winner_ROBOT_GAME: "Award Robot-Game",
                    winner_COACHING: "Award Coaching",
                    nominated: "Nominated",
                    nominated_RESEARCH: "Nominated Research",
                    nominated_CORE_VALUES: "Nominated Core Values",
                    nominated_ROBOT_DESIGN: "Nominated Robot Design",
                    place_ROBOT_GAME: "{{place}} Robot-Game",
                    view: {
                        categories: "Overview",
                        teams: "Cards",
                        matrix: "Table",
                    }
                },
                qualified: "Qualified",
                winner: "Award",
                nominated: "Nominated",
                additional_nominations: "Addtional Nominations",
                additional_places: "Additional Places",
                place_1: "Award Champion",
                place_ordinal_one: "{{count}}st Place",
                place_ordinal_two: "{{count}}nd Place",
                place_ordinal_few: "{{count}}rd Place",
                place_ordinal_other: "{{count}}th Place",
            },
            robot_game: {
                empty: "There are no Robot-Game results for this competition yet.",
            },
            previous: {
                empty: "There are no related competitions for this competition.",
                empty_QUALIFICATION: "There are no related regional competitions for this competition.",
                empty_FINAL: "There are no related qualification competitions for this competition.",
                tooltip: {
                    grid: "Cards",
                    table: "List",
                },
                header: {
                    name: "Name",
                    date: "Date",
                    type: "Type",
                    country: "Country",
                }
            }
        },
        season_team: {
            detail: {
                doc_title: "{{teamName}} - {{seasonId}} | CTH",
                results_not_available: "No results available yet.",
                season_journey: "Season Journey",
                no_competitions: "This team has not participated in any competitions this season.",
                links: {
                    show_more_one: "Show 1 more",
                    show_more_other: "Show {{count}} more",
                    show_less: "Show less",
                },
                awards: {
                    title: "Awards & Nominations",
                    none: "No awards or nominations for this team in this competition.",
                },
                robot_game: {
                    title: "Robot-Game",
                    none: "No Robot-Game results for this team in this competition.",
                    best_pr: "Best Preliminary Round",
                    points: "{{round}}: {{points}}",
                    points_empty: "{{round}}: -",
                    playoff_rank: "Playoff Rank:",
                    prelim_rank: "Preliminary Rank:",
                    playoff_points: "{{round}}: {{points}}",
                    playoff_points_final: "{{round}}: {{points}}+{{additionalPoints}}",
                    pr1: "PR1",
                    pr2: "PR2",
                    pr3: "PR3",
                    r16: "R16",
                    qf: "QF",
                    sf: "SF",
                    f: "F",
                    no_playoffs: "Did not advance to playoffs",
                    points_tooltip: {
                        pr1: "Preliminary Round 1",
                        pr2: "Preliminary Round 2",
                        pr3: "Preliminary Round 3",
                        r16: "Round of 16",
                        qf: "Quarterfinals",
                        sf: "Semifinals",
                        f: "Finals",
                    }
                },
                robot_game_stats: {
                    title: "Robot-Game Stats",
                    highscore: "Best Score",
                    average_score: "Average Score",
                    official_run_count: "Official Runs",
                    perfect_score: {
                        line_one: "PERFECT",
                        line_two: "SCORE"
                    },
                    legend: {
                        best_pr: "Best Preliminary Round",
                        other_pr: "Other Preliminary Rounds",
                        playoffs: "Playoffs",
                    }
                }
            }
        },
        team_profile: {
            detail: {
                doc_title: "{{profileName}} | CTH",
                profile_tab: "Profile",
                seasons_on_record_one: "{{count}} Season on record",
                seasons_on_record_other: "{{count}} Seasons on record",
                competition_name_change: "Competed as <bold>{{seasonName}}</bold>",
            }
        }

    }
}