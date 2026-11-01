import { useSyncExternalStore } from 'react';
import { SERVICE_WORKER_URL, isIos, isStandalone } from '../notifications/push/pushSupport.ts';

interface BeforeInstallPromptEvent extends Event {
    prompt: () => Promise<void>;
    userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

/** When the install banner was last dismissed (ms). */
const BANNER_KEY = 'cthub.pwa.banner';
const BANNER_SNOOZE_MS = 14 * 24 * 60 * 60 * 1000;
/** Query parameter of the QR-code link: shows the install banner even if it was dismissed. */
export const INSTALL_PARAM = 'install';

let deferredPrompt: BeforeInstallPromptEvent | null = null;
const listeners = new Set<() => void>();
const notify = () => listeners.forEach((listener) => listener());

// Registered at module load (imported from main.tsx): the event fires once, early, and is lost otherwise.
if (typeof window !== 'undefined') {
    window.addEventListener('beforeinstallprompt', (event) => {
        // Replaces the browser's own mini-infobar with our banner.
        event.preventDefault();
        deferredPrompt = event as BeforeInstallPromptEvent;
        notify();
    });
    window.addEventListener('appinstalled', () => {
        deferredPrompt = null;
        notify();
    });
    // Installability in some browsers needs a registered worker - also for visitors who are not signed in.
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register(SERVICE_WORKER_URL, { scope: '/' }).catch(() => undefined);
        });
    }
}

const subscribe = (listener: () => void) => {
    listeners.add(listener);
    return () => {
        listeners.delete(listener);
    };
};

/** Whether the browser offers a one-tap install (Chrome, Edge, Samsung Internet …). */
export function useCanPromptInstall(): boolean {
    return useSyncExternalStore(subscribe, () => deferredPrompt != null, () => false);
}

/** Opens the browser's install dialog; resolves to whether the user accepted. */
export async function promptInstall(): Promise<boolean> {
    const event = deferredPrompt;
    if (!event) return false;
    deferredPrompt = null;
    notify();
    await event.prompt();
    const { outcome } = await event.userChoice;
    return outcome === 'accepted';
}

export function isMobileDevice(): boolean {
    return isIos() || /Android|Mobi/i.test(navigator.userAgent);
}

/** iOS has no install dialog: the app is added via Share → "Add to Home Screen". */
export function needsManualIosInstall(): boolean {
    return isIos() && !isStandalone();
}

export function isInstallBannerSnoozed(): boolean {
    const value = localStorage.getItem(BANNER_KEY);
    return !!value && Date.now() - Number(value) < BANNER_SNOOZE_MS;
}

export function snoozeInstallBanner() {
    localStorage.setItem(BANNER_KEY, String(Date.now()));
}

/** Link for the QR code shown on desktop: opens CT-Hub on the phone with the install banner. */
export function phoneInstallUrl(): string {
    return `${window.location.origin}/?${INSTALL_PARAM}=1`;
}
