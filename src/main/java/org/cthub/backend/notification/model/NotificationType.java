package org.cthub.backend.notification.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Every kind of notification the app can send. Stored by name in {@code notification.type}, so a
 * value must never be renamed or removed while rows still reference it - loading such a row would
 * fail. Rendering lives with each channel (frontend i18n keys {@code notifications.types.<TYPE>}).
 *
 * <p>Each type declares the optional {@link NotificationChannel}s a user may switch on for it.
 * Defaults are in {@link #isEnabledByDefault}.
 */
public enum NotificationType {
    /**
     * Sent once the user has completed their profile. No e-mail: a transactional welcome mail already
     * goes out. No push: no device can be registered before the profile is complete.
     */
    WELCOME(EnumSet.noneOf(NotificationChannel.class)),
    /** Free-form announcement sent by an admin to one or all users. */
    ANNOUNCEMENT(EnumSet.of(NotificationChannel.EMAIL, NotificationChannel.PUSH));

    private final Set<NotificationChannel> configurableChannels;

    NotificationType(Set<NotificationChannel> configurableChannels) {
        this.configurableChannels = Set.copyOf(configurableChannels);
    }

    public Set<NotificationChannel> configurableChannels() {
        return configurableChannels;
    }

    public boolean supports(NotificationChannel channel) {
        return configurableChannels.contains(channel);
    }

    /**
     * Applies when the user has made no explicit choice. E-mail is opt-in. Push is on: registering a
     * device (which needs an explicit browser permission) already is the opt-in.
     */
    public boolean isEnabledByDefault(NotificationChannel channel) {
        return channel == NotificationChannel.PUSH;
    }
}
