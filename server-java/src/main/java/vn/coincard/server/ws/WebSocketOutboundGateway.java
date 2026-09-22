package vn.coincard.server.ws;

import org.springframework.stereotype.Component;
import vn.coincard.server.net.OutboundGameGateway;
import vn.coincard.server.net.OutboundEvent;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.room.RoomPlayer;

/** WebSocket adapter for the application outbound messaging port. */
@Component
public final class WebSocketOutboundGateway implements OutboundGameGateway {
  private final WebSocketSessionRegistry registry;
  private final RoomManager roomManager;

  public WebSocketOutboundGateway(WebSocketSessionRegistry registry, RoomManager roomManager) {
    this.registry = registry;
    this.roomManager = roomManager;
  }

  @Override
  public void toRoom(String roomCode, SocketEvent event, OutboundEvent data) {
    Room room;
    try {
      room = roomManager.getRoom(roomCode);
    } catch (RuntimeException error) {
      return;
    }
    for (RoomPlayer player : room.getPlayers()) {
      if (player.socketId() != null) registry.send(player.socketId(), event, data);
    }
  }

  @Override
  public void toSession(String sessionId, SocketEvent event, OutboundEvent data) {
    registry.send(sessionId, event, data);
  }

  @Override
  public void disconnectSession(String sessionId) {
    registry.close(sessionId);
  }
}
