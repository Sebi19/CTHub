import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { notifications as toasts } from '@mantine/notifications';
import { IconBell } from '@tabler/icons-react';
import { client } from '../../api.ts';
import type { NotificationDto } from '../../api/generated.ts';
import { useAuth } from '../auth/AuthContext.tsx';
import { useNotificationStream } from './useNotificationStream.ts';
import { getNotificationIcon, getNotificationText } from './notificationText.ts';

export const NOTIFICATIONS_PAGE_PATH = '/account/notifications';
/** Also linked from every notification e-mail (backend template {@code notification.html}). */
export const NOTIFICATION_SETTINGS_PATH = '/account/notifications/settings';

/** How many notifications the bell popover shows. */
const RECENT_SIZE = 10;
/** A reconnect replay with more items than this produces one summary toast instead of many. */
const MAX_INDIVIDUAL_TOASTS = 3;

export type NotificationChange =
    | { kind: 'created'; notification: NotificationDto }
    | { kind: 'updated'; notification: NotificationDto }
    | { kind: 'allRead'; upToId: number; readAt: string };

type Listener = (change: NotificationChange) => void;

interface NotificationsContextType {
    /** `false` for anonymous users and accounts that still have to complete their profile. */
    enabled: boolean;
    unreadCount: number;
    /** Newest notifications, for the bell popover. */
    recent: NotificationDto[];
    markRead: (notification: NotificationDto) => Promise<void>;
    markUnread: (notification: NotificationDto) => Promise<void>;
    markAllRead: () => Promise<void>;
    /** Explicit click: marks the notification read and follows its link. */
    openNotification: (notification: NotificationDto) => void;
    /** Lets lists outside the provider (e.g. the notifications page) follow changes. */
    subscribe: (listener: Listener) => () => void;
}

const NotificationsContext = createContext<NotificationsContextType | null>(null);

const byNewest = (a: NotificationDto, b: NotificationDto) => b.id - a.id;

function mergeRecent(current: NotificationDto[], incoming: NotificationDto[]) {
    const byId = new Map(current.map((n) => [n.id, n]));
    incoming.forEach((n) => byId.set(n.id, n));
    return [...byId.values()].sort(byNewest).slice(0, RECENT_SIZE);
}

