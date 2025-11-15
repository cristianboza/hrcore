package com.example.hrcore.repository;

import com.example.hrcore.entity.InvalidToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvalidTokenRepository extends JpaRepository<InvalidToken, Long> {
    Optional<InvalidToken> findByTokenJti(String tokenJti);
    List<InvalidToken> findByUserId(Long userId);
    List<InvalidToken> findByExpiresAtBefore(LocalDateTime dateTime);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
