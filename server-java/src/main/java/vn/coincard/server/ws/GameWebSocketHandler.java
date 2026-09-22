package vn.coincard.server.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import vn.coincard.server.game.ErrorCode;
import vn.coincard.server.game.GameException;
import vn.coincard.server.net.GameEvents;
import vn.coincard.server.net.GameService;
import vn.coincard.server.net.PlayerSession;

/** JSON-tree WebSocket adapter that converts each inbound event to a typed request record. */
@Component
public final class GameWebSocketHandler extends TextWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final GameService service;
  private final WebSocketSessionRegistry registry;

  public GameWebSocketHandler(GameService service, WebSocketSessionRegistry registry) {
    this.service = service;
    this.registry = registry;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    registry.register(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.debug("WebSocket transport lỗi cho {}", session.getId(), exception);
    closeQuietly(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    registry.remove(session.getId());
    safe(() -> service.handleDisconnect(session.getId()), session.getId());
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    String socketId = session.getId();
    PlayerSession playerSession = registry.playerSession(socketId);
    try {
      JsonNode root = mapper.readTree(message.getPayload());
      if (root == null || !root.isObject() || !root.path("event").isTextual()) {
        throw new GameException.InvalidPayload();
      }
      SocketEvent event = SocketEvent.fromWire(root.path("event").asText());
      JsonNode data = root.get("data");
      if (data == null || !data.isObject()) throw new GameException.InvalidPayload();
      route(socketId, playerSession, event, data);
    } catch (GameException error) {
      reject(socketId, error.errorCode(), error.getMessage());
    } catch (Exception error) {
      log.warn("WebSocket request thất bại cho session {}", socketId, error);
      reject(socketId, ErrorCode.INTERNAL_ERROR, "Server không thể xử lý yêu cầu này.");
    }
  }

  private void route(String socketId, PlayerSession session, SocketEvent event, JsonNode data) {
    switch (event) {
      case CREATE_ROOM -> {
        InboundRequests.CreateRoom request = decode(data, InboundRequests.CreateRoom.class);
        service.createRoom(socketId, session, request.playerName());
      }
      case JOIN_ROOM -> {
        InboundRequests.JoinRoom request = decode(data, InboundRequests.JoinRoom.class);
        service.joinRoom(socketId, session, request.roomCode(), request.playerName());
      }
      case SUBMIT_LOADOUT -> {
        InboundRequests.SubmitLoadout request = decode(data, InboundRequests.SubmitLoadout.class);
        service.submitLoadout(socketId, session, request.heroClass(), request.roomCode(),
            request.cardSlugs());
      }
      case PLAY_CARD -> {
        InboundRequests.PlayCard request = decode(data, InboundRequests.PlayCard.class);
        service.playCard(socketId, session, request.gameId(), request.cardInstanceId(),
            request.targetId());
      }
      case ATTACK -> {
        InboundRequests.Attack request = decode(data, InboundRequests.Attack.class);
        service.attack(socketId, session, request.gameId(), request.attackerId(), request.targetId());
      }
      case END_TURN -> service.endTurn(socketId, session, decode(data,
          InboundRequests.GameAction.class).gameId());
      case USE_HERO_POWER -> service.useHeroPower(socketId, session, decode(data,
          InboundRequests.GameAction.class).gameId());
      case CONCEDE -> service.concede(socketId, session, decode(data,
          InboundRequests.GameAction.class).gameId());
      case RECONNECT_GAME -> service.reconnect(socketId, session, decode(data,
          InboundRequests.ReconnectGame.class).sessionToken());
      case REMATCH -> service.rematch(socketId, session, decode(data,
          InboundRequests.GameAction.class).gameId());
      default -> throw new GameException.InvalidCommand("Event không hỗ trợ.");
    }
  }

  private <T> T decode(JsonNode data, Class<T> requestType) {
    try {
      return mapper.treeToValue(data, requestType);
    } catch (JsonProcessingException | IllegalArgumentException error) {
      throw new GameException.InvalidPayload();
    }
  }

  private void safe(Runnable task, String socketId) {
    try {
      task.run();
    } catch (GameException error) {
      reject(socketId, error.errorCode(), error.getMessage());
    } catch (Exception error) {
      log.warn("WebSocket lifecycle thất bại cho session {}", socketId, error);
      reject(socketId, ErrorCode.INTERNAL_ERROR, "Server không thể xử lý yêu cầu này.");
    }
  }

  private void reject(String socketId, ErrorCode code, String message) {
    registry.send(socketId, SocketEvent.ACTION_REJECTED, new GameEvents.ActionRejected(code, message));
  }

  private void closeQuietly(WebSocketSession session) {
    registry.close(session);
  }
}
