# Distributed Tic Tac Toe — Backend Assignment
 
A fully automated, microservices-based Tic Tac Toe system built with Spring Boot and Spring Cloud.
Two virtual players compete autonomously — the game is orchestrated entirely by the microservices,
with the UI watching the board update live via WebSocket.
 
---
 
## Architecture
 
```
                          ┌─────────────────────────┐
                          │   Discovery Server      │
                          │   Eureka  ·  port 8761  │
                          └────────────▲────────────┘
                                       │ all services register here
          ┌────────────────────────────┼─────────────────────────┐
          │                            │                         │
┌─────────▼──────────┐    ┌────────────▼─────────┐   ┌───────────▼───────┐
│   Game Engine      │◄───│   Game Session       │   │   UI Service      │
│   port 8081        │    │   port 8082          │   │   port 8083       │
│                    │    │                      │   │                   │
│  Move validation   │    │  Session lifecycle   │   │  Static frontend  │
│  Win detection     │    │  Simulation loop     │   │  REST proxy       │
│  Board state       │    │  WebSocket broker    │   │                   │
│  H2 in-memory      │    │  H2 in-memory        │   └──────────▲────────┘
└────────────────────┘    └───────────┬──────────┘              │
                                      │ STOMP/SockJS            │ WebSocket
                          ┌───────────▼───────────┐             │
                          │   API Gateway         ├─────────────┘
                          │   port 8080           │
                          │   Spring Cloud Gateway│◄──── Browser
                          └───────────────────────┘
```
 
### Service Summary
 
| Service | Port | Technology | Role |
|---|---|---|---|
| `discovery-server` | 8761 | Spring Cloud Netflix Eureka | Service registry — all services register and locate each other here |
| `api-gateway` | 8080 | Spring Cloud Gateway | Single entry point; load-balanced routing; `X-Trace-ID` injection |
| `game-engine-service` | 8081 | Spring Boot + H2 | Core Tic Tac Toe logic: move validation, win/draw detection, board state |
| `game-session-service` | 8082 | Spring Boot + H2 + WebSocket | Session orchestration; async simulation; STOMP broker for live updates |
| `ui-service` | 8083 | Spring Boot (static) | Serves the frontend; proxies REST calls to avoid browser CORS issues |
 
---
 
## Prerequisites
 
| Requirement | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| Docker + Docker Compose | any recent version |
 
---
 
## Running the System
 
### Option A — Docker Compose (recommended)
 
```bash
# 1. Build all modules
mvn clean package -DskipTests
 
# 2. Start all containers
docker-compose up --build
 
# 3. Tear down
docker-compose down
```
 
Services start in dependency order enforced by Actuator healthchecks:
`discovery-server` → `game-engine` → `game-session` → `api-gateway` + `ui-service`
 
| URL | What you'll find |
|---|---|
| http://localhost:8080 | The game UI |
| http://localhost:8761 | Eureka dashboard (all registered services) |
| http://localhost:8080/actuator/health | Gateway health status |
 
### Option B — Manual (development)
 
Start each service in a separate terminal **in this order**:
 
```bash
cd discovery-server       && mvn spring-boot:run   # wait for "Started" log
cd game-engine-service    && mvn spring-boot:run
cd game-session-service   && mvn spring-boot:run
cd api-gateway            && mvn spring-boot:run
cd ui-service             && mvn spring-boot:run
```
 
Then open **http://localhost:8083**.
 
---
 
## API Reference
 
### Game Session Service
 
| Method | Path | Status | Description |
|---|---|---|---|
| `POST` | `/sessions` | `201 Created` | Create a new game session; returns `{ "id": "<uuid>" }` |
| `POST` | `/sessions/{id}/simulate` | `202 Accepted` | Start async simulation; updates stream via WebSocket |
| `GET` | `/sessions/{id}` | `200 OK` | Full session details including move history |
 
### Game Engine Service
 
| Method | Path | Status | Description |
|---|---|---|---|
| `POST` | `/games/{gameId}` | `201 Created` | Initialise a new game (called internally by Session Service) |
| `GET` | `/games/{gameId}` | `200 OK` | Current board state and game status |
| `POST` | `/games/{gameId}/move` | `200 OK` | Apply a move; returns updated state |
 
**Move request body:**
```json
{ "symbol": "X", "position": 4 }
```
 
`position` is 0–8, row-major order:
```
0 │ 1 │ 2
──┼───┼──
3 │ 4 │ 5
──┼───┼──
6 │ 7 │ 8
```
 
