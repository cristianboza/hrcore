package com.example.hrcore.repository;

import com.example.hrcore.entity.InvalidToken;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
public interface InvalidTokenRepository extends JpaRepository<InvalidToken, Long> {
    Optional<InvalidToken> findByTokenJti(String tokenJti);
    List<InvalidToken> findByUserId(UUID userId);
    List<InvalidToken> findByExpiresAtBefore(LocalDateTime dateTime);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
