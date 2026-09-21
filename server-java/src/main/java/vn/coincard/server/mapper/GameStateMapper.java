package vn.coincard.server.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Player;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomPlayer;

/**
 * Centralized serialization — domain không format WebSocket.
 * Tách khỏi EffectResolver/CardTypes/GameService để thể hiện SRP.
 */
public final class GameStateMapper {
  private GameStateMapper() {}

  public static Map<String, Object> toMinionState(Minion m) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("instanceId", m.getInstanceId());
    out.put("cardId", m.getCardId());
    out.put("name", m.getName());
    out.put("attack", m.currentAttack());
    out.put("health", m.currentHealth());
    out.put("maxHealth", m.maxHealth());
    out.put("canAttack", m.canAttack());
    out.put("hasTaunt", m.hasTaunt());
    out.put("imagePath", m.getImagePath());
    return out;
  }

  public static Map<String, Object> toHeroState(Hero h) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("heroId", h.getHeroId());
    out.put("name", h.getName());
    out.put("heroClass", h.getHeroClass());
    out.put("health", h.currentHealth());
    out.put("maxHealth", h.maxHealth());
    out.put("imagePath", h.getImagePath());
    out.put("powerName", h.getPowerName());
    out.put("powerCost", h.getPowerCost());
    return out;
  }

  public static Map<String, Object> toCardState(CardTypes.CardDefinition c) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", c.id());
    out.put("name", c.name());
    out.put("slug", c.slug());
    out.put("description", c.description());
    out.put("type", c.type());
    out.put("rarity", c.rarity());
    out.put("manaCost", c.manaCost());
    out.put("attack", c.attack());
    out.put("health", c.health());
    out.put("heroClass", c.heroClass());
    out.put("imagePath", c.imagePath());
    out.put("effects", c.effects().stream().map(GameStateMapper::toEffectState).toList());
    out.put("keywords", c.keywords());
    out.put("collectible", c.collectible());
    return out;
  }

  private static Map<String, Object> toEffectState(CardTypes.EffectDefinition e) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("type", e.type());
    out.put("value", e.value());
    out.put("target", e.target());
    if (e.count() != null) out.put("count", e.count());
    if (e.minAttack() != null) out.put("minAttack", e.minAttack());
    if (e.cardSlug() != null) out.put("cardSlug", e.cardSlug());
    return out;
  }

  public static Map<String, Object> toPlayerState(Player player, boolean isOwner) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("playerId", player.id());
    List<Map<String, Object>> hand = new ArrayList<>();
    if (isOwner) {
      for (CardTypes.CardDefinition c : player.handCards()) hand.add(toCardState(c));
    }
    out.put("hand", hand);
    out.put("handCount", player.handCount());
    List<Map<String, Object>> board = new ArrayList<>();
    for (Minion m : player.getBoard()) board.add(toMinionState(m));
    out.put("board", board);
    out.put("deckCount", player.deckSize());
    out.put("mana", player.currentMana());
    out.put("maxMana", player.currentMaxMana());
    out.put("hero", toHeroState(player.heroState()));
    out.put("damageDealt", player.damageDealt());
    out.put("cardsPlayed", player.cardsPlayed());
    out.put("minionsSummoned", player.minionsSummoned());
    return out;
  }

  public static Map<String, Object> toGameStateFor(Game game, String viewerPlayerId) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("gameId", game.getGameId());
    out.put("roomCode", game.getRoomCode());
    out.put("status", game.getStatus());
    out.put("turn", game.getCurrentTurn());
    out.put("activePlayerId", game.getActivePlayerId());
    List<Map<String, Object>> players = new ArrayList<>();
    for (Player p : game.getPlayers()) players.add(toPlayerState(p, p.id().equals(viewerPlayerId)));
    out.put("players", players);
    out.put("winnerId", game.getWinnerId());
    out.put("statusMessage", game.getStatusMessage());
    out.put("manualDrawUsed", game.hasManualDrawnThisTurn());
    return out;
  }

  public static List<Map<String, Object>> toRoomSnapshot(Room room) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (RoomPlayer p : room.getPlayers()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("playerId", p.playerId);
      m.put("name", p.name);
      m.put("heroClass", p.heroClass);
      m.put("ready", p.ready);
      m.put("connected", p.socketId != null);
      out.add(m);
    }
    return out;
  }
}
