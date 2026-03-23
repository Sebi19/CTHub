package org.cthub.backend.repository;

import org.cthub.backend.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {
    Optional<Image> findById(UUID id);
}
