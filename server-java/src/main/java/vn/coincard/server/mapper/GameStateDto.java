package vn.coincard.server.mapper;

import java.util.List;
import vn.coincard.server.game.CardType;
import vn.coincard.server.game.EffectTarget;
import vn.coincard.server.game.EffectType;
import vn.coincard.server.game.GameStatus;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.Keyword;
import vn.coincard.server.game.Rarity;

/** Immutable wire DTOs. Jackson serializes record component names as protocol fields. */
public final class GameStateDto {
  private GameStateDto() {}

  public record EffectState(EffectType type, int value, EffectTarget target,
      Integer count, Integer minAttack, String cardSlug) {}

  public record CardState(String id, String name, String slug, String description,
      CardType type, Rarity rarity, int manaCost, int attack, int health, HeroClass heroClass,
      String imagePath, List<EffectState> effects, List<Keyword> keywords, boolean collectible) {
    public CardState {
      effects = List.copyOf(effects);
      keywords = List.copyOf(keywords);
    }
  }

  public record MinionState(String instanceId, String cardId, String name, int attack,
      int health, int maxHealth, boolean canAttack, boolean hasTaunt, String imagePath) {}

  public record HeroState(String heroId, String name, HeroClass heroClass, int health,
      int maxHealth, String imagePath, String powerName, int powerCost) {}

  public record PlayerState(String playerId, List<CardState> hand, int handCount,
      List<MinionState> board, int deckCount, int mana, int maxMana, HeroState hero,
      int damageDealt, int cardsPlayed, int minionsSummoned, boolean heroPowerUsed,
      int fatigueDamage) {
    public PlayerState {
      hand = List.copyOf(hand);
      board = List.copyOf(board);
    }
  }

  public record GameState(String gameId, String roomCode, GameStatus status, int turn,
      String activePlayerId, List<PlayerState> players, String winnerId, String statusMessage) {
    public GameState {
      players = List.copyOf(players);
    }
  }

  public record RoomPlayerState(String playerId, String name, HeroClass heroClass,
      boolean deckReady, boolean ready, boolean connected) {}
}
