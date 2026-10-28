import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import type { TFunction } from 'i18next';
import { IconBell, IconConfetti, IconSpeakerphone, type Icon } from '@tabler/icons-react';
import { NotificationChannelDto, type NotificationDto, NotificationTypeDto } from '../../api/generated.ts';

dayjs.extend(relativeTime);

export interface NotificationText {
    title: string;
    body?: string;
}

/** Notifications store a type plus parameters; the text is rendered here in the current language. */
export function getNotificationText(t: TFunction, notification: NotificationDto): NotificationText {
    const params = notification.params ?? {};
    switch (notification.type) {
        case NotificationTypeDto.WELCOME:
            return {
                title: t('app.notifications.types.WELCOME.title', { firstName: params.firstName ?? '' }),
                body: t('app.notifications.types.WELCOME.body'),
            };
        case NotificationTypeDto.ANNOUNCEMENT:
            // Free text written by an admin, already in its final form.
            return {
                title: params.title || t('app.notifications.types.ANNOUNCEMENT.title'),
                body: params.body,
            };
        default:
            return { title: t('app.notifications.types.unknown.title') };
    }
}

export function getNotificationTypeLabel(t: TFunction, type: NotificationTypeDto): { label: string; description: string } {
    switch (type) {
        case NotificationTypeDto.WELCOME:
            return { label: t('app.notifications.types.WELCOME.label'), description: t('app.notifications.types.WELCOME.description') };
        case NotificationTypeDto.ANNOUNCEMENT:
            return { label: t('app.notifications.types.ANNOUNCEMENT.label'), description: t('app.notifications.types.ANNOUNCEMENT.description') };
        default:
            return { label: t('app.notifications.types.unknown.title'), description: '' };
    }
}

export function getChannelLabel(t: TFunction, channel: NotificationChannelDto): string {
    switch (channel) {
        case NotificationChannelDto.EMAIL:
            return t('app.notifications.settings.channels.EMAIL');
        case NotificationChannelDto.PUSH:
            return t('app.notifications.settings.channels.PUSH');
        default:
            return channel;
    }
}

export function getNotificationIcon(type: NotificationTypeDto): { icon: Icon; color: string } {
    switch (type) {
        case NotificationTypeDto.WELCOME:
            return { icon: IconConfetti, color: 'green' };
        case NotificationTypeDto.ANNOUNCEMENT:
            return { icon: IconSpeakerphone, color: 'blue' };
        default:
            return { icon: IconBell, color: 'gray' };
    }
}

export function formatNotificationTime(createdAt: string): string {
    return dayjs(createdAt).fromNow();
}
