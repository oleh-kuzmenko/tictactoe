package com.ttt.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.ttt.session.client.GameEngineClient;
import com.ttt.session.client.GameStateDto;
import com.ttt.session.client.MoveRequestDto;
import com.ttt.session.model.GameStatus;
import com.ttt.session.model.PlayerSymbol;
import com.ttt.session.model.SessionStatus;
import com.ttt.session.service.GameSessionService;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "simulation.move-delay-ms=1"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameSessionService sessionService;

    @MockBean
    private GameEngineClient engineClient;

    @Test
    void createSession_shouldReturn201WithSessionId() throws Exception {
        when(engineClient.createGame(anyString())).thenReturn(emptyBoard("any-id"));

        mockMvc.perform(post("/sessions"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getSession_unknownId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/sessions/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void simulate_shouldReturn202AndCompleteAsynchronously() throws Exception {
        when(engineClient.createGame(anyString())).thenReturn(emptyBoard("any-id"));

        String response = mockMvc.perform(post("/sessions"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String sessionId = JsonPath.read(response, "$.id");
        setupEngineForXWin(sessionId);

        mockMvc.perform(post("/sessions/" + sessionId + "/simulate"))
                .andExpect(status().isAccepted());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var session = sessionService.getSession(sessionId);
            assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        });
    }

    @Test
    void simulate_calledTwice_shouldReturn409() throws Exception {
        when(engineClient.createGame(anyString())).thenReturn(emptyBoard("any-id"));
        when(engineClient.makeMove(anyString(), any(MoveRequestDto.class)))
                .thenReturn(new GameStateDto("x", "         ",
                        GameStatus.IN_PROGRESS, null, 1));

        String response = mockMvc.perform(post("/sessions"))
                .andReturn().getResponse().getContentAsString();
        String sessionId = JsonPath.read(response, "$.id");

        mockMvc.perform(post("/sessions/" + sessionId + "/simulate"))
                .andExpect(status().isAccepted());

        await().atMost(1, TimeUnit.SECONDS).until(() -> {
            var s = sessionService.getSession(sessionId);
            return s.getStatus() != SessionStatus.CREATED;
        });

        mockMvc.perform(post("/sessions/" + sessionId + "/simulate"))
                .andExpect(status().isConflict());
    }

    @Test
    void getSession_afterSimulation_shouldIncludeMoveHistory() throws Exception {
        when(engineClient.createGame(anyString())).thenReturn(emptyBoard("any-id"));

        String response = mockMvc.perform(post("/sessions"))
                .andReturn().getResponse().getContentAsString();
        String sessionId = JsonPath.read(response, "$.id");
        setupEngineForXWin(sessionId);

        mockMvc.perform(post("/sessions/" + sessionId + "/simulate"))
                .andExpect(status().isAccepted());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var session = sessionService.getSession(sessionId);
            assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
            assertThat(session.getMoves()).isNotEmpty();
            assertThat(session.getMoves().size()).isGreaterThan(0);
        });

        mockMvc.perform(get("/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.moves").isArray())
                .andExpect(jsonPath("$.moves.length()").value(greaterThan(0)));
    }

    private void setupEngineForXWin(String gameId) {
        when(engineClient.createGame(gameId)).thenReturn(emptyBoard(gameId));

        Mockito.doAnswer(inv -> {
            MoveRequestDto request = inv.getArgument(1);
            String symbol = request.symbol();
            int pos = request.position();

            GameStatus status = GameStatus.IN_PROGRESS;
            PlayerSymbol winner = null;

            if ("X".equals(symbol) && pos == 2) {
                status = GameStatus.WIN;
                winner = PlayerSymbol.X;
            } else if (pos == 8) {
                status = GameStatus.DRAW;
            }

            return new GameStateDto(gameId, "XXXXXXXXX", status, winner, 5);
        }).when(engineClient).makeMove(eq(gameId), any(MoveRequestDto.class));
    }

    private GameStateDto emptyBoard(String gameId) {
        return new GameStateDto(gameId, "         ",
                GameStatus.IN_PROGRESS, null, 0);
    }
}
