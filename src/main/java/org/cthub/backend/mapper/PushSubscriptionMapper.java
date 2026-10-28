package org.cthub.backend.mapper;

import org.cthub.backend.dto.notification.PushSubscriptionDto;
import org.cthub.backend.dto.notification.PushSubscriptionRequestDto;
import org.cthub.backend.notification.push.PushSubscription;
import org.cthub.backend.notification.push.PushSubscriptionService;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PushSubscriptionMapper {

    PushSubscriptionDto toDto(PushSubscription subscription);

    List<PushSubscriptionDto> toDtos(List<PushSubscription> subscriptions);

    default PushSubscriptionService.Registration toRegistration(PushSubscriptionRequestDto request) {
        return new PushSubscriptionService.Registration(
            request.getEndpoint(),
            request.getP256dh(),
            request.getAuth(),
            Boolean.TRUE.equals(request.getInstalled()),
            request.getPreviousEndpoint(),
            Boolean.TRUE.equals(request.getRenewOnly()));
    }
}
