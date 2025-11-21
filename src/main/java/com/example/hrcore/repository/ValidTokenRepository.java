package com.example.hrcore.repository;

import com.example.hrcore.entity.ValidToken;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ValidTokenRepository extends JpaRepository<ValidToken, Long> {

    Optional<ValidToken> findByTokenJti(String tokenJti);

    List<ValidToken> findByUserId(UUID userId);

    List<ValidToken> findByUserIdAndExpiresAtAfter(UUID userId, LocalDateTime expiresAt);

    List<ValidToken> findByExpiresAtAfter(LocalDateTime expiresAt);

    @Modifying
    @Query("DELETE FROM ValidToken vt WHERE vt.expiresAt < :now")
    long deleteExpiredTokens(LocalDateTime now);

    void deleteByTokenJti(String tokenJti);

    void deleteByUserId(UUID userId);
}

