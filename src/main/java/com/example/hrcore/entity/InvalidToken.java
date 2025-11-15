package com.example.hrcore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime invalidatedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
