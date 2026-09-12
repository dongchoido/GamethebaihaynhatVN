package vn.coincard.server.room;

/** Mutable member record (internal). Snapshots are copied on read. */
public class RoomPlayer {
  public final String playerId;
  public String name;
  public final String sessionToken;
  public String socketId;
  public String heroClass;
  public boolean ready;

  public RoomPlayer(String playerId, String name, String sessionToken,
      String socketId, String heroClass, boolean ready) {
    this.playerId = playerId;
    this.name = name;
    this.sessionToken = sessionToken;
    this.socketId = socketId;
    this.heroClass = heroClass;
    this.ready = ready;
  }

  public RoomPlayer copy() {
    return new RoomPlayer(playerId, name, sessionToken, socketId, heroClass, ready);
  }
}
