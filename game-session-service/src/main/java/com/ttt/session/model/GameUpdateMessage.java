package com.ttt.session.model;

/**
 * Payload pushed over WebSocket/STOMP to /topic/game/{sessionId} after each
 * move.
 * The UI uses this to update the board without polling.
 */
public record GameUpdateMessage(
                String sessionId,
                int moveNumber,
                String symbol,
                int position,
                String board,
                String status,
                String winner) {
}
