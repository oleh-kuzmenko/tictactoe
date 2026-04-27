package com.ttt.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.ttt.engine.model.GameStatus;
import com.ttt.engine.model.PlayerSymbol;
import com.ttt.engine.service.GameEngineService;

@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameEngineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameEngineService gameEngineService;

    @Test
    void createGame_shouldReturnCreatedGame() throws Exception {
        mockMvc.perform(post("/games/test-game-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-game-1"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.board").value("         "));
    }

    @Test
    void getGame_shouldReturn404ForUnknownGame() throws Exception {
        mockMvc.perform(get("/games/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void makeMove_shouldUpdateBoard() throws Exception {
        gameEngineService.createGame("game-move-test");

        mockMvc.perform(post("/games/game-move-test/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"X","position":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").value("X        "))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void makeMove_shouldRejectOccupiedCell() throws Exception {
        gameEngineService.createGame("game-occupied");
        gameEngineService.makeMove("game-occupied", PlayerSymbol.X, 4);

        mockMvc.perform(post("/games/game-occupied/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"O","position":4}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void makeMove_shouldDetectWin() {
        gameEngineService.createGame("game-win");
        // X wins top row: positions 0, 1, 2
        gameEngineService.makeMove("game-win", PlayerSymbol.X, 0);
        gameEngineService.makeMove("game-win", PlayerSymbol.O, 3);
        gameEngineService.makeMove("game-win", PlayerSymbol.X, 1);
        gameEngineService.makeMove("game-win", PlayerSymbol.O, 4);
        var result = gameEngineService.makeMove("game-win", PlayerSymbol.X, 2);

        assertThat(result.getStatus()).isEqualTo(GameStatus.WIN);
        assertThat(result.getWinner()).isEqualTo(PlayerSymbol.X);
    }

    @Test
    void makeMove_shouldDetectDraw() {
        gameEngineService.createGame("game-draw");
        // Force a draw: X O X / O X O / O X O — note centre stays X
        // Positions that result in draw: X:0, O:1, X:2, O:3, X:4, O:6, X:5, O:8, X:7
        int[] moves = {0, 1, 2, 3, 4, 6, 5, 8, 7};
        PlayerSymbol[] players = {
            PlayerSymbol.X, PlayerSymbol.O, PlayerSymbol.X,
            PlayerSymbol.O, PlayerSymbol.X, PlayerSymbol.O,
            PlayerSymbol.X, PlayerSymbol.O, PlayerSymbol.X
        };
        for (int i = 0; i < 9; i++) {
            gameEngineService.makeMove("game-draw", players[i], moves[i]);
        }
        var finalState = gameEngineService.getGame("game-draw");
        assertThat(finalState.getStatus()).isEqualTo(GameStatus.DRAW);
    }

    @Test
    void moveAfterGameOver_shouldBeRejected() {
        gameEngineService.createGame("game-over");
        gameEngineService.makeMove("game-over", PlayerSymbol.X, 0);
        gameEngineService.makeMove("game-over", PlayerSymbol.O, 3);
        gameEngineService.makeMove("game-over", PlayerSymbol.X, 1);
        gameEngineService.makeMove("game-over", PlayerSymbol.O, 4);
        gameEngineService.makeMove("game-over", PlayerSymbol.X, 2); // X wins

        org.junit.jupiter.api.Assertions.assertThrows(
                com.ttt.engine.exception.InvalidMoveException.class,
                () -> gameEngineService.makeMove("game-over", PlayerSymbol.O, 5)
        );
    }
}
