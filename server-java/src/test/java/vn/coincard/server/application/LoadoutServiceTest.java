package vn.coincard.server.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.coincard.server.game.DeckFactory;
import vn.coincard.server.game.GameException;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.net.PlayerSession;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.ws.SocketEvent;

class LoadoutServiceTest {
  @Test
  void acceptsValidatedDecksAndStartsOnlyAfterBothPlayersAreReady() {
    RoomManager rooms = new RoomManager();
    Room room = ApplicationFixtures.fullRoom(rooms);
    ApplicationFixtures.RecordingOutbound outbound = new ApplicationFixtures.RecordingOutbound();
    LoadoutService service = new LoadoutService(rooms, ApplicationFixtures.catalog(), outbound);
    List<String> deck = DeckFactory.defaultDeck(HeroClass.MAGE, ApplicationFixtures.cards());

    LoadoutService.Outcome first = service.submit("socket-1", session("token-1"), HeroClass.MAGE,
        room.getRoomCode(), deck);
    LoadoutService.Outcome second = service.submit("socket-2", session("token-2"), HeroClass.HUNTER,
        room.getRoomCode(), deck);

    assertFalse(first.shouldStart());
    assertTrue(second.shouldStart());
    assertTrue(room.isStarting());
    assertTrue(room.getPlayers().stream().allMatch(player -> player.deckReady()));
    assertTrue(outbound.deliveries.stream()
        .allMatch(delivery -> delivery.event() == SocketEvent.LOADOUT_ACCEPTED));
  }

  @Test
  void rejectsAnIllegalDeckWithoutMarkingThePlayerReady() {
    RoomManager rooms = new RoomManager();
    Room room = ApplicationFixtures.fullRoom(rooms);
    LoadoutService service = new LoadoutService(rooms, ApplicationFixtures.catalog(),
        new ApplicationFixtures.RecordingOutbound());

    assertThrows(GameException.InvalidDeck.class,
        () -> service.submit("socket-1", session("token-1"), HeroClass.MAGE,
            room.getRoomCode(), List.of("missing")));
    assertFalse(room.getPlayers().get(0).ready());
  }

  @Test
  void rejectsNeutralAsAHeroChoice() {
    RoomManager rooms = new RoomManager();
    Room room = ApplicationFixtures.fullRoom(rooms);
    LoadoutService service = new LoadoutService(rooms, ApplicationFixtures.catalog(),
        new ApplicationFixtures.RecordingOutbound());

    assertThrows(GameException.InvalidDeck.class,
        () -> service.submit("socket-1", session("token-1"), HeroClass.NEUTRAL,
            room.getRoomCode(), List.of()));
  }

  @Test
  void rejectsAStaleSocketAndAStartedRoom() {
    RoomManager rooms = new RoomManager();
    Room room = ApplicationFixtures.fullRoom(rooms);
    LoadoutService service = new LoadoutService(rooms, ApplicationFixtures.catalog(),
        new ApplicationFixtures.RecordingOutbound());
    List<String> deck = DeckFactory.defaultDeck(HeroClass.MAGE, ApplicationFixtures.cards());

    assertThrows(GameException.InvalidPayload.class,
        () -> service.submit("old-socket", session("token-1"), HeroClass.MAGE,
            room.getRoomCode(), deck));
    room.tryStart();
    assertThrows(GameException.InvalidCommand.class,
        () -> service.submit("socket-1", session("token-1"), HeroClass.MAGE,
            room.getRoomCode(), deck));
  }

  private PlayerSession session(String token) {
    PlayerSession session = new PlayerSession();
    session.bindSessionToken(token);
    return session;
  }
}
