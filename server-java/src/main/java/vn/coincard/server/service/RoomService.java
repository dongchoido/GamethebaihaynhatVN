package vn.coincard.server.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.coincard.server.mapper.GameStateMapper;
import vn.coincard.server.mapper.GameStateDto;
import vn.coincard.server.net.GameEvents;
import vn.coincard.server.net.OutboundGameGateway;
import vn.coincard.server.net.OutboundEvent;
import vn.coincard.server.net.PlayerSession;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.room.RoomPlayer;
import vn.coincard.server.ws.SocketEvent;

/**
 * Owns room creation and joining; loadout and match lifecycle live in dedicated services.
 */
@Service
public class RoomService {
  private final RoomManager roomManager;
  private final OutboundGameGateway sender;

  public RoomService(RoomManager roomManager, OutboundGameGateway sender) {
    this.roomManager = roomManager;
    this.sender = sender;
  }
  public void createRoom(String socketId, PlayerSession session, String playerName) {
    if (roomManager.findRoomBySocketId(socketId) != null) throw new IllegalArgumentException("Bạn đã ở trong phòng.");
    Room room = roomManager.createRoom();
    String token = UUID.randomUUID().toString();
    String playerId = "player-" + UUID.randomUUID();
    RoomPlayer first = new RoomPlayer(playerId, playerName, token, socketId, null, false);
    room.addPlayer(first);
    roomManager.indexPlayer(room, first);
    session.bindSessionToken(token);
    room.touch();
    sender.toSession(socketId, SocketEvent.ROOM_CREATED, new GameEvents.RoomCreated(
        room.getRoomCode(), playerId, token, GameStateMapper.toRoomSnapshot(room)));
  }

  public void joinRoom(String socketId, PlayerSession session, String roomCode, String playerName) {
    if (roomManager.findRoomBySocketId(socketId) != null) throw new IllegalArgumentException("Bạn đã ở trong phòng.");
    Room room = roomManager.getRoom(roomCode);
    String token = UUID.randomUUID().toString();
    String playerId = "player-" + UUID.randomUUID();
    RoomPlayer second = new RoomPlayer(playerId, playerName, token, socketId, null, false);
    room.addPlayer(second);
    roomManager.indexPlayer(room, second);
    session.bindSessionToken(token);
    room.touch();
    List<GameStateDto.RoomPlayerState> players = GameStateMapper.toRoomSnapshot(room);
    sender.toSession(socketId, SocketEvent.PLAYER_JOINED, new GameEvents.PlayerJoined(
        room.getRoomCode(), playerId, token, players));
    broadcastExcept(room, socketId, SocketEvent.PLAYER_JOINED,
        new GameEvents.PlayerJoined(room.getRoomCode(), null, null, players));
    if (room.isFull()) {
      sender.toRoom(room.getRoomCode(), SocketEvent.ROOM_READY,
          new GameEvents.RoomReady(room.getRoomCode()));
    }
  }

  private void broadcastExcept(Room room, String exceptSocketId, SocketEvent event,
      OutboundEvent data) {
    for (RoomPlayer p : room.getPlayers()) {
      if (p.socketId() != null && !p.socketId().equals(exceptSocketId)) {
        sender.toSession(p.socketId(), event, data);
      }
    }
  }
}
