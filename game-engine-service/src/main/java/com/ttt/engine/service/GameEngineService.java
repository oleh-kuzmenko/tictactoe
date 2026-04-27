package com.ttt.engine.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ttt.engine.exception.GameNotFoundException;
import com.ttt.engine.exception.InvalidMoveException;
import com.ttt.engine.model.Game;
import com.ttt.engine.model.GameStatus;
import com.ttt.engine.model.PlayerSymbol;
import com.ttt.engine.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core game logic service.
 *
 * Concurrency: each game has its own ReentrantLock, stored in a Caffeine cache.
 * Locks are evicted 10 minutes after last access — safely after any game
 * can possibly still be active (max 9 moves at ~500 ms each = ~5 seconds).
 */
@Service
public class GameEngineService {

    private static final int[][] WIN_LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
        {0, 4, 8}, {2, 4, 6}             // diagonals
    };

    private final GameRepository gameRepository;

    /**
     * One lock per active gameId, evicted 10 minutes after last access.
     * expireAfterAccess resets the TTL on every read or write,
     * so an active game is never evicted mid-simulation.
     */
    private final Cache<String, ReentrantLock> gameLocks = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public GameEngineService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

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

    @Transactional(readOnly = true)
    public Game getGame(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
    }

    @Transactional
    public Game makeMove(String gameId, PlayerSymbol symbol, int position) {
        // get() with a loader ensures a lock is always present even if evicted
        // (edge case: game fetched after TTL with status still IN_PROGRESS in DB)
        ReentrantLock lock = gameLocks.get(gameId, k -> new ReentrantLock());
        lock.lock();
        try {
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new GameNotFoundException(gameId));

            validateMoveOrThrow(game, symbol, position);

            char[] board = game.getBoard().toCharArray();
            board[position] = symbol == PlayerSymbol.X ? 'X' : 'O';
            game.setBoard(new String(board));
            game.setMoveCount(game.getMoveCount() + 1);
            game.setUpdatedAt(Instant.now());

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
            throw new InvalidMoveException(
                "Game " + game.getId() + " is already finished: " + game.getStatus());
        }

        if (position < 0 || position > 8) {
            throw new InvalidMoveException("Position must be 0–8, got: " + position);
        }

        char cell = game.getBoard().charAt(position);
        if (cell == 'X' || cell == 'O') {
            throw new InvalidMoveException(
                "Position " + position + " is already occupied by " + cell);
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
