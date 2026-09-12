package vn.coincard.server.net;

/** Transport abstraction so GameService never touches WebSocket directly. */
public interface MessageSender {
  void toRoom(String roomCode, String event, Object data);
  void toSession(String sessionId, String event, Object data);
  void disconnectSession(String sessionId);
}
