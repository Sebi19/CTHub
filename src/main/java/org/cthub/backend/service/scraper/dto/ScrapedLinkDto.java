package org.cthub.backend.service.scraper.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScrapedLinkDto {
    private String label;
    private String url;
}
