package vn.coincard.server.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import vn.coincard.server.game.GameException;
import vn.coincard.server.net.GameService;
import vn.coincard.server.net.MessageSender;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomPlayer;

/**
 * Plain WebSocket transport. Envelope: {"event": "...", "data": {...}}.
 * Same event names and payload shapes as the Socket.IO server, so the
 * React client only swaps its transport adapter.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler implements MessageSender {
  private final ObjectMapper mapper = new ObjectMapper();
  private final GameService service;
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Object>> sessionData = new ConcurrentHashMap<>();

  public GameWebSocketHandler(GameService service) {
    this.service = service;
    service.setSender(this);
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    sessions.put(session.getId(), session);
    sessionData.put(session.getId(), new ConcurrentHashMap<>());
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    closeQuietly(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessions.remove(session.getId());
    sessionData.remove(session.getId());
    safe(() -> service.handleDisconnect(session.getId()), session.getId());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    String socketId = session.getId();
    Map<String, Object> sessionMap = sessionData.computeIfAbsent(socketId,
        k -> new ConcurrentHashMap<>());
    try {
      Map<String, Object> envelope = mapper.readValue(message.getPayload(), Map.class);
      Object eventObj = envelope.get("event");
      if (!(eventObj instanceof String event)) {
        throw new GameException.InvalidPayload();
      }
      Map<String, Object> data = envelope.get("data") instanceof Map
          ? (Map<String, Object>) envelope.get("data")
          : new LinkedHashMap<>();
      route(socketId, sessionMap, event, data);
    } catch (GameException e) {
      send(socketId, "ACTION_REJECTED", Map.of("code", e.code(), "message", e.getMessage()));
    } catch (Exception e) {
      String msg = e.getMessage() != null ? e.getMessage() : "Unknown error.";
      send(socketId, "ACTION_REJECTED", Map.of("code", "UNKNOWN", "message", msg));
    }
  }

  private void route(String socketId, Map<String, Object> session, String event,
      Map<String, Object> data) {
    switch (event) {
      case "CREATE_ROOM" -> service.createRoom(socketId, session,
          Payload.requiredString(data, "playerName", 24));
      case "JOIN_ROOM" -> service.joinRoom(socketId, session,
          Payload.requiredString(data, "roomCode", 6),
          Payload.requiredString(data, "playerName", 24));
      case "SELECT_DECK" -> service.selectDeck(socketId, session,
          Payload.requiredString(data, "heroId", 32),
          Payload.requiredString(data, "roomCode", 6));
      case "PLAY_CARD" -> service.playCard(socketId, session,
          Payload.requiredString(data, "gameId", 64),
          Payload.requiredString(data, "cardInstanceId", 128),
          Payload.optionalString(data, "targetId", 128));
      case "ATTACK" -> service.attack(socketId, session,
          Payload.requiredString(data, "gameId", 64),
          Payload.requiredString(data, "attackerId", 128),
          Payload.requiredString(data, "targetId", 128));
      case "END_TURN" -> service.endTurn(socketId, session,
          Payload.requiredString(data, "gameId", 64));
      case "USE_HERO_POWER" -> service.useHeroPower(socketId, session,
          Payload.requiredString(data, "gameId", 64));
      case "DRAW_CARD" -> service.drawCard(socketId, session,
          Payload.requiredString(data, "gameId", 64));
      case "CONCEDE" -> service.concede(socketId, session,
          Payload.requiredString(data, "gameId", 64));
      case "RECONNECT_GAME" -> service.reconnect(socketId, session,
          Payload.requiredString(data, "sessionToken", 128));
      case "REMATCH" -> service.rematch(socketId, session,
          Payload.requiredString(data, "gameId", 64));
      default -> throw new GameException.InvalidPayload("Event không hỗ trợ.");
    }
  }

  private void safe(Runnable task, String socketId) {
    try {
      task.run();
    } catch (GameException e) {
      send(socketId, "ACTION_REJECTED", Map.of("code", e.code(), "message", e.getMessage()));
    } catch (Exception e) {
      String msg = e.getMessage() != null ? e.getMessage() : "Unknown error.";
      send(socketId, "ACTION_REJECTED", Map.of("code", "UNKNOWN", "message", msg));
    }
  }

  private void send(String socketId, String event, Object data) {
    WebSocketSession session = sessions.get(socketId);
    if (session == null || !session.isOpen()) return;
    try {
      Map<String, Object> envelope = new LinkedHashMap<>();
      envelope.put("event", event);
      envelope.put("data", data);
      synchronized (session) {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(envelope)));
      }
    } catch (Exception ignored) {
      // Send failures must never crash the server.
    }
  }

  private void closeQuietly(WebSocketSession session) {
    try {
      session.close();
    } catch (Exception ignored) {
    }
  }

  // ----- MessageSender -----

  @Override
  public void toRoom(String roomCode, String event, Object data) {
    Room room;
    try {
      room = service.rooms().getRoom(roomCode);
    } catch (RuntimeException e) {
      return;
    }
    for (RoomPlayer p : room.getPlayers()) {
      if (p.socketId != null) send(p.socketId, event, data);
    }
  }

  @Override
  public void toSession(String sessionId, String event, Object data) {
    send(sessionId, event, data);
  }

  @Override
  public void disconnectSession(String sessionId) {
    WebSocketSession session = sessions.get(sessionId);
    if (session != null) closeQuietly(session);
  }
}
