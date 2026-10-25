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

    // 3. Registration abandoned before naming yourself? -> Finish the profile first
    if (user && !user.profileComplete && location.pathname !== '/complete-profile') {
        return <Navigate to="/complete-profile" replace />;
    }

    // 4. Logged in, but wrong role? -> Robotgame
    if (allowedRoles && user && !allowedRoles.includes(user.role || '')) {
        return <Navigate to="/" replace />;
    }

    // 5. Access Granted
    return <>{children}</>;
}