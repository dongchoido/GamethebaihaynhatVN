package vn.coincard.server.game;

import java.util.Objects;

/** Factory boundary for creating a fresh aggregate from validated player input. */
public final class GameFactory {
  public Game create(String gameId, String roomCode,
      PlayerInit first, PlayerInit second) {
    return new Game(gameId, roomCode,
        new Player(first.playerId(), first.name(), first.hero(), first.deck()),
        new Player(second.playerId(), second.name(), second.hero(), second.deck()));
  }

  public record PlayerInit(String playerId, String name, Hero hero, Deck deck) {
    public PlayerInit {
      require(playerId, "playerId");
      require(name, "name");
      Objects.requireNonNull(hero, "hero");
      Objects.requireNonNull(deck, "deck");
    }

    private static void require(String value, String field) {
      if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " là bắt buộc.");
    }
  }
}
