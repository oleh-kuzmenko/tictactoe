package com.ttt.engine.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents one Tic Tac Toe game.
 *
 * The board is stored as a 9-char string (positions 0–8, row-major):
 * 'X', 'O', or ' ' (empty).
 *
 * Example for a board where X played 0, O played 4:
 * "X O "
 */
@Entity
@Getter
@Setter
@Table(name = "games")
public class Game {

    @Id
    private String id;

    /**
     * Compact 9-char board representation. Initialized to 9 spaces.
     */
    @Column(length = 9, nullable = false)
    private String board = "         ";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.IN_PROGRESS;

    /**
     * Symbol of the winner ('X' or 'O'), null if game is ongoing or draw.
     */
    @Enumerated(EnumType.STRING)
    private PlayerSymbol winner;

    @Column(nullable = false)
    private int moveCount = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant updatedAt;

    /**
     * Returns the board as a 3×3 char array for easy processing.
     */
    public char[][] getBoardGrid() {
        char[][] grid = new char[3][3];
        for (int i = 0; i < 9; i++) {
            grid[i / 3][i % 3] = board.charAt(i);
        }
        return grid;
    }
}
