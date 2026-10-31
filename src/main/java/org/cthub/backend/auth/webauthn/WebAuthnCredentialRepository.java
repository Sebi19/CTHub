package org.cthub.backend.auth.webauthn;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {

    boolean existsByUserId(Long userId);

    Optional<WebAuthnCredential> findByCredentialId(String credentialId);

    List<WebAuthnCredential> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<WebAuthnCredential> findByIdAndUserId(Long id, Long userId);
}
