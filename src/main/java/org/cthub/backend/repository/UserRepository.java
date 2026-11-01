package org.cthub.backend.repository;

import org.cthub.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("select u.id from User u")
    List<Long> findAllIds();

    /** Records an accepted TOTP step unless it (or a later one) was already used. Returns 1 if accepted. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.totpLastUsedStep = :step where u.id = :id "
        + "and (u.totpLastUsedStep is null or u.totpLastUsedStep < :step)")
    int acceptTotpStep(@Param("id") long id, @Param("step") long step);

    /**
     * Deletes the account in SQL so the database cascades remove every dependent row. A plain
     * {@code delete(user)} fails when the same transaction still holds modified entities that
     * reference the user (e.g. sessions that were just revoked).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from User u where u.id = :id")
    int deleteAccountById(@Param("id") long id);
}