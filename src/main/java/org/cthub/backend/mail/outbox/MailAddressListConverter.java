package org.cthub.backend.mail.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.cthub.backend.mail.model.MailAddress;

import java.util.List;

/**
 * Persists a list of {@link MailAddress} (used for cc/bcc) as a JSON string in a single text column.
 */
@Converter
public class MailAddressListConverter implements AttributeConverter<List<MailAddress>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<MailAddress> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize mail address list", e);
        }
    }

    @Override
    public List<MailAddress> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<MailAddress>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialize mail address list", e);
        }
    }
}
