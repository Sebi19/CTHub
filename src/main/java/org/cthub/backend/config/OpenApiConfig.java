package org.cthub.backend.config;

import org.springframework.context.annotation.Configuration;
import io.swagger.v3.core.jackson.ModelResolver;
import jakarta.annotation.PostConstruct;

@Configuration
public class OpenApiConfig {

    @PostConstruct
    public void init() {
        // This globally forces all enums to be generated as $ref components
        ModelResolver.enumsAsRef = true;
    }
}