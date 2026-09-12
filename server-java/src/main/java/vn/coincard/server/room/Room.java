package vn.coincard.server.room;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vn.coincard.server.game.GameException;

/** Mirror of server/src/room/Room.ts (max 2 players, session for reconnect). */
public class Room {
  private final List<RoomPlayer> players = new ArrayList<>();
  private boolean started;
  private boolean starting;
  private long lastActivityAt = System.currentTimeMillis();
  private final Set<String> rematchVotes = new HashSet<>();

  public Room(String roomCode) {
    this.roomCode = roomCode;
  }

  public final String roomCode;

  public synchronized void addPlayer(RoomPlayer player) {
    if (players.size() >= 2) throw new GameException.RoomFull();
    players.add(player);
    touch();
  }

  public synchronized List<RoomPlayer> getPlayers() {
    List<RoomPlayer> out = new ArrayList<>();
    for (RoomPlayer p : players) out.add(p.copy());
    return out;
  }

  /** Live reference for internal mutation only. */
  synchronized RoomPlayer internal(String playerId) {
    return players.stream().filter(p -> p.playerId.equals(playerId)).findFirst().orElse(null);
  }

  public synchronized RoomPlayer getPlayerBySession(String sessionToken) {
    RoomPlayer p = players.stream().filter(x -> x.sessionToken.equals(sessionToken)).findFirst().orElse(null);
    return p == null ? null : p.copy();
  }

  public synchronized RoomPlayer findBySocketId(String socketId) {
    RoomPlayer p = players.stream().filter(x -> socketId.equals(x.socketId)).findFirst().orElse(null);
    return p == null ? null : p.copy();
  }

  public synchronized void selectHero(String playerId, String heroClass) {
    RoomPlayer p = internal(playerId);
    if (p == null) throw new IllegalArgumentException("Player không tồn tại.");
    p.heroClass = heroClass;
    p.ready = true;
    touch();
  }

  public synchronized void bindSocket(String playerId, String socketId) {
    RoomPlayer p = internal(playerId);
    if (p == null) throw new IllegalArgumentException("Player không tồn tại.");
    p.socketId = socketId;
    touch();
  }

  public synchronized int count() { return players.size(); }
  public synchronized boolean isFull() { return players.size() == 2; }
  public synchronized boolean isStarted() { return started; }
  public synchronized boolean isStarting() { return starting; }
  public synchronized int getRematchVotes() { return rematchVotes.size(); }

  public synchronized void touch() { lastActivityAt = System.currentTimeMillis(); }

  public synchronized boolean isIdle(long ttlMs) {
    return System.currentTimeMillis() - lastActivityAt >= ttlMs
        && players.stream().allMatch(p -> p.socketId == null);
  }

  public synchronized boolean tryStart() {
    if (started || starting) return false;
    starting = true;
    return true;
  }

  public synchronized void markStarted() {
    started = true;
    starting = false;
  }

  public synchronized void cancelStart() { starting = false; }

  public synchronized RoomPlayer removeSocket(String socketId) {
    for (RoomPlayer p : players) {
      if (socketId.equals(p.socketId)) {
        p.socketId = null;
        touch();
        return p.copy();
      }
    }
    return null;
  }

  public synchronized int voteRematch(String playerId) {
    rematchVotes.add(playerId);
    touch();
    return rematchVotes.size();
  }

  public synchronized boolean tryStartRematch() {
    if (starting) return false;
    starting = true;
    started = false;
    rematchVotes.clear();
    for (RoomPlayer p : players) p.ready = true;
    touch();
    return true;
  }
}
