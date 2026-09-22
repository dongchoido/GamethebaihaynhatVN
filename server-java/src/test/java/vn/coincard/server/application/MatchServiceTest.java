package vn.coincard.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.coincard.server.game.DeckFactory;
import vn.coincard.server.game.GameFactory;
import vn.coincard.server.game.GameEngine;
import vn.coincard.server.game.GameSessionRegistry;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;

class MatchServiceTest {
  @Test
  void requiresExactlyTwoPlayersBeforeCreatingASession() {
    RoomManager rooms = new RoomManager();
    Room room = rooms.createRoom();
    MatchService service = new MatchService(ApplicationFixtures.catalog(), bound -> 0,
        new GameFactory(), new GameEngine(), new GameSessionRegistry(), new ActiveGameRegistry());

    assertThrows(IllegalStateException.class, () -> service.start(room));
  }

  @Test
  void startsAValidatedMatchWithDeterministicFirstPlayerAndViewerSnapshots() {
    RoomManager rooms = new RoomManager();
    Room room = ApplicationFixtures.fullRoom(rooms);
    List<String> deck = DeckFactory.defaultDeck(HeroClass.MAGE, ApplicationFixtures.cards());
    room.selectLoadout("p1", HeroClass.MAGE, deck);
    room.selectLoadout("p2", HeroClass.HUNTER, deck);
    room.tryStart();

    GameSessionRegistry sessions = new GameSessionRegistry();
    ActiveGameRegistry activeGames = new ActiveGameRegistry();
    MatchService service = new MatchService(ApplicationFixtures.catalog(), bound -> 0,
        new GameFactory(), new GameEngine(), sessions, activeGames);

    MatchService.StartedMatch match = service.start(room);

    assertTrue(room.isStarted());
    assertTrue(activeGames.isCurrent(room.getRoomCode(), match.gameId()));
    assertEquals(2, match.viewers().size());
    assertEquals("p1", sessions.withLockedGame(match.gameId(), game -> game.getActivePlayerId()));
    int firstHand = sessions.withLockedGame(match.gameId(),
        game -> game.getPlayerById("p1").handCount());
    int secondHand = sessions.withLockedGame(match.gameId(),
        game -> game.getPlayerById("p2").handCount());
    assertEquals(4, firstHand);
    assertEquals(5, secondHand);
  }
}
