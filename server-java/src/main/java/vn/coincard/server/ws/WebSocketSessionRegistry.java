package vn.coincard.server.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import vn.coincard.server.net.OutboundEvent;
import vn.coincard.server.net.PlayerSession;

/** Owns socket registration, per-session data and serialized outbound messages. */
@Component
public final class WebSocketSessionRegistry {
  private static final Logger log = LoggerFactory.getLogger(WebSocketSessionRegistry.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final Map<String, PlayerSession> sessionData = new ConcurrentHashMap<>();

  public void register(WebSocketSession session) {
    sessions.put(session.getId(), session);
    sessionData.put(session.getId(), new PlayerSession());
  }

  public void remove(String sessionId) {
    sessions.remove(sessionId);
    sessionData.remove(sessionId);
  }

  public PlayerSession playerSession(String sessionId) {
    return sessionData.computeIfAbsent(sessionId, ignored -> new PlayerSession());
  }

  public void send(String sessionId, SocketEvent event, OutboundEvent data) {
    WebSocketSession session = sessions.get(sessionId);
    if (session == null || !session.isOpen()) return;
    try {
      synchronized (session) {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(new Envelope(event, data))));
      }
    } catch (Exception error) {
      log.debug("Không thể gửi WebSocket event {} cho {}", event, sessionId, error);
    }
  }

  public void close(WebSocketSession session) {
    try {
      session.close();
    } catch (Exception error) {
      log.debug("Không thể đóng WebSocket session {}", session.getId(), error);
    }
  }

  public void close(String sessionId) {
    WebSocketSession session = sessions.get(sessionId);
    if (session != null) close(session);
  }

  private record Envelope(SocketEvent event, OutboundEvent data) {}
}
