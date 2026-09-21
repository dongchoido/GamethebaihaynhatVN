package vn.coincard.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import vn.coincard.server.db.Repositories.GamePlayerInput;
import vn.coincard.server.db.Repositories.GameRepository;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.Player;
import vn.coincard.server.room.Room;

/**
 * SRP: chỉ lo lưu kết quả trận — tách khỏi GameService.
 * Thể hiện Single Responsibility và Dependency Inversion (phụ thuộc GameRepository abstraction).
 */
@Service
public class GamePersistenceService {
  private final GameRepository gameRepository;
  private final Set<String> savedGames = ConcurrentHashMap.newKeySet();
  private final Set<String> pendingSaves = ConcurrentHashMap.newKeySet();
  private final Set<String> gameOverEmitted = ConcurrentHashMap.newKeySet();

  public GamePersistenceService(GameRepository gameRepository) {
    this.gameRepository = gameRepository;
  }

  public boolean shouldSave(String gameId) {
    return !savedGames.contains(gameId) && !pendingSaves.contains(gameId);
  }

  public boolean shouldEmitGameOver(String gameId) {
    return !gameOverEmitted.contains(gameId);
  }

  public void markGameOverEmitted(String gameId) {
    gameOverEmitted.add(gameId);
  }

  public void saveGame(Game game, Room room, String currentGameIdForRoom, int attempt) {
    if (!shouldSave(game.getGameId())) return;
    pendingSaves.add(game.getGameId());
    doSave(game, room, currentGameIdForRoom, attempt);
  }

  private void doSave(Game game, Room room, String currentGameIdForRoom, int attempt) {
    CompletableFuture.runAsync(() -> {
      try {
        List<GamePlayerInput> players = new ArrayList<>();
        for (Player p : game.getPlayers()) {
          players.add(new GamePlayerInput(p.id(), p.getName(),
              game.getWinnerId() != null && game.getWinnerId().equals(p.id())));
        }
        gameRepository.recordFinishedGame(game.getGameId(), room.getRoomCode(), players, game.getWinnerId());
        if (currentGameIdForRoom != null && currentGameIdForRoom.equals(game.getGameId())) {
          savedGames.add(game.getGameId());
        }
        pendingSaves.remove(game.getGameId());
      } catch (RuntimeException error) {
        System.err.println("Không lưu được kết quả " + game.getGameId() + ": " + error.getMessage());
        if (attempt < 2) {
          try { Thread.sleep(1000L * (attempt + 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
          doSave(game, room, currentGameIdForRoom, attempt + 1);
        } else {
          pendingSaves.remove(game.getGameId());
        }
      }
    });
  }

  public void removeGame(String gameId) {
    savedGames.remove(gameId);
    gameOverEmitted.remove(gameId);
    pendingSaves.remove(gameId);
  }

  public void clearPending(String gameId) {
    pendingSaves.remove(gameId);
  }
}
