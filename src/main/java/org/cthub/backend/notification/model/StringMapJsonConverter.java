package org.cthub.backend.notification.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * Persists a {@code Map<String, String>} as JSON in a plain {@code text} column (same approach as
 * {@code MailHeadersConverter}; see V9 for why the mail outbox moved away from {@code jsonb}).
 */
@Converter
public class StringMapJsonConverter implements AttributeConverter<Map<String, String>, String> {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<Map<String, String>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return MAPPER.writeValueAsString(attribute);
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Map.of();
        }
        return Map.copyOf(MAPPER.readValue(dbData, TYPE));
    }
}
