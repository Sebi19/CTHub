const STORAGE_KEY = 'cthub.pendingRegistration';

export interface PendingRegistration {
    registrationId: string;
    email: string;
    /**
     * The two-digit number this browser displays. Whoever opens the e-mailed link picks it out of
     * three to hand the session back here.
     */
    challengeNumber?: number;
}

/**
 * The pending registration survives a reload of the waiting page, so the user can leave the tab
 * open (or refresh it) without losing the polling handle.
 */
export function storePendingRegistration(handle: PendingRegistration) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(handle));
}

export function readPendingRegistration(): PendingRegistration | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
        const parsed = JSON.parse(raw) as PendingRegistration;
        return parsed.registrationId && parsed.email ? parsed : null;
    } catch {
        return null;
    }
}

export function clearPendingRegistration() {
    sessionStorage.removeItem(STORAGE_KEY);
}
