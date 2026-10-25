package org.cthub.backend.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.cthub.backend.dto.mail.MailOutboxEntryDto;
import org.cthub.backend.mail.MailService;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailSendResult;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.outbox.MailOutboxEntry;
import org.cthub.backend.mail.outbox.MailOutboxRepository;
import org.cthub.backend.mail.outbox.MailOutboxService;
import org.cthub.backend.mail.outbox.MailOutboxStatus;
import org.cthub.backend.mail.render.WelcomeModel;
import org.cthub.backend.mail.spi.MailDeliveryException;
import org.cthub.backend.mapper.MailOutboxMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

/**
 * Admin-only endpoints for diagnosing the mail outbox: list entries by status, requeue a failed
 * entry, and send a test mail to a given address. {@link org.cthub.backend.mail.outbox.MailOutboxCleanupJob}
 * handles scheduled retention of old {@code SENT} rows; {@code FAILED} rows are kept indefinitely
 * until requeued or manually cleaned up here.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/mail")
public class AdminMailController {

    private final MailOutboxRepository mailOutboxRepository;
    private final MailOutboxService mailOutboxService;
    private final MailOutboxMapper mailOutboxMapper;
    private final MailService mailService;

    // GET /api/admin/mail/outbox?status=FAILED
    @GetMapping("/outbox")
    public ResponseEntity<List<MailOutboxEntryDto>> listOutbox(
        @RequestParam(defaultValue = "FAILED") MailOutboxStatus status) {
        List<MailOutboxEntryDto> entries = mailOutboxRepository.findByStatusOrderByCreatedAtDesc(status)
            .stream()
            .map(mailOutboxMapper::toDto)
            .toList();
        return ResponseEntity.ok(entries);
    }

    // POST /api/admin/mail/outbox/{id}/requeue
    @PostMapping("/outbox/{id}/requeue")
    public ResponseEntity<MailOutboxEntryDto> requeue(@PathVariable Long id) {
        return mailOutboxRepository.findById(id)
            .map(entry -> ResponseEntity.ok(mailOutboxMapper.toDto(mailOutboxService.requeue(entry))))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST /api/admin/mail/test
    @PostMapping("/test")
    public ResponseEntity<?> sendTestMail(@jakarta.validation.Valid @RequestBody TestMailRequestDto request) {
        try {
            MailSendResult result = mailService.sendNow(
                MailTemplate.WELCOME,
                MailAddress.of(request.getEmail()),
                new WelcomeModel("Admin"),
                Locale.GERMAN);
            return ResponseEntity.ok(result);
        } catch (MailDeliveryException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(e.getMessage());
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestMailRequestDto {
        @NotBlank
        @Email
        private String email;
    }
}
