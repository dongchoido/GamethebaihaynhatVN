package vn.coincard.server.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Application-owned lifecycle registry for active game sessions. */
public final class GameSessionRegistry {
  private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

  public GameSession register(Game game) {
    GameSession session = new GameSession(game);
    GameSession existing = sessions.putIfAbsent(game.getGameId(), session);
    if (existing != null) throw new IllegalArgumentException("Game id đã tồn tại.");
    return session;
  }

  public GameSession require(String gameId) {
    GameSession session = sessions.get(gameId);
    if (session == null) throw new GameException.GameNotRunning();
    return session;
  }

  public <T> T withLockedGame(String gameId, Function<Game, T> work) {
    return require(gameId).withLock(work);
  }

  public void remove(String gameId) {
    sessions.remove(gameId);
  }
}
