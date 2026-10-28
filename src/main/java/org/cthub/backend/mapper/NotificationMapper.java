package org.cthub.backend.mapper;

import org.cthub.backend.dto.notification.NotificationChannelDto;
import org.cthub.backend.dto.notification.NotificationDto;
import org.cthub.backend.dto.notification.NotificationPageDto;
import org.cthub.backend.dto.notification.NotificationPreferenceDto;
import org.cthub.backend.dto.notification.NotificationTypeDto;
import org.cthub.backend.notification.NotificationPage;
import org.cthub.backend.notification.NotificationPreferenceService;
import org.cthub.backend.notification.event.NotificationsCreatedEvent;
import org.cthub.backend.notification.model.Notification;
import org.cthub.backend.notification.model.NotificationChannel;
import org.cthub.backend.notification.model.NotificationType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDto toDto(Notification notification);

    List<NotificationDto> toDtos(List<Notification> notifications);

    /** Freshly created notifications are by definition unread. */
    @Mapping(target = "readAt", ignore = true)
    NotificationDto toDto(NotificationsCreatedEvent.Item item);

    NotificationPageDto toPageDto(NotificationPage page);

    NotificationPreferenceDto toDto(NotificationPreferenceService.EffectivePreference preference);

    List<NotificationPreferenceDto> toPreferenceDtos(List<NotificationPreferenceService.EffectivePreference> preferences);

    NotificationType toDomain(NotificationTypeDto type);

    NotificationChannel toDomain(NotificationChannelDto channel);
}
