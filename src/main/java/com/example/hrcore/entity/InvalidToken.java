package com.example.hrcore.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import java.util.UUID;
import lombok.Builder;
import java.util.UUID;
import lombok.Data;
import java.util.UUID;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invalid_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvalidToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenJti;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime invalidatedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
