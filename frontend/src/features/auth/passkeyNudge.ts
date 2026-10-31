const DUE_KEY = 'cthub.passkeyNudge.due';
const DISMISSED_KEY = 'cthub.passkeyNudge.dismissed';

export function markPasskeyNudgeDue() {
    try {
        sessionStorage.setItem(DUE_KEY, '1');
    } catch { /* storage unavailable */ }
}

export function isPasskeyNudgeDue(): boolean {
    try {
        return sessionStorage.getItem(DUE_KEY) === '1' && localStorage.getItem(DISMISSED_KEY) !== '1';
    } catch {
        return false;
    }
}

/** `forever` once a passkey exists or the user said no; otherwise only for this tab. */
export function clearPasskeyNudge(forever: boolean) {
    try {
        sessionStorage.removeItem(DUE_KEY);
        if (forever) localStorage.setItem(DISMISSED_KEY, '1');
    } catch { /* storage unavailable */ }
}
