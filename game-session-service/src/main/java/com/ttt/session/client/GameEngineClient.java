package com.ttt.session.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "game-engine-client", url = "${engine.base-url}")
public interface GameEngineClient {

    @PostMapping("/games/{gameId}")
    GameStateDto createGame(@PathVariable("gameId") String gameId);

    @GetMapping("/games/{gameId}")
    GameStateDto getGame(@PathVariable("gameId") String gameId);

    @PostMapping("/games/{gameId}/move")
    GameStateDto makeMove(@PathVariable("gameId") String gameId, @RequestBody MoveRequestDto request);

}
