import {createContext, useState, type ReactNode, useContext} from 'react';

type AppContextType = {
    activeSeason: string;
    setActiveSeason: (season: string) => void;
    globalDefaultSeason: string; // The baseline we always fall back to
};

const AppContext = createContext<AppContextType | undefined>(undefined);

export function AppProvider({ children }: { children: ReactNode }) {
    // Eventually, you will fetch '2025-26' from your backend API here
    const [globalDefaultSeason] = useState('2025-26');
    const [activeSeason, setActiveSeason] = useState(globalDefaultSeason);

    return (
        <AppContext.Provider value={{ activeSeason, setActiveSeason, globalDefaultSeason }}>
            {children}
        </AppContext.Provider>
    );
}

export const useAppContext = () => {
    const context = useContext(AppContext);
    if (!context) throw new Error("useAppContext must be used within an AppProvider");
    return context;
};