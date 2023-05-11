package com.josill.jwtlogin.confirmation_token;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {

    Optional<ConfirmationToken> findByToken(String token);
    @Transactional // if the update fails the database will be restored to the last state
    @Modifying // method modifies the database, required for all update and delete operations
    @Query("UPDATE ConfirmationToken c " +
            "SET c.confirmedAt = ?2 " +
            "WHERE c.token = ?1") // ?1 = token and ?2 = confirmedAt
    int updateConfirmedAt(String token, LocalDateTime confirmedAt);
}
