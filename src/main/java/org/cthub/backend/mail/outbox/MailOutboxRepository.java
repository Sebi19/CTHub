package org.cthub.backend.mail.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MailOutboxRepository extends JpaRepository<MailOutboxEntry, Long> {

    Optional<MailOutboxEntry> findByDedupeKey(String dedupeKey);

    /**
     * Claims a batch of due entries for sending. {@code SKIP LOCKED} makes this safe even if the app
     * ever scales past one instance - concurrent dispatchers simply grab different rows. The lock is
     * expressed directly in the native SQL (not via {@code @Lock}) because Hibernate rejects
     * {@code setLockMode} on plain native queries.
     */
    @Query(value = """
        select * from mail_outbox
        where status = 'PENDING'
          and next_attempt_at <= :now
        order by next_attempt_at
        limit :batchSize
        for update skip locked
        """, nativeQuery = true)
    List<MailOutboxEntry> claimDueBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);

    List<MailOutboxEntry> findByStatusOrderByCreatedAtDesc(MailOutboxStatus status);

    long countByStatus(MailOutboxStatus status);

    long deleteByStatusAndSentAtBefore(MailOutboxStatus status, Instant cutoff);
}
