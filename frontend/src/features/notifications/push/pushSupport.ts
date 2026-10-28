import type { TFunction } from 'i18next';
import { client } from '../../../api.ts';

export const SERVICE_WORKER_URL = '/sw.js';
/** Id of the user this browser's push subscription was registered for. */
const OWNER_KEY = 'cthub.push.owner';
/** When the install prompt was last dismissed (ms), or `never` after push was switched off explicitly. */
const PROMPT_KEY = 'cthub.push.prompt';
const PROMPT_SNOOZE_MS = 30 * 24 * 60 * 60 * 1000;

export function isPushSupported(): boolean {
    return typeof window !== 'undefined'
        && 'serviceWorker' in navigator
        && 'PushManager' in window
        && 'Notification' in window;
}

/** Running as installed app (home screen / desktop install) rather than in a browser tab. */
export function isStandalone(): boolean {
    return window.matchMedia?.('(display-mode: standalone)').matches
        || (navigator as Navigator & { standalone?: boolean }).standalone === true;
}

/** iPhone/iPad, where push only works once the app is added to the home screen. */
export function isIos(): boolean {
    const ua = navigator.userAgent;
    return /iPhone|iPad|iPod/.test(ua) || (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1);
}

export function urlBase64ToUint8Array(base64Url: string): Uint8Array {
    const padding = '='.repeat((4 - (base64Url.length % 4)) % 4);
    const raw = atob((base64Url + padding).replace(/-/g, '+').replace(/_/g, '/'));
    const bytes = new Uint8Array(new ArrayBuffer(raw.length));
    for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
    return bytes;
}

/** Whether an existing subscription was made with the server's current VAPID key. */
export function hasApplicationServerKey(subscription: PushSubscription, publicKey: string): boolean {
    const current = subscription.options?.applicationServerKey;
    if (!current) return true; // Not exposed by every browser; assume it matches.
    const expected = urlBase64ToUint8Array(publicKey);
    const actual = new Uint8Array(current);
    return actual.length === expected.length && actual.every((b, i) => b === expected[i]);
}

export function getPushOwner(): string | null {
    return localStorage.getItem(OWNER_KEY);
}

export function setPushOwner(userId: number | null) {
    if (userId == null) localStorage.removeItem(OWNER_KEY);
    else localStorage.setItem(OWNER_KEY, String(userId));
}

export function isPromptSnoozed(): boolean {
    const value = localStorage.getItem(PROMPT_KEY);
    if (!value) return false;
    if (value === 'never') return true;
    return Date.now() - Number(value) < PROMPT_SNOOZE_MS;
}

export function snoozePrompt(forever = false) {
    localStorage.setItem(PROMPT_KEY, forever ? 'never' : String(Date.now()));
}

export async function getPushRegistration(): Promise<ServiceWorkerRegistration | undefined> {
    if (!isPushSupported()) return undefined;
    return navigator.serviceWorker.getRegistration('/');
}

export async function getCurrentSubscription(): Promise<PushSubscription | null> {
    const registration = await getPushRegistration();
    return registration ? registration.pushManager.getSubscription() : null;
}

/**
 * Sends this browser's subscription to the backend; returns the server-side device id.
 * With `renewOnly` the server only refreshes a device it already knows (404 otherwise), so a device
 * removed from another device's settings is not silently re-added on its next start.
 */
export async function registerSubscription(
    subscription: PushSubscription,
    options: { renewOnly?: boolean } = {},
): Promise<number> {
    const json = subscription.toJSON();
    const { data } = await client.api.registerPushSubscription({
        endpoint: subscription.endpoint,
        p256dh: json.keys?.p256dh ?? '',
        auth: json.keys?.auth ?? '',
        installed: isStandalone(),
        renewOnly: options.renewOnly,
    });
    return data.id;
}

/**
 * Stops pushes to this browser: removes it from the account and drops the browser subscription.
 * Called on logout so the next person using this browser doesn't get the previous user's pushes.
 */
export async function releasePushSubscription(): Promise<void> {
    try {
        const subscription = await getCurrentSubscription();
        if (subscription) {
            await client.api.unregisterPushSubscription({ endpoint: subscription.endpoint }).catch(() => undefined);
            await subscription.unsubscribe();
        }
    } catch {
        // Best effort; the owner check on the next start catches anything left over.
    } finally {
        setPushOwner(null);
    }
}

/** Readable name such as "Chrome on Android" from a stored User-Agent. */
export function describeDevice(t: TFunction, userAgent?: string | null): string {
    const ua = userAgent ?? '';
    const browser =
        /EdgA?\/|Edg\//.test(ua) ? 'Edge'
            : /SamsungBrowser/.test(ua) ? 'Samsung Internet'
                : /OPR\/|Opera/.test(ua) ? 'Opera'
                    : /Firefox|FxiOS/.test(ua) ? 'Firefox'
                        : /Chrome|CriOS|Chromium/.test(ua) ? 'Chrome'
                            : /Safari/.test(ua) || /AppleWebKit/.test(ua) ? 'Safari'
                                : t('app.notifications.push.unknown_browser');
    const os =
        /Android/.test(ua) ? 'Android'
            : /iPhone|iPad|iPod/.test(ua) ? 'iOS'
                : /Windows/.test(ua) ? 'Windows'
                    : /Mac OS X|Macintosh/.test(ua) ? 'macOS'
                        : /CrOS/.test(ua) ? 'ChromeOS'
                            : /Linux/.test(ua) ? 'Linux'
                                : t('app.notifications.push.unknown_os');
    return t('app.notifications.push.device_label', { browser, os });
}
