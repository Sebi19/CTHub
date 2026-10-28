import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiErrorStatus } from '../../auth/apiError.ts';
import { client } from '../../../api.ts';
import { useAuth } from '../../auth/AuthContext.tsx';
import { useNotifications } from '../NotificationsContext.tsx';
import {
    SERVICE_WORKER_URL,
    getPushOwner,
    hasApplicationServerKey,
    isPushSupported,
    registerSubscription,
    setPushOwner,
    snoozePrompt,
    urlBase64ToUint8Array,
} from './pushSupport.ts';

interface PushContextType {
    /** Browser has the APIs (service worker, Push API, Notification API). */
    supported: boolean;
    /** Server has VAPID keys configured. `null` while loading. */
    available: boolean | null;
    permission: NotificationPermission;
    /** This browser currently receives pushes for the signed-in user. */
    subscribed: boolean;
    /** Server-side id of this browser's subscription, to mark it in the device list. */
    currentDeviceId: number | null;
    /** Initial sync finished; avoids flashing prompts before the state is known. */
    ready: boolean;
    busy: boolean;
    /** Must be called from a click handler: it asks for notification permission. */
    enable: () => Promise<boolean>;
    disable: () => Promise<void>;
}

const PushContext = createContext<PushContextType | null>(null);

const initialPermission = (): NotificationPermission =>
    isPushSupported() ? Notification.permission : 'default';

export function PushProvider({ children }: { children: ReactNode }) {
    const navigate = useNavigate();
    const { user } = useAuth();
    const { enabled } = useNotifications();
    const supported = isPushSupported();
    const userId = enabled ? user?.id ?? null : null;

    const [publicKey, setPublicKey] = useState<string | null>(null);
    const [available, setAvailable] = useState<boolean | null>(null);
    const [permission, setPermission] = useState<NotificationPermission>(initialPermission);
    const [currentDeviceId, setCurrentDeviceId] = useState<number | null>(null);
    const [ready, setReady] = useState(false);
    const [busy, setBusy] = useState(false);
    const registration = useRef<Promise<ServiceWorkerRegistration> | null>(null);

    const getRegistration = useCallback(() => {
        registration.current ??= navigator.serviceWorker.register(SERVICE_WORKER_URL, { scope: '/' })
            .then(() => navigator.serviceWorker.ready);
        return registration.current;
    }, []);

    const subscribe = useCallback(async (key: string, ownerId: number) => {
        const reg = await getRegistration();
        const subscription = await reg.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: urlBase64ToUint8Array(key),
        });
        const id = await registerSubscription(subscription);
        setPushOwner(ownerId);
        setCurrentDeviceId(id);
    }, [getRegistration]);

    // Load config, register the worker and reconcile this browser's subscription with the account.
    useEffect(() => {
        if (!supported || userId == null) {
            setReady(false);
            setCurrentDeviceId(null);
            return;
        }
        let cancelled = false;
        (async () => {
            try {
                const { data: config } = await client.api.getPushConfig();
                if (cancelled) return;
                setAvailable(config.enabled);
                setPublicKey(config.publicKey ?? null);
                if (!config.enabled || !config.publicKey) return;

                const reg = await getRegistration();
                const existing = await reg.pushManager.getSubscription();
                const ownedByUser = getPushOwner() === String(userId);
                setPermission(Notification.permission);

                if (existing && (!ownedByUser || !hasApplicationServerKey(existing, config.publicKey))) {
                    // Left over from another account, or from a rotated server key.
                    await existing.unsubscribe();
                    if (!ownedByUser) setPushOwner(null);
                }
                if (!ownedByUser || Notification.permission !== 'granted') return;

                const current = await reg.pushManager.getSubscription();
                if (!current) {
                    // Gone (rotated key, browser reset): enabling again is an explicit user decision.
                    setPushOwner(null);
                    return;
                }
                try {
                    const id = await registerSubscription(current, { renewOnly: true });
                    if (!cancelled) setCurrentDeviceId(id);
                } catch (error) {
                    if (apiErrorStatus(error) === 404) {
                        // Removed from the device list elsewhere: respect that instead of re-adding it.
                        await current.unsubscribe();
                        setPushOwner(null);
                    } else {
                        throw error;
                    }
                }
            } catch {
                // Push is an extra; the app works without it.
            } finally {
                if (!cancelled) setReady(true);
            }
        })();
        return () => {
            cancelled = true;
        };
    }, [supported, userId, getRegistration]);

    // Notifications already shown by the OS are stale once the app is in front; clear them.
    useEffect(() => {
        if (!supported || userId == null) return;
        const sweep = () => {
            if (document.visibilityState !== 'visible') return;
            navigator.serviceWorker.getRegistration('/')
                .then((reg) => reg?.getNotifications())
                .then((shown) => shown?.forEach((n) => n.close()))
                .catch(() => undefined);
        };
        sweep();
        document.addEventListener('visibilitychange', sweep);
        return () => document.removeEventListener('visibilitychange', sweep);
    }, [supported, userId]);

    // Clicking a system notification while the app is open navigates inside the running app.
    useEffect(() => {
        if (!supported) return;
        const onMessage = (event: MessageEvent) => {
            const data = event.data as { type?: string; url?: string } | null;
            if (data?.type === 'navigate' && typeof data.url === 'string' && data.url.startsWith('/')) {
                navigate(data.url);
            }
        };
        navigator.serviceWorker.addEventListener('message', onMessage);
        return () => navigator.serviceWorker.removeEventListener('message', onMessage);
    }, [supported, navigate]);

    const enable = useCallback(async () => {
        if (!supported || !publicKey || userId == null) return false;
        setBusy(true);
        try {
            // First await: the permission prompt needs the click's user activation.
            const result = await Notification.requestPermission();
            setPermission(result);
            if (result !== 'granted') return false;
            await subscribe(publicKey, userId);
            return true;
        } finally {
            setBusy(false);
        }
    }, [supported, publicKey, userId, subscribe]);

    const disable = useCallback(async () => {
        setBusy(true);
        try {
            const reg = await navigator.serviceWorker.getRegistration('/');
            const subscription = await reg?.pushManager.getSubscription();
            if (subscription) {
                await client.api.unregisterPushSubscription({ endpoint: subscription.endpoint });
                await subscription.unsubscribe();
            }
            setPushOwner(null);
            setCurrentDeviceId(null);
            snoozePrompt(true);
        } finally {
            setBusy(false);
        }
    }, []);

    const value = useMemo<PushContextType>(() => ({
        supported,
        available,
        permission,
        subscribed: currentDeviceId != null,
        currentDeviceId,
        ready,
        busy,
        enable,
        disable,
    }), [supported, available, permission, currentDeviceId, ready, busy, enable, disable]);

    return <PushContext.Provider value={value}>{children}</PushContext.Provider>;
}

export const usePush = () => {
    const context = useContext(PushContext);
    if (!context) throw new Error('usePush must be used within a PushProvider');
    return context;
};