export function NotificationsProvider({ children }: { children: ReactNode }) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { isAuthenticated, user } = useAuth();
    const enabled = isAuthenticated && !!user?.profileComplete;

    const [unreadCount, setUnreadCount] = useState(0);
    const [recent, setRecent] = useState<NotificationDto[]>([]);

    const knownIds = useRef(new Set<number>());
    const latestId = useRef(0);
    const initialized = useRef(false);
    const listeners = useRef(new Set<Listener>());

    const emit = useCallback((change: NotificationChange) => {
        listeners.current.forEach((listener) => listener(change));
    }, []);

    const subscribe = useCallback((listener: Listener) => {
        listeners.current.add(listener);
        return () => {
            listeners.current.delete(listener);
        };
    }, []);

    /** Records notifications; returns those not seen before. */
    const remember = useCallback((items: NotificationDto[]) => {
        const fresh = items.filter((n) => !knownIds.current.has(n.id));
        fresh.forEach((n) => {
            knownIds.current.add(n.id);
            latestId.current = Math.max(latestId.current, n.id);
        });
        if (fresh.length > 0) {
            setRecent((current) => mergeRecent(current, fresh));
        }
        return fresh;
    }, []);

    const refreshUnreadCount = useCallback(async () => {
        try {
            const { data } = await client.api.getUnreadCount();
            setUnreadCount(data.count ?? 0);
        } catch {
            // The stream delivers the next count anyway.
        }
    }, []);

    const applyUpdate = useCallback((updated: NotificationDto) => {
        setRecent((current) => current.map((n) => (n.id === updated.id ? updated : n)));
        emit({ kind: 'updated', notification: updated });
    }, [emit]);

    const showActionError = useCallback(() => {
        toasts.show({ color: 'red', message: t('app.notifications.action_error') });
    }, [t]);

    const markRead = useCallback(async (notification: NotificationDto) => {
        if (notification.readAt) return;
        try {
            const { data } = await client.api.markNotificationRead(notification.id);
            applyUpdate(data);
            await refreshUnreadCount();
        } catch {
            showActionError();
        }
    }, [applyUpdate, refreshUnreadCount, showActionError]);

    const markUnread = useCallback(async (notification: NotificationDto) => {
        if (!notification.readAt) return;
        try {
            const { data } = await client.api.markNotificationUnread(notification.id);
            applyUpdate(data);
            await refreshUnreadCount();
        } catch {
            showActionError();
        }
    }, [applyUpdate, refreshUnreadCount, showActionError]);

    const markAllRead = useCallback(async () => {
        const upToId = latestId.current;
        if (upToId === 0) return;
        try {
            const { data } = await client.api.markAllNotificationsRead({ upToId });
            const readAt = new Date().toISOString();
            setRecent((current) => current.map((n) => (n.id <= upToId && !n.readAt ? { ...n, readAt } : n)));
            setUnreadCount(data.count ?? 0);
            emit({ kind: 'allRead', upToId, readAt });
        } catch {
            showActionError();
        }
    }, [emit, showActionError]);

    const openNotification = useCallback((notification: NotificationDto) => {
        toasts.hide(`notification-${notification.id}`);
        void markRead(notification);
        if (notification.link) {
            navigate(notification.link);
        }
    }, [markRead, navigate]);

    const showToasts = useCallback((fresh: NotificationDto[]) => {
        if (fresh.length === 0 || document.visibilityState !== 'visible') return;

        if (fresh.length > MAX_INDIVIDUAL_TOASTS) {
            toasts.show({
                id: 'notification-summary',
                title: t('app.notifications.summary_toast_title'),
                message: t('app.notifications.summary_toast_body', { count: fresh.length }),
                icon: <IconBell size={18} />,
                style: { cursor: 'pointer' },
                onClick: () => {
                    toasts.hide('notification-summary');
                    navigate(NOTIFICATIONS_PAGE_PATH);
                },
            });
            return;
        }

        fresh.forEach((notification) => {
            const { title, body } = getNotificationText(t, notification);
            const { icon: Icon, color } = getNotificationIcon(notification.type);
            toasts.show({
                id: `notification-${notification.id}`,
                title,
                message: body ?? '',
                icon: <Icon size={18} />,
                color,
                autoClose: 8000,
                style: { cursor: 'pointer' },
                onClick: () => openNotification(notification),
            });
        });
    }, [navigate, openNotification, t]);

    const loadInitial = useCallback(async () => {
        try {
            const [{ data: page }, { data: count }] = await Promise.all([
                client.api.listNotifications({ size: RECENT_SIZE }),
                client.api.getUnreadCount(),
            ]);
            remember(page.items);
            setUnreadCount(count.count ?? 0);
            initialized.current = true;
        } catch {
            // Retried on the next stream (re)connect.
        }
    }, [remember]);

    const replayMissed = useCallback(async () => {
        try {
            const { data } = await client.api.replayNotifications({ after: latestId.current });
            const fresh = remember(data);
            fresh.forEach((notification) => emit({ kind: 'created', notification }));
            showToasts(fresh);
            await refreshUnreadCount();
        } catch {
            // Retried on the next stream (re)connect.
        }
    }, [emit, refreshUnreadCount, remember, showToasts]);

    useNotificationStream(enabled, {
        onOpen: () => void (initialized.current ? replayMissed() : loadInitial()),
        onNotification: (notification) => {
            const fresh = remember([notification]);
            fresh.forEach((n) => emit({ kind: 'created', notification: n }));
            showToasts(fresh);
        },
        onUnreadCount: setUnreadCount,
    });

    // Forget everything on logout so the next account starts clean.
    useEffect(() => {
        if (!enabled) {
            knownIds.current = new Set();
            latestId.current = 0;
            initialized.current = false;
            setRecent([]);
            setUnreadCount(0);
        }
    }, [enabled]);

    const value = useMemo<NotificationsContextType>(() => ({
        enabled, unreadCount, recent, markRead, markUnread, markAllRead, openNotification, subscribe,
    }), [enabled, unreadCount, recent, markRead, markUnread, markAllRead, openNotification, subscribe]);

    return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>;
}

export const useNotifications = () => {
    const context = useContext(NotificationsContext);
    if (!context) throw new Error('useNotifications must be used within a NotificationsProvider');
    return context;
};
