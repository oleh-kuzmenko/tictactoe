package com.ttt.engine.controller;

import java.util.Optional;

import com.ttt.engine.model.Game;
import com.ttt.engine.model.PlayerSymbol;

/**
 * Data Transfer Object for game state responses.
 */
record GameResponseDto(
        String id,
        String board,
        String status,
        String winner,
        int moveCount) {

    static GameResponseDto fromEntity(Game game) {
        return new GameResponseDto(
                game.getId(),
                game.getBoard(),
                game.getStatus().name(),
                Optional.of(game).map(Game::getWinner).map(PlayerSymbol::name).orElse(null),
                game.getMoveCount());
    }
}
