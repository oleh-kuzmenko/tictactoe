package com.ttt.engine.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttt.engine.exception.GameNotFoundException;
import com.ttt.engine.exception.InvalidMoveException;
import com.ttt.engine.model.Game;
import com.ttt.engine.model.GameStatus;
import com.ttt.engine.model.PlayerSymbol;
import com.ttt.engine.repository.GameRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core game logic service.
 *
 * Concurrency: each game has its own ReentrantLock, stored in a ConcurrentHashMap.
 * This prevents two simultaneous move requests for the same game from corrupting state,
 * while still allowing full parallelism across different games.
 */
@Service
public class GameEngineService {

    /**
     * Win condition patterns (indices into the 9-char board string).
     * */
    private static final int[][] WIN_LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
        {0, 4, 8}, {2, 4, 6}             // diagonals
    };

    private final GameRepository gameRepository;

    /**
     * One lock per active gameId to serialise concurrent moves on the same game.
     * */
    private final Map<String, ReentrantLock> gameLocks = new ConcurrentHashMap<>();

    public GameEngineService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Creates a new game with the given id.
     * The id is typically the sessionId provided by the Game Session Service.
     */
    @Transactional
    public Game createGame(String gameId) {
        if (gameId == null || gameId.isBlank()) {
            gameId = UUID.randomUUID().toString();
        }

        if (gameRepository.existsById(gameId)) {
            return gameRepository.findById(gameId).orElseThrow();
        }

        Game game = new Game();
        game.setId(gameId);
        gameLocks.put(gameId, new ReentrantLock());
        
        return gameRepository.save(game);
    }

    /**
     * Retrieves the current game state.
     */
    @Transactional(readOnly = true)
    public Game getGame(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
    }

    /**
     * Applies a move to the game.
     *
     * @param gameId  the game identifier
     * @param symbol  the player making the move (X or O)
     * @param position board position 0–8 (row-major)
     * @return the updated Game state
     */
    @Transactional
    public Game makeMove(String gameId, PlayerSymbol symbol, int position) {
        ReentrantLock lock = gameLocks.computeIfAbsent(gameId, k -> new ReentrantLock());
        lock.lock();
        try {
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new GameNotFoundException(gameId));

            validateMoveOrThrow(game, symbol, position);

            // Apply the move
            char[] board = game.getBoard().toCharArray();
            board[position] = symbol == PlayerSymbol.X ? 'X' : 'O';
            game.setBoard(new String(board));
            game.setMoveCount(game.getMoveCount() + 1);
            game.setUpdatedAt(Instant.now());

            // Evaluate result
            if (checkWin(board, symbol == PlayerSymbol.X ? 'X' : 'O')) {
                game.setStatus(GameStatus.WIN);
                game.setWinner(symbol);
            } else if (game.getMoveCount() == 9) {
                game.setStatus(GameStatus.DRAW);
            }

            return gameRepository.save(game);
        } finally {
            lock.unlock();
        }
    }

    private void validateMoveOrThrow(Game game, PlayerSymbol symbol, int position) {
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new InvalidMoveException("Game " + game.getId() + " is already finished: " + game.getStatus());
        }

        if (position < 0 || position > 8) {
            throw new InvalidMoveException("Position must be 0–8, got: " + position);
        }

        char cell = game.getBoard().charAt(position);
        if (cell == 'X' || cell == 'O') {
            throw new InvalidMoveException("Position " + position + " is already occupied by " + cell);
        }
    }

    private boolean checkWin(char[] board, char symbol) {
        for (int[] line : WIN_LINES) {
            if (board[line[0]] == symbol &&
                board[line[1]] == symbol &&
                board[line[2]] == symbol) {
                return true;
            }
        }
        return false;
    }
}
