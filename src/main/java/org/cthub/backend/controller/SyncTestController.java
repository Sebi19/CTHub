package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.service.syncer.FlowApiSyncerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/syncer")
public class SyncTestController {

    private final FlowApiSyncerService syncerService;
    @GetMapping("/trigger-sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerFullSync() {
        // Run in background
        syncerService.syncActiveSeason();
        return ResponseEntity.ok("🌙 Full Sync started in background.");
    }
}