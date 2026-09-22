package vn.coincard.server.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import vn.coincard.server.db.Repositories.GamePlayerInput;
import vn.coincard.server.db.Repositories.GameResultRepository;

/**
 * SRP: chỉ lo lưu kết quả trận — tách khỏi GameService.
 * Thể hiện Single Responsibility và Dependency Inversion (phụ thuộc GameResultRepository abstraction).
 */
@Service
public class GamePersistenceService {
  private static final Logger log = LoggerFactory.getLogger(GamePersistenceService.class);
  private final GameResultRepository gameRepository;
  private final TaskExecutor taskExecutor;
  private final Set<String> savedGames = ConcurrentHashMap.newKeySet();
  private final Set<String> pendingSaves = ConcurrentHashMap.newKeySet();
  private final Set<String> gameOverEmitted = ConcurrentHashMap.newKeySet();

  public GamePersistenceService(GameResultRepository gameRepository,
      @Qualifier("gameTaskExecutor") TaskExecutor taskExecutor) {
    this.gameRepository = gameRepository;
    this.taskExecutor = taskExecutor;
  }

  public boolean shouldEmitGameOver(String gameId) {
    return !gameOverEmitted.contains(gameId);
  }

  public void markGameOverEmitted(String gameId) {
    gameOverEmitted.add(gameId);
  }

  public void saveGame(FinishedGame game, String currentGameIdForRoom, int attempt) {
    if (savedGames.contains(game.gameId()) || !pendingSaves.add(game.gameId())) return;
    doSave(game, currentGameIdForRoom, attempt);
  }

  private void doSave(FinishedGame game, String currentGameIdForRoom, int attempt) {
    CompletableFuture.runAsync(() -> {
      try {
        gameRepository.recordFinishedGame(game.gameId(), game.roomCode(), game.players(),
            game.winnerId());
        if (currentGameIdForRoom != null && currentGameIdForRoom.equals(game.gameId())) {
          savedGames.add(game.gameId());
        }
        pendingSaves.remove(game.gameId());
      } catch (RuntimeException error) {
        log.error("Không lưu được kết quả {}", game.gameId(), error);
        if (attempt < 2) {
          doSave(game, currentGameIdForRoom, attempt + 1);
        } else {
          pendingSaves.remove(game.gameId());
        }
      }
    }, taskExecutor);
  }

  public void removeGame(String gameId) {
    savedGames.remove(gameId);
    gameOverEmitted.remove(gameId);
    pendingSaves.remove(gameId);
  }

  /** Immutable result captured while a game session lock is held. */
  public record FinishedGame(String gameId, String roomCode, List<GamePlayerInput> players,
      String winnerId) {
    public FinishedGame {
      players = List.copyOf(players);
    }
  }
}
