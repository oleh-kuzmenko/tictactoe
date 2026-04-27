package com.ttt.session.service;

import com.ttt.session.client.GameEngineClient;
import com.ttt.session.client.GameStateDto;
import com.ttt.session.client.MoveRequestDto;
import com.ttt.session.model.GameSession;
import com.ttt.session.model.GameStatus;
import com.ttt.session.model.GameUpdateMessage;
import com.ttt.session.model.MoveRecord;
import com.ttt.session.model.PlayerSymbol;
import com.ttt.session.model.SessionStatus;
import com.ttt.session.repository.GameSessionRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class GameSimulationRunner {

    private final GameSessionRepository sessionRepository;
    private final GameEngineClient engineClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final long moveDelayMs;

    public GameSimulationRunner(GameSessionRepository sessionRepository,
            GameEngineClient engineClient,
            SimpMessagingTemplate messagingTemplate,
            @Value("${simulation.move-delay-ms:500}") long moveDelayMs) {
        this.sessionRepository = sessionRepository;
        this.engineClient = engineClient;
        this.messagingTemplate = messagingTemplate;
        this.moveDelayMs = moveDelayMs;
    }

    /**
     * The actual simulation loop — runs asynchronously on "simulationExecutor".
     *
     * Algorithm:
     * 1. Shuffle all 9 positions as a "move deck".
     * 2. Deal positions alternately to X and O.
     * 3. After each move, check the Engine's response status.
     * 4. Stop on WIN or DRAW; push a WebSocket update after each move.
     *
     * Using a shuffled deck (rather than random choice from remaining positions)
     * avoids the birthday-problem bias and guarantees max 9 iterations.
     */
    @Async("simulationExecutor")
    public void run(String sessionId) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow();
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setUpdatedAt(Instant.now());
        session = sessionRepository.save(session);

        List<Integer> positions = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
        Collections.shuffle(positions);

        PlayerSymbol currentPlayer = PlayerSymbol.X;
        int moveNumber = 1;

        try {
            for (int position : positions) {
                applyDelay();

                GameStateDto state = engineClient.makeMove(sessionId, new MoveRequestDto(currentPlayer.name(), position));

                saveMoveRecord(session, moveNumber, currentPlayer, position, state);
                broadcastUpdate(sessionId, moveNumber, currentPlayer, position, state);

                if (state.status() != GameStatus.IN_PROGRESS) {
                    finaliseSession(session, state);
                    return;
                }

                currentPlayer = currentPlayer.opponent();
                moveNumber++;
            }

            log.warn("Simulation reached end of board without engine terminal state. Finalising.");
            forceComplete(session);

        } catch (Exception e) {
            log.error("Simulation FATAL error for session {}: {}", sessionId, e.getMessage(), e);
            forceComplete(session);
        }
    }

    private void forceComplete(GameSession session) {
        session.setStatus(SessionStatus.COMPLETED);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
    }

    private void applyDelay() {
        if (moveDelayMs > 0) {
            try {
                Thread.sleep(moveDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void saveMoveRecord(GameSession session, int moveNum, PlayerSymbol symbol, int pos, GameStateDto state) {
        MoveRecord record = new MoveRecord();
        record.setSession(session);
        record.setMoveNumber(moveNum);
        record.setSymbol(symbol);
        record.setPosition(pos);
        record.setBoardSnapshot(state.board());
        record.setResultStatus(state.status());
        session.getMoves().add(record);
        sessionRepository.save(session);
    }

    private void broadcastUpdate(String sessionId, int moveNum, PlayerSymbol symbol, int pos, GameStateDto state) {
        String winnerStr = state.winner() != null ? state.winner().name() : null;
        messagingTemplate.convertAndSend("/topic/game/" + sessionId,
                new GameUpdateMessage(sessionId, moveNum, symbol.name(), pos,
                        state.board(), state.status().name(), winnerStr));
    }

    private void finaliseSession(GameSession session, GameStateDto finalState) {
        session.setStatus(SessionStatus.COMPLETED);
        session.setUpdatedAt(Instant.now());
        if (finalState.winner() != null) {
            session.setWinner(PlayerSymbol.valueOf(finalState.winner().name()));
        }
        sessionRepository.save(session);
    }
}
