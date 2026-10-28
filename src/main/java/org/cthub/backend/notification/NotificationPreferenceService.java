package org.cthub.backend.notification;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.exception.ApiException;
import org.cthub.backend.notification.model.NotificationChannel;
import org.cthub.backend.notification.model.NotificationPreference;
import org.cthub.backend.notification.model.NotificationType;
import org.cthub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves which optional channels a user wants per notification type. Only explicit choices are
 * stored; anything else falls back to {@link NotificationType#isEnabledByDefault}.
 */
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    /** Keeps the IN-list well below Postgres' bind-parameter limit on broadcasts. */
    private static final int LOOKUP_CHUNK = 1000;

    private final NotificationPreferenceRepository repository;
    private final UserRepository userRepository;

    /** The effective setting for one configurable type/channel combination. */
    public record EffectivePreference(NotificationType type, NotificationChannel channel, boolean enabled) {
    }

    /** Every configurable combination for the user, in enum declaration order. */
    @Transactional(readOnly = true)
    public List<EffectivePreference> list(long userId) {
        Map<NotificationType, Map<NotificationChannel, Boolean>> explicit = new HashMap<>();
        repository.findByUserId(userId).forEach(p ->
            explicit.computeIfAbsent(p.getType(), t -> new HashMap<>()).put(p.getChannel(), p.isEnabled()));

        List<EffectivePreference> result = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                if (!type.supports(channel)) {
                    continue;
                }
                Boolean choice = explicit.getOrDefault(type, Map.of()).get(channel);
                result.add(new EffectivePreference(type, channel, choice != null ? choice : type.isEnabledByDefault(channel)));
            }
        }
        return result;
    }

    @Transactional
    public EffectivePreference update(long userId, NotificationType type, NotificationChannel channel, boolean enabled) {
        if (!type.supports(channel)) {
            throw ApiException.badRequest("UNSUPPORTED_NOTIFICATION_CHANNEL",
                "Notifications of type " + type + " cannot be delivered via " + channel + ".");
        }
        NotificationPreference preference = repository.findByUserIdAndTypeAndChannel(userId, type, channel)
            .orElseGet(() -> NotificationPreference.builder()
                .user(userRepository.getReferenceById(userId))
                .type(type)
                .channel(channel)
                .build());
        preference.setEnabled(enabled);
        preference.setUpdatedAt(Instant.now());
        repository.save(preference);
        return new EffectivePreference(type, channel, enabled);
    }

    /** The subset of {@code userIds} that want {@code type} delivered via {@code channel}. */
    @Transactional(readOnly = true)
    public Set<Long> recipientsFor(NotificationType type, NotificationChannel channel, Collection<Long> userIds) {
        if (!type.supports(channel) || userIds.isEmpty()) {
            return Set.of();
        }
        boolean byDefault = type.isEnabledByDefault(channel);
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(userIds));
        Set<Long> result = byDefault ? new LinkedHashSet<>(ids) : new LinkedHashSet<>();

        for (int i = 0; i < ids.size(); i += LOOKUP_CHUNK) {
            List<Long> chunk = ids.subList(i, Math.min(i + LOOKUP_CHUNK, ids.size()));
            repository.findChoices(type, channel, chunk).forEach(choice -> {
                if (Boolean.TRUE.equals(choice.getEnabled())) {
                    result.add(choice.getUserId());
                } else {
                    result.remove(choice.getUserId());
                }
            });
        }
        return result;
    }
}
