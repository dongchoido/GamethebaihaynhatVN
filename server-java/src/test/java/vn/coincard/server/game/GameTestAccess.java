package vn.coincard.server.game;

/** Provides deterministic aggregate setup for tests without widening production visibility. */
public final class GameTestAccess {
  private GameTestAccess() {}

  public static Game create(String gameId, String roomCode, Player first, Player second) {
    return new Game(gameId, roomCode, first, second);
  }

  public static void start(Game game, String firstPlayerId) {
    game.start(firstPlayerId);
  }
}
