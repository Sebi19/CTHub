import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { onProfileIncomplete } from './profileIncomplete.ts';

/**
 * Mounted once inside the router: turns a `PROFILE_INCOMPLETE` API rejection into a redirect to the
 * complete-profile page.
 */
export function ProfileCompletionRedirect() {
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        onProfileIncomplete(() => {
            if (location.pathname !== '/complete-profile') {
                navigate('/complete-profile', { replace: true });
            }
        });
        return () => onProfileIncomplete(null);
    }, [navigate, location.pathname]);

    return null;
}
