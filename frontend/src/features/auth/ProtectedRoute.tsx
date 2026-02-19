import { type ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { Center, Loader } from '@mantine/core';

interface ProtectedRouteProps {
    children: ReactNode;
    allowedRoles?: string[]; // e.g. ['ROLE_ADMIN']
}

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
    const { isAuthenticated, user, isLoading } = useAuth();
    const location = useLocation();

    // 1. Wait for auth check to finish (prevents premature redirects)
    if (isLoading) {
        return (
            <Center h="100vh">
                <Loader size="xl" />
            </Center>
        );
    }

    // 2. Not logged in? -> Login page
    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    // 3. Logged in, but wrong role? -> Robotgame
    if (allowedRoles && user && !allowedRoles.includes(user.role || '')) {
        return <Navigate to="/robotgame" replace />;
    }

    // 4. Access Granted
    return <>{children}</>;
}