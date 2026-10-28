package org.cthub.backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnnouncementRequestDto {
    @NotBlank
    @Size(max = 200)
    private String title;
    @NotBlank
    @Size(max = 2000)
    private String body;
    /** Optional app-relative path, e.g. {@code /competitions/2026-27}. */
    @Size(max = 2048)
    private String link;
    /** Single recipient; {@code null} sends to every user. */
    private Long userId;
}
