type Handler = () => void;

let handler: Handler | null = null;

/**
 * The backend rejects mutating calls from users with an incomplete profile (`PROFILE_INCOMPLETE`).
 * The axios interceptor in `api.ts` is outside the router, so it reports through this hook and a
 * router-aware component performs the actual navigation.
 */
export function onProfileIncomplete(fn: Handler | null) {
    handler = fn;
}

export function reportProfileIncomplete() {
    handler?.();
}
