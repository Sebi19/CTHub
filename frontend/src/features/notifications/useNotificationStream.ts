import { useEffect, useRef } from 'react';
import { backendEndpoint } from '../../api.ts';
import type { NotificationDto, UnreadCountDto } from '../../api/generated.ts';

export interface NotificationStreamHandlers {
    /** The stream is (re)connected. Load or replay anything that may have been missed. */
    onOpen: () => void;
    onNotification: (notification: NotificationDto) => void;
    onUnreadCount: (count: number) => void;
}

const MIN_RETRY_MS = 5_000;
const MAX_RETRY_MS = 5 * 60_000;

/**
 * Keeps one `EventSource` open while `enabled`. The browser reconnects by itself after network
 * errors; a rejected connection (401/403) closes the source for good, so that case is retried here
 * with backoff.
 */
export function useNotificationStream(enabled: boolean, handlers: NotificationStreamHandlers) {
    const handlersRef = useRef(handlers);
    handlersRef.current = handlers;

    useEffect(() => {
        if (!enabled) {
            return;
        }

        let source: EventSource | null = null;
        let retryTimer: ReturnType<typeof setTimeout> | undefined;
        let retryDelay = MIN_RETRY_MS;
        let disposed = false;

        const parse = <T>(event: MessageEvent): T | null => {
            try {
                return JSON.parse(event.data) as T;
            } catch {
                return null;
            }
        };

        const connect = () => {
            source = new EventSource(backendEndpoint('/api/notifications/stream'), { withCredentials: true });

            source.addEventListener('ready', () => {
                retryDelay = MIN_RETRY_MS;
                handlersRef.current.onOpen();
            });
            source.addEventListener('notification', (event) => {
                const notification = parse<NotificationDto>(event as MessageEvent);
                if (notification) handlersRef.current.onNotification(notification);
            });
            source.addEventListener('unread-count', (event) => {
                const dto = parse<UnreadCountDto>(event as MessageEvent);
                if (dto) handlersRef.current.onUnreadCount(dto.count ?? 0);
            });
            source.onerror = () => {
                if (source?.readyState === EventSource.CLOSED && !disposed) {
                    source.close();
                    retryTimer = setTimeout(connect, retryDelay);
                    retryDelay = Math.min(retryDelay * 2, MAX_RETRY_MS);
                }
            };
        };

        connect();

        return () => {
            disposed = true;
            clearTimeout(retryTimer);
            source?.close();
        };
    }, [enabled]);
}
