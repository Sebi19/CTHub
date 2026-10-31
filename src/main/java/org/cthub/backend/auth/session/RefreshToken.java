package org.cthub.backend.auth.session;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One link in a session's refresh-token rotation chain. Only the hash of the token is stored.
 * {@code rotatedAt} is set exactly once, by the request that also inserts the single successor.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_seq_gen")
    @SequenceGenerator(name = "refresh_token_seq_gen", sequenceName = "refresh_token_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AuthSession session;

    @Column(name = "parent_id", unique = true)
    private Long parentId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "rotated_at")
    private Instant rotatedAt;
}
