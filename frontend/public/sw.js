/* CT-Hub service worker: Web Push only (no offline caching, no fetch handler). */
/* global self, fetch, URL */

const NOTIFICATION_ICON = '/icon-192.png';

// Safari/WebKit revokes push permission for pushes that show nothing, so it always gets a notification.
const ua = self.navigator.userAgent;
const IS_WEBKIT = /AppleWebKit/.test(ua) && !/Chrome|Chromium|CriOS|Edg|OPR/.test(ua);

self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));

function sameOriginPath(url) {
    try {
        const target = new URL(url || '/', self.location.origin);
        return target.origin === self.location.origin ? target.pathname + target.search + target.hash : '/';
    } catch {
        return '/';
    }
}

self.addEventListener('push', (event) => {
    let message = {};
    try {
        message = event.data ? event.data.json() : {};
    } catch {
        // Unreadable payload: still show something on WebKit, see IS_WEBKIT.
    }

    event.waitUntil((async () => {
        if (!IS_WEBKIT) {
            // A focused app window already shows the live toast (SSE); skip the duplicate system notification.
            const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
            if (windows.some((client) => client.focused)) {
                return;
            }
        }
        await self.registration.showNotification(message.title || 'CT-Hub', {
            body: message.body || '',
            tag: message.tag || undefined,
            renotify: Boolean(message.tag),
            icon: NOTIFICATION_ICON,
            data: { url: sameOriginPath(message.url), notificationId: message.notificationId ?? null },
        });
    })());
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const { url = '/', notificationId = null } = event.notification.data || {};

    event.waitUntil((async () => {
        if (notificationId != null) {
            // Best effort; the session cookie may have expired.
            fetch(`/api/notifications/${notificationId}/read`, { method: 'POST', credentials: 'include' }).catch(() => {});
        }
        const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
        const existing = windows.find((client) => client.focused) || windows[0];
        if (existing) {
            await existing.focus();
            existing.postMessage({ type: 'navigate', url });
            return;
        }
        await self.clients.openWindow(url);
    })());
});

// The browser rotated the subscription: re-register so pushes keep arriving.
self.addEventListener('pushsubscriptionchange', (event) => {
    event.waitUntil((async () => {
        const old = event.oldSubscription;
        const subscription = event.newSubscription
            || (old && await self.registration.pushManager.subscribe(old.options));
        if (!subscription) {
            return;
        }
        const json = subscription.toJSON();
        await fetch('/api/notifications/push/subscriptions', {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                endpoint: json.endpoint,
                p256dh: json.keys.p256dh,
                auth: json.keys.auth,
                // Unknown here; corrected by the app on its next start.
                installed: false,
                previousEndpoint: old ? old.endpoint : null,
            }),
        }).catch(() => {});
    })());
});
