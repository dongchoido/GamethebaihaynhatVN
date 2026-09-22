package vn.coincard.server.ws;

/** Stable WebSocket event names. Enums serialize as the existing uppercase protocol strings. */
public enum SocketEvent {
  CREATE_ROOM,
  JOIN_ROOM,
  SUBMIT_LOADOUT,
  PLAY_CARD,
  ATTACK,
  END_TURN,
  USE_HERO_POWER,
  CONCEDE,
  RECONNECT_GAME,
  REMATCH,
  ROOM_CREATED,
  PLAYER_JOINED,
  ROOM_READY,
  LOADOUT_ACCEPTED,
  GAME_STARTED,
  GAME_STATE_UPDATED,
  ACTION_REJECTED,
  PLAYER_DISCONNECTED,
  GAME_OVER;

  public static SocketEvent fromWire(String event) {
    try {
      return SocketEvent.valueOf(event);
    } catch (IllegalArgumentException error) {
      throw new vn.coincard.server.game.GameException.InvalidCommand("Event không hỗ trợ.");
    }
  }
}
