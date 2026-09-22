package vn.coincard.server.application;

import java.util.ArrayList;
import java.util.List;
import vn.coincard.server.catalog.CatalogCache;
import vn.coincard.server.db.Repositories;
import vn.coincard.server.game.CardType;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.Rarity;
import vn.coincard.server.net.OutboundEvent;
import vn.coincard.server.net.OutboundGameGateway;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.room.RoomPlayer;
import vn.coincard.server.ws.SocketEvent;

final class ApplicationFixtures {
  private ApplicationFixtures() {}

  static List<CardTypes.CardDefinition> cards() {
    List<CardTypes.CardDefinition> cards = new ArrayList<>();
    for (int index = 0; index < 15; index++) {
      cards.add(new CardTypes.CardDefinition("card-" + index, "Card " + index, "card-" + index,
          "", CardType.MINION, Rarity.COMMON, index % 5, 1, 1, HeroClass.NEUTRAL, "img",
          List.of(), List.of(), true));
    }
    return List.copyOf(cards);
  }

  static List<Repositories.HeroRecord> heroes() {
    return List.of(
        new Repositories.HeroRecord("mage", "Mage", HeroClass.MAGE, "Fireblast", 2, "mage.png"),
        new Repositories.HeroRecord("hunter", "Hunter", HeroClass.HUNTER, "Steady Shot", 2,
            "hunter.png"));
  }

  static CatalogCache catalog() {
    CatalogCache cache = new CatalogCache(new Repositories.CatalogRepository() {
      @Override
      public List<Repositories.HeroRecord> heroes() {
        return ApplicationFixtures.heroes();
      }

      @Override
      public List<CardTypes.CardDefinition> cards() {
        return ApplicationFixtures.cards();
      }
    });
    cache.reload();
    return cache;
  }

  static Room fullRoom(RoomManager rooms) {
    Room room = rooms.createRoom();
    RoomPlayer first = new RoomPlayer("p1", "One", "token-1", "socket-1", null, false);
    RoomPlayer second = new RoomPlayer("p2", "Two", "token-2", "socket-2", null, false);
    room.addPlayer(first);
    room.addPlayer(second);
    rooms.indexPlayer(room, first);
    rooms.indexPlayer(room, second);
    return room;
  }

  static final class RecordingOutbound implements OutboundGameGateway {
    final List<Delivery> deliveries = new ArrayList<>();

    @Override
    public void toRoom(String roomCode, SocketEvent event, OutboundEvent data) {
      deliveries.add(new Delivery("room:" + roomCode, event, data));
    }

    @Override
    public void toSession(String sessionId, SocketEvent event, OutboundEvent data) {
      deliveries.add(new Delivery(sessionId, event, data));
    }

    @Override
    public void disconnectSession(String sessionId) {
      deliveries.add(new Delivery(sessionId, null, null));
    }
  }

  record Delivery(String destination, SocketEvent event, OutboundEvent data) {}
}