**Error responses** use [RFC 9457 Problem Detail](https://www.rfc-editor.org/rfc/rfc9457):
```json
{ "status": 422, "detail": "Position 4 is already occupied by X" }
```
 
### WebSocket / STOMP
 
Connect via SockJS to `http://localhost:8082/ws`, then subscribe to:
```
/topic/game/{sessionId}
```
 
Message payload pushed after every move:
```json
{
  "sessionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "moveNumber": 3,
  "symbol": "X",
  "position": 4,
  "board": "X O  X   ",
  "status": "IN_PROGRESS",
  "winner": null
}
```
 
`status` values: `IN_PROGRESS` · `WIN` · `DRAW`
 
---
 
## Running Tests
 
```bash
# All modules
mvn test
 
# Individual service
cd game-engine-service  && mvn test
cd game-session-service && mvn test
```
 
### Test Strategy
 
**Game Engine** (`GameEngineIntegrationTest`) — `MockMvc` + embedded H2, no external dependencies. Covers:
- Game creation and retrieval
- Valid move application and board update
- Rejection of occupied cells (`422`)
- Win detection across all 8 lines (rows, columns, diagonals)
- Draw detection on a full board
- Move rejection after game is already over
**Game Session** (`GameSessionIntegrationTest`) — mocks `GameEngineClient` with Mockito; uses `Awaitility` to assert async simulation completion. Covers:
- Session creation (`201`)
- Simulation trigger returns `202 Accepted` immediately
- Double-simulate conflict (`409`)
- Move history populated after simulation completes
---
 
## Observability & Tracing
 
Distributed tracing is implemented using **custom filters + MDC** — a zero-dependency, log-based solution that provides full request correlation across all service boundaries.
 
- **Trace generation** — A unique `X-Trace-ID` UUID is generated at the `api-gateway` for every incoming request (or forwarded if already present)
- **Propagation** — The ID is passed downstream to all services via the `X-Trace-ID` HTTP header
- **MDC logging** — Each service captures the header in a `OncePerRequestFilter` and binds it into the Mapped Diagnostic Context; the log pattern includes `%X{traceId}` so every log line is correlated:
  ```
  12:00:01 INFO  [a3f9c812] GameSessionService : Simulation started for session 3fa85f64
  12:00:02 INFO  [a3f9c812] GameEngineService  : Move applied — X at position 4
  ```
- **Async support** — Trace context is propagated across `@Async` boundaries via a custom `MdcTaskDecorator` on the `simulationExecutor` thread pool, so simulation log lines carry the same trace ID as the originating HTTP request
- **Outbound calls** — All `Client` calls from Session Service to Engine Service include the current `X-Trace-ID` via a request header filter, ensuring the same ID appears in Engine logs for the same logical game
---
 
## Design Decisions
 
### `sessionId == gameId`
The Session Service generates one UUID per game and passes it directly to the Engine as the `gameId`. This eliminates any mapping table — cross-service lookups are trivial and the gateway stays fully stateless.
 
### Concurrency safety with `ReentrantLock`
Each game holds a dedicated `ReentrantLock` in a `ConcurrentHashMap<String, ReentrantLock>` on the Engine. Two simultaneous move requests for the same game are serialised at the lock; different games run fully in parallel. This is more correct than synchronizing on a JPA entity, which is re-fetched from the session cache on every transaction boundary.
 
### Async simulation with `@Async` + dedicated thread pool
`POST /sessions/{id}/simulate` returns `202 Accepted` immediately. The simulation loop runs on a named `ThreadPoolTaskExecutor` (threads named `sim-*`, 10 core / 20 max), so the Tomcat I/O threads are never blocked. `simulation.move-delay-ms` (default 600 ms) is externalized to `application.yml` and overridden per environment in `docker-compose.yml`.
 
### Shuffle-based move generation
Rather than picking a random free cell on each turn — which suffers from birthday-problem clustering and requires retries — all 9 positions are shuffled into a deck and dealt alternately to X and O. This guarantees termination in exactly 5–9 moves with no repeated sampling and produces varied, natural-looking games.
 
### WebSocket-first simulation start
The UI establishes the WebSocket subscription *before* calling `POST /simulate`. This ordering guarantee means no moves are missed between the HTTP response and the first STOMP frame — a subtle race condition that polling-first designs can't avoid.
 
---
 
## Potential Improvements
 
| Area | Improvement |
|---|---|
| **Resilience** | Add Resilience4j circuit breaker around `GameEngineClient` so the Session Service degrades gracefully when the Engine is unavailable |
| **Persistence** | Swap H2 for PostgreSQL — the `JpaRepository` interface is unchanged, only the datasource configuration differs |
| **Intelligence** | Replace the shuffle algorithm with Minimax for competitive AI — the simulation loop interface stays identical |
| **Scaling** | Replace the in-memory STOMP broker with RabbitMQ (`spring-cloud-starter-bus-amqp`) to support horizontal scaling of the Session Service |
| **Metrics** | Add Micrometer + Prometheus counters (move latency, win/draw ratio, active sessions) visualised in Grafana |
| **Log aggregation** | Ship logs to an ELK stack and search by `X-Trace-ID` for production-grade distributed debugging |
 