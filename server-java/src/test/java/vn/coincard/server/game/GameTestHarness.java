package vn.coincard.server.game;

/** Test-only facade that drives the production command handler and session lock. */
public final class GameTestHarness {
  private final GameFactory factory = new GameFactory();
  private final GameSessionRegistry sessions = new GameSessionRegistry();
  private final GameEngine engine = new GameEngine();
  private final GameCommandHandler commands = new GameCommandHandler(engine, sessions);

  public Game createGame(String gameId, String roomCode,
      GameFactory.PlayerInit first, GameFactory.PlayerInit second) {
    Game game = factory.create(gameId, roomCode, first, second);
    sessions.register(game);
    return game;
  }

  public Game getGame(String gameId) {
    return sessions.withLockedGame(gameId, game -> game);
  }

  public void start(String gameId, String firstPlayerId) {
    sessions.withLockedGame(gameId, game -> {
      engine.start(game, firstPlayerId);
      return null;
    });
  }

  public void playCard(String gameId, String playerId, String cardInstanceId, String targetId) {
    commands.handle(new GameCommand.PlayCard(gameId, playerId, cardInstanceId, targetId));
  }

  public void attack(String gameId, String playerId, String attackerId, String targetId) {
    commands.handle(new GameCommand.Attack(gameId, playerId, attackerId, targetId));
  }

  public void endTurn(String gameId, String playerId) {
    commands.handle(new GameCommand.EndTurn(gameId, playerId));
  }

  public void useHeroPower(String gameId, String playerId) {
    commands.handle(new GameCommand.UseHeroPower(gameId, playerId));
  }

  public void concede(String gameId, String playerId) {
    commands.handle(new GameCommand.Concede(gameId, playerId));
  }
}
