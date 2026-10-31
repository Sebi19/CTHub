import { useCallback } from 'react';
import { useLocation, useNavigate, type Location } from 'react-router-dom';
import type { UserDto } from '../../api/generated.ts';
import { useAuth } from './AuthContext.tsx';
import { markPasskeyNudgeDue } from './passkeyNudge.ts';

/**
 * Finishes a sign-in in this browser: stores the user and moves on to the page that sent the
 * visitor to the login (or to the profile form for brand-new accounts).
 */
export function useFinishSignIn() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    return useCallback((user: UserDto, viaChallenge: boolean) => {
        login(user);
        if (viaChallenge) {
            // Signed in without a passkey: a good moment to offer one for next time.
            markPasskeyNudgeDue();
        }
        const from = (location.state as { from?: Location } | null)?.from;
        const target = !user.profileComplete ? '/complete-profile'
            : from ? `${from.pathname}${from.search}${from.hash}` : '/';
        navigate(target, { replace: true });
    }, [login, navigate, location.state]);
}
