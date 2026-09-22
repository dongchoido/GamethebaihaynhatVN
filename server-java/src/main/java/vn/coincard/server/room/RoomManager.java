package vn.coincard.server.room;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import vn.coincard.server.game.GameException;

/** Owns rooms and O(1) socket/session indexes. */
@org.springframework.stereotype.Component
public class RoomManager {
  private final Map<String, Room> rooms = new ConcurrentHashMap<>();
  private final Map<String, Room> sockets = new ConcurrentHashMap<>();
  private final Map<String, Room> sessions = new ConcurrentHashMap<>();
  private final SecureRandom random = new SecureRandom();
  private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  public Room createRoom() {
    String code;
    do {
      StringBuilder sb = new StringBuilder(6);
      for (int i = 0; i < 6; i++) sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
      code = sb.toString();
    } while (rooms.containsKey(code));
    Room room = new Room(code);
    rooms.put(code, room);
    return room;
  }

  public Room getRoom(String roomCode) {
    String upper = roomCode == null ? "" : roomCode.toUpperCase().trim();
    Room room = rooms.get(upper);
    if (room == null) throw new GameException.RoomNotFound();
    return room;
  }

  public Room findRoomBySocketId(String socketId) {
    return socketId == null ? null : sockets.get(socketId);
  }

  public Room findRoomBySessionToken(String sessionToken) {
    return sessionToken == null ? null : sessions.get(sessionToken);
  }

  public List<Room> listRooms() {
    return new ArrayList<>(rooms.values());
  }

  public void indexPlayer(Room room, RoomPlayer player) {
    if (player.socketId() != null) sockets.put(player.socketId(), room);
    sessions.put(player.sessionToken(), room);
  }

  public void unindexSocket(String socketId) {
    if (socketId != null) sockets.remove(socketId);
  }

  public void deleteRoom(String roomCode) {
    String upper = roomCode == null ? "" : roomCode.toUpperCase().trim();
    Room room = rooms.get(upper);
    if (room == null) return;
    for (RoomPlayer p : room.getPlayers()) {
      if (p.socketId() != null) sockets.remove(p.socketId());
      sessions.remove(p.sessionToken());
    }
    rooms.remove(room.getRoomCode());
  }
}
