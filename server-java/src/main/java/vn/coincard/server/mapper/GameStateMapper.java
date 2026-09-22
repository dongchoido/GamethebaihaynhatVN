package vn.coincard.server.mapper;

import java.util.List;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Player;
import vn.coincard.server.room.Room;

/** Maps the domain aggregate to immutable, viewer-specific wire records. */
public final class GameStateMapper {
  private GameStateMapper() {}

  public static GameStateDto.MinionState toMinionState(Minion m) {
    return new GameStateDto.MinionState(m.getInstanceId(), m.getCardId(), m.getName(),
        m.currentAttack(), m.currentHealth(), m.maxHealth(), m.canAttack(), m.hasTaunt(),
        m.getImagePath());
  }

  public static GameStateDto.HeroState toHeroState(Hero h) {
    return new GameStateDto.HeroState(h.getHeroId(), h.getName(), h.getHeroClass(),
        h.currentHealth(), h.maxHealth(), h.getImagePath(), h.getPowerName(), h.getPowerCost());
  }

  public static GameStateDto.CardState toCardState(CardTypes.CardDefinition c) {
    List<GameStateDto.EffectState> effects = c.effects().stream()
        .map(e -> new GameStateDto.EffectState(e.type(), e.value(), e.target(), e.count(),
            e.minAttack(), e.cardSlug()))
        .toList();
    return new GameStateDto.CardState(c.id(), c.name(), c.slug(), c.description(), c.type(),
        c.rarity(), c.manaCost(), c.attack(), c.health(), c.heroClass(), c.imagePath(), effects,
        List.copyOf(c.keywords()), c.collectible());
  }

  public static GameStateDto.PlayerState toPlayerState(Player player, boolean isOwner) {
    List<GameStateDto.CardState> hand = isOwner
        ? player.handCards().stream().map(GameStateMapper::toCardState).toList()
        : List.of();
    List<GameStateDto.MinionState> board = player.getBoard().stream()
        .map(GameStateMapper::toMinionState).toList();
    return new GameStateDto.PlayerState(player.id(), hand, player.handCount(), board,
        player.deckSize(), player.currentMana(), player.currentMaxMana(),
        toHeroState(player.heroState()), player.damageDealt(), player.cardsPlayed(),
        player.minionsSummoned(), player.heroPowerUsed(), player.fatigueDamage());
  }

  public static GameStateDto.GameState toGameStateFor(Game game, String viewerPlayerId) {
    List<GameStateDto.PlayerState> players = game.getPlayers().stream()
        .map(p -> toPlayerState(p, p.id().equals(viewerPlayerId))).toList();
    return new GameStateDto.GameState(game.getGameId(), game.getRoomCode(), game.status(),
        game.getCurrentTurn(), game.getActivePlayerId(), players, game.getWinnerId(),
        game.getStatusMessage());
  }

  public static List<GameStateDto.RoomPlayerState> toRoomSnapshot(Room room) {
    return room.getPlayers().stream().map(p -> new GameStateDto.RoomPlayerState(
        p.playerId(), p.name(), p.heroClass(), p.deckReady(), p.ready(),
        p.socketId() != null)).toList();
  }
}
