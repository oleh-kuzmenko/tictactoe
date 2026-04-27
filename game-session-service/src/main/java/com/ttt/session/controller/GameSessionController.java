package com.ttt.session.controller;

import com.ttt.session.service.GameSessionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class GameSessionController {

    private final GameSessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameSessionResponseDto createSession(HttpServletResponse response) {
        log.info("Creating new game session");
        GameSessionResponseDto session = GameSessionResponseDto.fromEntity(sessionService.createSession());
        response.setHeader("Location", "/api/session/" + session.id());
        return session;
    }

    @PostMapping("/{sessionId}/simulate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void simulate(@PathVariable String sessionId) {
        log.info("Starting simulation for session: {}", sessionId);
        sessionService.startSimulation(sessionId);
    }

    @GetMapping("/{sessionId}")
    public GameSessionResponseDto getSession(@PathVariable String sessionId) {
        log.info("Fetching session details for session: {}", sessionId);
        return GameSessionResponseDto.fromEntity(sessionService.getSession(sessionId));
    }
}
