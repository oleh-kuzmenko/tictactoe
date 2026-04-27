package com.ttt.session.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a game session.
 * The sessionId is also used as the gameId in the Game Engine Service.
 */
@Entity
@Getter
@Setter
@Table(name = "sessions")
public class GameSession {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.CREATED;

    /**
     * Symbol of the winner once the game is decided.
     */
    @Enumerated(EnumType.STRING)
    private PlayerSymbol winner;

    /**
     * Ordered list of moves made during simulation.
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("moveNumber ASC")
    private List<MoveRecord> moves = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant updatedAt;

}
