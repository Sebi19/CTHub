package org.cthub.backend.auth.webauthn;

import jakarta.persistence.*;
import lombok.*;
import org.cthub.backend.model.User;

import java.time.Instant;

/** One registered passkey (synced or device-bound). A user may have any number of them. */
@Entity
@Table(name = "webauthn_credential")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebAuthnCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webauthn_credential_seq_gen")
    @SequenceGenerator(name = "webauthn_credential_seq_gen", sequenceName = "webauthn_credential_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** base64url, as reported by the authenticator. */
    @Column(name = "credential_id", nullable = false, unique = true, length = 1366)
    private String credentialId;

    /** Serialized {@code AttestedCredentialData} (AAGUID, credential id, COSE public key). */
    @Column(name = "attested_credential_data", nullable = false)
    private byte[] attestedCredentialData;

    @Column(name = "sign_count", nullable = false)
    private long signCount;

    /** Comma-separated authenticator transports (usb, nfc, ble, internal, hybrid). */
    @Column(name = "transports")
    private String transports;

    @Column(name = "backup_eligible", nullable = false)
    private boolean backupEligible;

    @Column(name = "backup_state", nullable = false)
    private boolean backupState;

    @Column(name = "uv_initialized", nullable = false)
    private boolean uvInitialized;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
