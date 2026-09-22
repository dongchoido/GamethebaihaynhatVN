package vn.coincard.server.application;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import vn.coincard.server.game.Constants;
import vn.coincard.server.game.GameSessionRegistry;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.service.GamePersistenceService;

/** Removes idle rooms and their runtime-only game state on a managed Spring scheduler. */
@Service
public final class CleanupService {
  private static final Logger log = LoggerFactory.getLogger(CleanupService.class);
  private final RoomManager rooms;
  private final ActiveGameRegistry activeGames;
  private final GameSessionRegistry sessions;
  private final GamePersistenceService persistence;
  private final TaskScheduler scheduler;

  public CleanupService(RoomManager rooms, ActiveGameRegistry activeGames,
      GameSessionRegistry sessions, GamePersistenceService persistence,
      @Qualifier("gameTaskScheduler") TaskScheduler scheduler) {
    this.rooms = rooms;
    this.activeGames = activeGames;
    this.sessions = sessions;
    this.persistence = persistence;
    this.scheduler = scheduler;
  }

  @PostConstruct
  public void schedule() {
    scheduler.scheduleAtFixedRate(this::cleanupIdleRooms, Constants.ROOM_SWEEP_INTERVAL_MS);
  }

  void cleanupIdleRooms() {
    cleanupIdleRooms(Constants.ROOM_IDLE_TTL_MS);
  }

  void cleanupIdleRooms(long idleTtlMs) {
    try {
      for (Room room : rooms.listRooms()) {
        if (!room.isIdle(idleTtlMs)) continue;
        String gameId = activeGames.removeRoom(room.getRoomCode());
        if (gameId != null) {
          sessions.remove(gameId);
          persistence.removeGame(gameId);
        }
        rooms.deleteRoom(room.getRoomCode());
      }
    } catch (RuntimeException error) {
      log.warn("Room cleanup thất bại", error);
    }
  }
}
