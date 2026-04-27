package com.ttt.session.controller;

import com.ttt.session.model.GameSession;
import com.ttt.session.model.MoveRecord;
import com.ttt.session.model.PlayerSymbol;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data Transfer Object for game session responses.
 */
record GameSessionResponseDto(
        String id,
        String status,
        String winner,
        List<MoveRecordDto> moves,
        Instant createdAt,
        Instant updatedAt) {

    static GameSessionResponseDto fromEntity(GameSession session) {
        return new GameSessionResponseDto(
                session.getId(),
                session.getStatus().name(),
                Optional.ofNullable(session.getWinner()).map(PlayerSymbol::name).orElse(null),
                session.getMoves().stream().map(MoveRecordDto::fromEntity).toList(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    /**
     * Nested DTO for individual move records within a session.
     */
    record MoveRecordDto(
            int moveNumber,
            String symbol,
            int position,
            String boardSnapshot,
            String status) {

        static MoveRecordDto fromEntity(MoveRecord move) {
            return new MoveRecordDto(
                    move.getMoveNumber(),
                    move.getSymbol().name(),
                    move.getPosition(),
                    move.getBoardSnapshot(),
                    move.getResultStatus().name());
        }
    }
}
