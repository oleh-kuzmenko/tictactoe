package com.ttt.session.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ttt.session.model.GameSession;
import com.ttt.session.service.GameSessionService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for the Game Session Service.
 *
 * POST /sessions → create session (201 Created)
 * POST /sessions/{sessionId}/simulate → start simulation (202 Accepted)
 * GET /sessions/{sessionId} → get session details + move history
 */
@Slf4j
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class GameSessionController {

    private final GameSessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameSession createSession(HttpServletResponse response) {
        log.info("Creating new game session");
        GameSession session = sessionService.createSession();
        response.setHeader("Location", "/api/session/" + session.getId());
        return session;
    }

    @PostMapping("/{sessionId}/simulate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void simulate(@PathVariable String sessionId) {
        log.info("Starting simulation for session: {}", sessionId);
        sessionService.startSimulation(sessionId);
    }

    @GetMapping("/{sessionId}")
    public GameSession getSession(@PathVariable String sessionId) {
        log.info("Fetching session details for session: {}", sessionId);
        return sessionService.getSession(sessionId);
    }

}
