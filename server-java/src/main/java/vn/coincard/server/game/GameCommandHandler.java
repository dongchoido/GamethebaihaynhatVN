package vn.coincard.server.game;

import java.util.function.Function;

/** Single dispatch point for all gameplay commands. */
public final class GameCommandHandler {
  private final GameEngine engine;
  private final GameSessionRegistry sessions;

  public GameCommandHandler(GameEngine engine, GameSessionRegistry sessions) {
    this.engine = engine;
    this.sessions = sessions;
  }

  public void handle(GameCommand command) {
    handle(command, game -> null);
  }

  /** Executes and derives an immutable outbound snapshot while the session remains locked. */
  public <T> T handle(GameCommand command, Function<Game, T> snapshotFactory) {
    return sessions.withLockedGame(command.gameId(), game -> {
      engine.execute(game, command);
      return snapshotFactory.apply(game);
    });
  }

  public <T> T read(String gameId, Function<Game, T> reader) {
    return sessions.withLockedGame(gameId, reader);
  }
}
