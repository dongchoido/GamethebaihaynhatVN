package vn.coincard.server.application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Application-owned index from a room to its current game. */
public final class ActiveGameRegistry {
  private final Map<String, String> gameIdsByRoom = new ConcurrentHashMap<>();

  public void associate(String roomCode, String gameId) {
    gameIdsByRoom.put(roomCode, gameId);
  }

  public String gameIdForRoom(String roomCode) {
    return gameIdsByRoom.get(roomCode);
  }

  public boolean isCurrent(String roomCode, String gameId) {
    return gameId.equals(gameIdsByRoom.get(roomCode));
  }

  public String removeRoom(String roomCode) {
    return gameIdsByRoom.remove(roomCode);
  }

}
