package vn.coincard.server.application;

import java.util.List;
import org.springframework.stereotype.Service;
import vn.coincard.server.catalog.CatalogCache;
import vn.coincard.server.game.DeckFactory;
import vn.coincard.server.game.GameException;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.mapper.GameStateMapper;
import vn.coincard.server.net.GameEvents;
import vn.coincard.server.net.OutboundGameGateway;
import vn.coincard.server.net.PlayerSession;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.room.RoomPlayer;
import vn.coincard.server.ws.SocketEvent;

/** Validates a private deck submission and decides whether a room can start. */
@Service
public final class LoadoutService {
  private final RoomManager rooms;
  private final CatalogCache catalog;
  private final OutboundGameGateway outbound;

  public LoadoutService(RoomManager rooms, CatalogCache catalog, OutboundGameGateway outbound) {
    this.rooms = rooms;
    this.catalog = catalog;
    this.outbound = outbound;
  }

  public Outcome submit(String socketId, PlayerSession session, HeroClass heroClass,
      String roomCode, List<String> cardSlugs) {
    Room room = rooms.getRoom(roomCode);
    synchronized (room) {
      if (room.isStarted() || room.isStarting()) {
        throw new GameException.InvalidCommand("Phòng đã bắt đầu trận.");
      }
      RoomPlayer player = room.getPlayerBySession(session.sessionToken());
      if (player == null || !socketId.equals(player.socketId())) {
        throw new GameException.InvalidPayload("Player không tồn tại trong room.");
      }
      if (heroClass == null || heroClass == HeroClass.NEUTRAL) {
        throw new GameException.InvalidDeck("Hero không hợp lệ.");
      }

      DeckFactory.validate(heroClass, cardSlugs, catalog.cards());
      room.selectLoadout(player.playerId(), heroClass, cardSlugs);
      outbound.toRoom(room.getRoomCode(), SocketEvent.LOADOUT_ACCEPTED,
          new GameEvents.LoadoutAccepted(room.getRoomCode(), GameStateMapper.toRoomSnapshot(room)));
      boolean shouldStart = room.isFull() && room.getPlayers().stream().allMatch(RoomPlayer::deckReady)
          && room.tryStart();
      return new Outcome(room, shouldStart);
    }
  }

  public record Outcome(Room room, boolean shouldStart) {}
}
