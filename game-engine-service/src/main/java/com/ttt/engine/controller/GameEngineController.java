package com.ttt.engine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ttt.engine.model.Game;
import com.ttt.engine.model.PlayerSymbol;
import com.ttt.engine.service.GameEngineService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for the Game Engine Service.
 *
 * POST /games/{gameId}/move — make a move
 * POST /games/{gameId} — create a new game
 * GET /games/{gameId} — retrieve current game state
 */
@Slf4j
@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameEngineController {

    private final GameEngineService gameEngineService;

    /**
     * Create (or fetch existing) game for the given id.
     */
    @PostMapping("/{gameId}")
    public GameResponseDto createGame(@PathVariable String gameId) {
        log.info("Creating or fetching game with id: {}", gameId);
        return GameResponseDto.fromEntity(gameEngineService.createGame(gameId));
    }

    /**
     * Retrieve the current game state.
     */
    @GetMapping("/{gameId}")
    public GameResponseDto getGame(@PathVariable String gameId) {
        log.info("Fetching game state for game: {}", gameId);
        return GameResponseDto.fromEntity(gameEngineService.getGame(gameId));
    }

    /**
     * Apply a move to the game.
     */
    @PostMapping("/{gameId}/move")
    public GameResponseDto makeMove(@PathVariable String gameId,@RequestBody @Valid MoveRequestDto request) {
        log.info("Making move for game: {}", gameId);
        Game updated = gameEngineService.makeMove(gameId, PlayerSymbol.valueOf(request.symbol()), request.position());
        return GameResponseDto.fromEntity(updated);
    }
}
