import {createContext, useState, type ReactNode, useContext, useEffect} from 'react';
import {client} from "../api.ts";
import type {SeasonDto} from "../api/generated.ts";

type AppContextType = {
    activeSeason: string;
    setActiveSeason: (season: string) => void;
    globalDefaultSeason: string; // The baseline we always fall back to
    availableSeasonIds: string[];
    getSeasonDto: (seasonId: string) => SeasonDto;
};

const AppContext = createContext<AppContextType | undefined>(undefined);

export function AppProvider({ children }: { children: ReactNode }) {
    const [availableSeasons, setAvailableSeasons] = useState<SeasonDto[]>([]);
    const [availableSeasonIds, setAvailableSeasonIds] = useState<string[]>(['2025-26']);
    const [globalDefaultSeason, setGlobalDefaultSeason] = useState('2025-26');
    const [activeSeason, setActiveSeason] = useState(globalDefaultSeason);

    useEffect(() => {
        // Fetch the seasons exactly ONE time when the app loads
        client.api.getAllSeasons() // <-- Replace with your actual API method
            .then((res) => {
                // Assuming your API returns an array of SeasonDto: [{id: "2025-26", active: true}, ...]
                const fetchedSeasons = res.data;

                setAvailableSeasons(fetchedSeasons);

                const seasonIds = fetchedSeasons.map(s => s.id);
                setAvailableSeasonIds(seasonIds);

                // 2. Find the one marked as "active" in the database, or default to the first one
                const currentSeasonId = fetchedSeasons.find(s => s.active)?.id || seasonIds[0];

                if (currentSeasonId) {
                    setGlobalDefaultSeason(currentSeasonId);

                    // We only want to auto-update the activeSeason if the user
                    // hasn't ALREADY navigated to a specific detail page while the API was loading.
                    setActiveSeason((prev) =>
                        prev === '2025-26' ? currentSeasonId : prev
                    );
                }
            })
            .catch((err) => {
                console.error("Failed to load seasons:", err);
                // It will just gracefully fall back to the initial '2025-26' state!
            });
    }, []);

    const getSeasonDto = (seasonId: string): SeasonDto => {
        const season = availableSeasons.find(s => s.id === seasonId);

        if (season) return season;

        return {
            id: seasonId,
            name: seasonId,
            active: false,
            startYear: parseInt(seasonId.split('-')[0]),
            maxPoints: 0,
        }
    }

    return (
        <AppContext.Provider value={{ activeSeason, setActiveSeason, globalDefaultSeason, availableSeasonIds, getSeasonDto }}>
            {children}
        </AppContext.Provider>
    );
}

export const useAppContext = () => {
    const context = useContext(AppContext);
    if (!context) throw new Error("useAppContext must be used within an AppProvider");
    return context;
};