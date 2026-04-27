package com.ttt.session.client;

import com.ttt.session.model.GameStatus;
import com.ttt.session.model.PlayerSymbol;

/**
 * DTO mirroring the Game Engine Service's Game response body.
 * Kept intentionally minimal — we only need what the Session Service cares about.
 */
public record GameStateDto(
        String id,
        String board,
        GameStatus status,
        PlayerSymbol winner,
        int moveCount) {
}
