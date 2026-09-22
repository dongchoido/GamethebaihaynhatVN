package vn.coincard.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.coincard.server.game.DeckFactory;
import vn.coincard.server.game.EffectResolver;
import vn.coincard.server.game.GameCommand;
import vn.coincard.server.game.GameCommandHandler;
import vn.coincard.server.game.GameEngine;
import vn.coincard.server.game.GameFactory;
import vn.coincard.server.game.GameSessionRegistry;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.HeroPower;
import vn.coincard.server.game.HeroPowerRegistry;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;

class CommandExecutionServiceTest {
  @Test
  void capturesViewerSpecificStateAndFinishedResultUnderTheCommandLock() {
    RoomManager rooms = new RoomManager();
    Room room = ApplicationFixtures.fullRoom(rooms);
    List<String> deck = DeckFactory.defaultDeck(HeroClass.MAGE, ApplicationFixtures.cards());
    room.selectLoadout("p1", HeroClass.MAGE, deck);
    room.selectLoadout("p2", HeroClass.HUNTER, deck);
    room.tryStart();

    GameSessionRegistry sessions = new GameSessionRegistry();
    ActiveGameRegistry activeGames = new ActiveGameRegistry();
    MatchService matches = new MatchService(ApplicationFixtures.catalog(), bound -> 0,
        new GameFactory(), new GameEngine(), sessions, activeGames);
    MatchService.StartedMatch match = matches.start(room);
    CommandExecutionService commands = new CommandExecutionService(new GameCommandHandler(
        new GameEngine(new EffectResolver(bound -> 0), new HeroPowerRegistry(HeroPower.defaults())),
        sessions));

    assertFalse(commands.isFinished(match.gameId()));
    assertEquals(4, commands.stateFor(match.gameId(), "p1").players().get(0).hand().size());
    assertTrue(commands.stateFor(match.gameId(), "p1").players().get(1).hand().isEmpty());

    CommandExecutionService.CommandResult result = commands.execute(
        new GameCommand.Concede(match.gameId(), "p1"), room);

    assertNotNull(result.finishedGame());
    assertEquals("p2", result.winnerId());
    assertEquals(2, result.viewers().size());
    assertTrue(commands.isFinished(match.gameId()));
  }
}
