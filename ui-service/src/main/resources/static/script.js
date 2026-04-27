const currentHost = window.location.host;
const SESSION_BASE = "/api/sessions";
const WS_URL = `http://${currentHost}/ws`;

let sessionId = null;
let stompClient = null;
let lastCellIndex = -1;

function initBoard() {
  const board = document.getElementById("board");
  board.innerHTML = "";
  for (let i = 0; i < 9; i++) {
    const cell = document.createElement("div");
    cell.className = "cell";
    cell.id = "cell-" + i;
    board.appendChild(cell);
  }
}

async function startGame() {
  document.getElementById("btn-start").disabled = true;
  setStatus("Creating session…", "");

  try {
    const res = await fetch(SESSION_BASE, { method: "POST" });
    const data = await res.json();
    sessionId = data.id;

    connectWebSocket(sessionId, () => {
      fetch(SESSION_BASE + "/" + sessionId + "/simulate", {
        method: "POST",
      })
        .then(() => setStatus("Simulating…", "live"))
        .catch((err) => {
          setStatus("Simulation error: " + err.message, "");
          document.getElementById("btn-start").disabled = false;
        });
    });
  } catch (err) {
    setStatus("Error: " + err.message, "");
    document.getElementById("btn-start").disabled = false;
  }
}

function resetGame() {
  disconnectWebSocket();
  hideOverlay();
  sessionId = null;
  lastCellIndex = -1;
  initBoard();
  document.getElementById("move-log").innerHTML = "";
  document.getElementById("btn-start").disabled = false;
  setStatus("Ready to simulate", "");
}

function hideOverlay() {
  document.getElementById("winner-overlay").style.display = "none";
}

function connectWebSocket(sid, onConnected) {
  const socket = new SockJS(WS_URL);
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  stompClient.connect(
    {},
    () => {
      setWsIndicator(true);
      stompClient.subscribe("/topic/game/" + sid, (msg) => {
        handleGameUpdate(JSON.parse(msg.body));
      });
      if (onConnected) onConnected();
    },
    (err) => {
      setWsIndicator(false);
      document.getElementById("btn-start").disabled = false;
    },
  );
}

function disconnectWebSocket() {
  if (stompClient && stompClient.connected) {
    stompClient.disconnect();
  }
  stompClient = null;
  setWsIndicator(false);
}

function handleGameUpdate(update) {
  const { moveNumber, symbol, position, status, winner } = update;

  if (lastCellIndex >= 0) {
    document.getElementById("cell-" + lastCellIndex).classList.remove("latest");
  }

  const cell = document.getElementById("cell-" + position);
  if (cell) {
    cell.innerHTML = `<span class="symbol-${symbol.toLowerCase()}">${symbol}</span>`;
    cell.classList.add("played", "latest");
    lastCellIndex = position;
  }

  appendLog(moveNumber, symbol, position);

  if (status === "WIN" || status === "DRAW") {
    showFinishOverlay(status, winner);
    disconnectWebSocket();
  }
}

function showFinishOverlay(status, winner) {
  const overlay = document.getElementById("winner-overlay");
  const nameEl = document.getElementById("winner-name");

  if (status === "DRAW") {
    nameEl.textContent = "It's a Draw!";
    nameEl.style.color = "var(--text)";
    setStatus("It's a draw!", "draw");
  } else {
    nameEl.textContent = winner + " Wins!";
    nameEl.style.color = winner === "X" ? "var(--x-color)" : "var(--o-color)";
    setStatus(winner + " wins! 🎉", "win");
  }
  overlay.style.display = "flex";
}

function appendLog(moveNum, symbol, position) {
  const li = document.createElement("li");
  const row = Math.floor(position / 3) + 1;
  const col = (position % 3) + 1;
  li.innerHTML = `
        <span class="badge ${symbol}">${symbol}</span>
        <span>Move ${moveNum}</span>
        <span style="color:var(--muted); font-size:11px; margin-left:auto;">row ${row}, col ${col}</span>`;
  const log = document.getElementById("move-log");
  log.appendChild(li);
  log.scrollTop = log.scrollHeight;
}

function setStatus(text, cls) {
  const el = document.getElementById("status-bar");
  el.textContent = text;
  el.className = cls || "";
}

function setWsIndicator(connected) {
  const dot = document.getElementById("ws-dot");
  dot.className = connected ? "connected" : "";
  document.getElementById("ws-label").textContent = connected
    ? "live"
    : "disconnected";
}

initBoard();
