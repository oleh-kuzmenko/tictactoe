package com.ttt.session.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttt.session.client.GameEngineClient;
import com.ttt.session.exception.SessionNotFoundException;
import com.ttt.session.model.GameSession;
import com.ttt.session.model.SessionStatus;
import com.ttt.session.repository.GameSessionRepository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Orchestrates game sessions and drives the automated simulation.
 */
@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final GameSessionRepository sessionRepository;
    private final GameEngineClient engineClient;
    private final GameSimulationRunner simulationRunner;

    /**
     * Creates a new session and initialises the corresponding game on the Engine.
     */
    @Transactional
    public GameSession createSession() {
        String sessionId = UUID.randomUUID().toString();
        engineClient.createGame(sessionId);

        GameSession session = new GameSession();
        session.setId(sessionId);
        session.setStatus(SessionStatus.CREATED);
        return sessionRepository.save(session);
    }

    /**
     * Returns the session with its full move history.
     */
    @Transactional(readOnly = true)
    public GameSession getSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    /**
     * Triggers the async simulation for the given session.
     *
     * @throws IllegalStateException if the session has already been simulated
     */
    public void startSimulation(String sessionId) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (session.getStatus() != SessionStatus.CREATED) {
            throw new IllegalStateException("Simulation cannot be started in status: " + session.getStatus());
        }

        simulationRunner.run(sessionId);
    }

}
