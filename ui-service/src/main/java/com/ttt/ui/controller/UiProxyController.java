package com.ttt.ui.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.ttt.common.interceptor.TraceIdRestClientInterceptor;

/**
 * Thin proxy controller so the browser only talks to one origin (UI Service).
 * Forwards session creation and simulation triggers to the Session Service.
 */
@RestController
@RequestMapping("/api")
public class UiProxyController {

    private final RestClient restClient;
    private final String sessionBaseUrl;

    public UiProxyController(@Value("${session.base-url}") String sessionBaseUrl) {
        this.restClient = RestClient.builder()
                .requestInterceptor(new TraceIdRestClientInterceptor())
                .build();
        this.sessionBaseUrl = sessionBaseUrl;
    }

    /**
     * Create a new game session.
     */
    @PostMapping("/sessions")
    public String createSession() {
        return restClient.post().uri(sessionBaseUrl + "/sessions").retrieve().body(String.class);
    }

    /**
     * Start simulation for the given session.
     */
    @PostMapping("/sessions/{sessionId}/simulate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void simulate(@PathVariable String sessionId) {
        restClient.post().uri(sessionBaseUrl + "/sessions/" + sessionId + "/simulate").retrieve();
    }

    /**
     * Get session details (move history, status).
     */
    @GetMapping("/sessions/{sessionId}")
    public String getSession(@PathVariable String sessionId) {
        return restClient.get().uri(sessionBaseUrl + "/sessions/" + sessionId).retrieve().body(String.class);
    }
}
