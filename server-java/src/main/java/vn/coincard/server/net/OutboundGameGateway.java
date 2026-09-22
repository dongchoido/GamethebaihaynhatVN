package vn.coincard.server.net;

import vn.coincard.server.ws.SocketEvent;

/** Application port for outbound room/session messages. */
public interface OutboundGameGateway {
  void toRoom(String roomCode, SocketEvent event, OutboundEvent data);
  void toSession(String sessionId, SocketEvent event, OutboundEvent data);
  void disconnectSession(String sessionId);
}
