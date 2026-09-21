package vn.coincard.server.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import vn.coincard.server.mapper.GameStateMapper;
import vn.coincard.server.net.MessageSender;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.room.RoomPlayer;

/**
 * SRP: chỉ lo vòng đời phòng — tạo/join/select/reconnect/rematch/cleanup.
 * GameService sẽ delegate phần room cho service này.
 */
@Service
public class RoomService {
  private final RoomManager roomManager;
  private MessageSender sender = new MessageSender() {
    @Override public void toRoom(String r, String e, Object d) {}
    @Override public void toSession(String s, String e, Object d) {}
    @Override public void disconnectSession(String s) {}
  };

  public RoomService(RoomManager roomManager) {
    this.roomManager = roomManager;
  }

  public void setSender(MessageSender sender) { this.sender = sender; }
  public RoomManager rooms() { return roomManager; }

  public void createRoom(String socketId, Map<String, Object> session, String playerName) {
    if (roomManager.findRoomBySocketId(socketId) != null) throw new IllegalArgumentException("Bạn đã ở trong phòng.");
    Room room = roomManager.createRoom();
    String token = UUID.randomUUID().toString();
    String playerId = "player-" + UUID.randomUUID();
    RoomPlayer first = new RoomPlayer(playerId, playerName, token, socketId, null, false);
    room.addPlayer(first);
    roomManager.indexPlayer(room, first);
    session.put("sessionToken", token);
    room.touch();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("roomCode", room.getRoomCode());
    data.put("playerId", playerId);
    data.put("sessionToken", token);
    data.put("players", GameStateMapper.toRoomSnapshot(room));
    sender.toSession(socketId, "ROOM_CREATED", data);
  }

  public void joinRoom(String socketId, Map<String, Object> session, String roomCode, String playerName) {
    if (roomManager.findRoomBySocketId(socketId) != null) throw new IllegalArgumentException("Bạn đã ở trong phòng.");
    Room room = roomManager.getRoom(roomCode);
    String token = UUID.randomUUID().toString();
    String playerId = "player-" + UUID.randomUUID();
    RoomPlayer second = new RoomPlayer(playerId, playerName, token, socketId, null, false);
    room.addPlayer(second);
    roomManager.indexPlayer(room, second);
    session.put("sessionToken", token);
    room.touch();
    List<Map<String, Object>> players = GameStateMapper.toRoomSnapshot(room);
    Map<String, Object> mine = new LinkedHashMap<>();
    mine.put("roomCode", room.getRoomCode());
    mine.put("playerId", playerId);
    mine.put("sessionToken", token);
    mine.put("players", players);
    sender.toSession(socketId, "PLAYER_JOINED", mine);
    Map<String, Object> others = new LinkedHashMap<>();
    others.put("roomCode", room.getRoomCode());
    others.put("players", players);
    broadcastExcept(room, socketId, "PLAYER_JOINED", others);
    if (room.isFull()) sender.toRoom(room.getRoomCode(), "ROOM_READY", Map.of("roomCode", room.getRoomCode()));
  }

  private void broadcastExcept(Room room, String exceptSocketId, String event, Object data) {
    for (RoomPlayer p : room.getPlayers()) {
      if (p.socketId != null && !p.socketId.equals(exceptSocketId)) sender.toSession(p.socketId, event, data);
    }
  }

  // Delegates for GameService to keep compatible — sẽ dần chuyển logic sang đây
  public Room getRoom(String roomCode) { return roomManager.getRoom(roomCode); }
  public Room findRoomBySocketId(String socketId) { return roomManager.findRoomBySocketId(socketId); }
}
