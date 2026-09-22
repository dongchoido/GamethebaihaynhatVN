package vn.coincard.server.game;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/** Owns the lock for exactly one game aggregate. */
public final class GameSession {
  private final Game game;
  private final ReentrantLock lock = new ReentrantLock();

  GameSession(Game game) {
    this.game = game;
  }

  public <T> T withLock(Function<Game, T> work) {
    lock.lock();
    try {
      return work.apply(game);
    } finally {
      lock.unlock();
    }
  }
}
