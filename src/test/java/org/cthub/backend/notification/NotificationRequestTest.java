package org.cthub.backend.notification;

import org.cthub.backend.notification.model.NotificationType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationRequestTest {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/competitions", "/competition/2025-26/berlin?tab=awards"})
    void acceptsInAppPaths(String link) {
        assertThat(NotificationRequest.builder().type(NotificationType.ANNOUNCEMENT).link(link).build().link())
            .isEqualTo(link);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example", "//evil.example", "/\\evil.example", "javascript:alert(1)", "competitions"})
    void rejectsLinksLeavingTheApp(String link) {
        assertThatThrownBy(() -> NotificationRequest.builder().type(NotificationType.ANNOUNCEMENT).link(link).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresType() {
        assertThatThrownBy(() -> NotificationRequest.builder().build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void copiesParamsDefensively() {
        Map<String, String> params = new HashMap<>(Map.of("a", "1"));
        NotificationRequest request = NotificationRequest.builder().type(NotificationType.WELCOME).params(params).build();
        params.put("b", "2");

        assertThat(request.params()).containsOnlyKeys("a");
        assertThat(NotificationRequest.of(NotificationType.WELCOME).params()).isEmpty();
    }
}
