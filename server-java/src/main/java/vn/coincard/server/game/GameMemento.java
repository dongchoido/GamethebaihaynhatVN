package vn.coincard.server.game;

/** Immutable full-aggregate snapshot used to roll back one failed command. */
public final class GameMemento {
  private final Game game;
  private final Game.State state;

  private GameMemento(Game game, Game.State state) {
    this.game = game;
    this.state = state;
  }

  public static GameMemento capture(Game game) {
    return new GameMemento(game, game.snapshotState());
  }

  public void restore() {
    game.restoreState(state);
  }
}
