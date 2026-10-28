package org.cthub.backend.notification.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StringMapJsonConverterTest {

    private final StringMapJsonConverter converter = new StringMapJsonConverter();

    @Test
    void roundTripsParamsIncludingSpecialCharacters() {
        Map<String, String> params = Map.of("team", "Robo \"Füchse\" & Co.", "season", "2025-26");

        String json = converter.convertToDatabaseColumn(params);

        assertThat(converter.convertToEntityAttribute(json)).isEqualTo(params);
    }

    @Test
    void storesEmptyParamsAsNullAndReadsNullAsEmpty() {
        assertThat(converter.convertToDatabaseColumn(Map.of())).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute(" ")).isEmpty();
    }
}
