package com.example.hrcore.repository;

import com.example.hrcore.entity.ValidToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ValidTokenRepository extends JpaRepository<ValidToken, Long> {

    Optional<ValidToken> findByTokenJti(String tokenJti);

    List<ValidToken> findByUserId(Long userId);

    List<ValidToken> findByUserIdAndExpiresAtAfter(Long userId, LocalDateTime expiresAt);

    List<ValidToken> findByExpiresAtAfter(LocalDateTime expiresAt);

    @Modifying
    @Query("DELETE FROM ValidToken vt WHERE vt.expiresAt < :now")
    long deleteExpiredTokens(LocalDateTime now);

    void deleteByTokenJti(String tokenJti);

    void deleteByUserId(Long userId);
}

