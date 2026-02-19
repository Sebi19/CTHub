import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { Center, Loader } from '@mantine/core';
import {client} from "../../api.ts";
import type {UserDto} from "../../api/generated.ts"; // Import Loader

interface AuthContextType {
    isAuthenticated: boolean;
    user: UserDto | null;
    login: (user: UserDto) => void;
    logout: () => void;
    isLoading: boolean; // Optional: expose this if needed elsewhere
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState<UserDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const checkSession = async () => {
            try {
                const userProfile = await client.api.getCurrentUser();
                setIsAuthenticated(true);
                setUser(userProfile.data)
            } catch (e) {
                // 401 means no session -> Not logged in
                setIsAuthenticated(false);
                setUser(null);
            } finally {
                setIsLoading(false);
            }
        };
        checkSession();
    }, []);

    const login = (user: UserDto) => {
        setIsAuthenticated(true);
        setUser(user);
    };

    const logout = async () => {
        try {
            await client.api.logout();
        } catch (e) { /* ignore */ }

        setIsAuthenticated(false);
        setUser(null);
    };

    // 3. BLOCK the app from rendering until we know the user's status
    if (isLoading) {
        return (
            <Center h="100vh">
                <Loader size="xl" />
            </Center>
        );
    }

    return (
        <AuthContext.Provider value={{ isAuthenticated, user, login, logout, isLoading }}>
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within an AuthProvider');
    return context;
};