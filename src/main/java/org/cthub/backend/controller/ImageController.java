package org.cthub.backend.controller;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.model.Image;
import org.cthub.backend.repository.ImageRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageRepository imageRepository;

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadImage(@PathVariable UUID id) {

        // 1. Fetch the image from the database
        Image image = imageRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found with id: " + id));

        // 2. Build the "Forever" Cache-Control header
        CacheControl cache = CacheControl
            .maxAge(365, TimeUnit.DAYS)
            .cachePublic()
            .immutable();

        // 3. Return the raw bytes with the exact content type
        return ResponseEntity.ok()
            .cacheControl(cache)
            .contentType(MediaType.parseMediaType(image.getContentType()))
            .body(image.getData());
    }
}