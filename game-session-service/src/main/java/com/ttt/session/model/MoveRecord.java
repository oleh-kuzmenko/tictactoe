package com.ttt.session.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Persisted record of a single move within a session.
 */
@Entity
@Getter
@Setter
@Table(name = "move_records")
public class MoveRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @Column(nullable = false)
    private int moveNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerSymbol symbol;

    @Column(nullable = false)
    private int position;

    /**
     * Board state AFTER this move was applied (9-char snapshot).
     */
    @Column(length = 9, nullable = false)
    private String boardSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus resultStatus;

}
