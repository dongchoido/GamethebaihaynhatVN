package vn.coincard.server.application;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import vn.coincard.server.game.Deck;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.GameException;
import vn.coincard.server.game.GameFactory;
import vn.coincard.server.game.GameSessionRegistry;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.service.GamePersistenceService;

class CleanupServiceTest {
  @Test
  void removesAnIdleRoomAndItsRuntimeGameSession() {
    RoomManager rooms = new RoomManager();
    Room room = rooms.createRoom();
    Room roomWithoutGame = rooms.createRoom();
    ActiveGameRegistry activeGames = new ActiveGameRegistry();
    activeGames.associate(room.getRoomCode(), "game-1");
    GameSessionRegistry sessions = new GameSessionRegistry();
    sessions.register(game("game-1", room.getRoomCode()));
    TaskExecutor directExecutor = Runnable::run;
    GamePersistenceService persistence = new GamePersistenceService(
        (gameId, roomCode, players, winnerId) -> { }, directExecutor);
    CleanupService cleanup = new CleanupService(rooms, activeGames, sessions, persistence,
        new ConcurrentTaskScheduler());

    cleanup.cleanupIdleRooms(0);

    assertNull(activeGames.gameIdForRoom(room.getRoomCode()));
    assertThrows(GameException.RoomNotFound.class, () -> rooms.getRoom(room.getRoomCode()));
    assertThrows(GameException.RoomNotFound.class,
        () -> rooms.getRoom(roomWithoutGame.getRoomCode()));
    assertThrows(GameException.GameNotRunning.class, () -> sessions.require("game-1"));
  }

  private Game game(String gameId, String roomCode) {
    Hero firstHero = new Hero("hero-1", "Mage", HeroClass.MAGE, "Fireblast", 2, "img");
    Hero secondHero = new Hero("hero-2", "Hunter", HeroClass.HUNTER, "Steady Shot", 2, "img");
    return new GameFactory().create(gameId, roomCode,
        new GameFactory.PlayerInit("p1", "One", firstHero, new Deck(List.of())),
        new GameFactory.PlayerInit("p2", "Two", secondHero, new Deck(List.of())));
  }
}
