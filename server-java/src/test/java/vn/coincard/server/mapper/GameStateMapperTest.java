package vn.coincard.server.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vn.coincard.server.game.*;
import java.util.List;
import java.util.Map;

class GameStateMapperTest {

  @Test
  void mapsMinionAndHero() {
    Hero hero = new Hero("h1", "Jaina", "MAGE", "Fireblast", 2, "img");
    Minion m = new Minion("m1", "c1", "Yeti", 4, 5, "p1", true, "img", true);
    Map<String, Object> hm = GameStateMapper.toHeroState(hero);
    Map<String, Object> mm = GameStateMapper.toMinionState(m);
    assertEquals("h1", hm.get("heroId"));
    assertEquals("MAGE", hm.get("heroClass"));
    assertEquals(30, hm.get("maxHealth"));
    assertEquals("m1", mm.get("instanceId"));
    assertEquals(4, mm.get("attack"));
    assertEquals(true, mm.get("hasTaunt"));
  }

  @Test
  void mapsGameStateForViewer() {
    Hero h1 = new Hero("h1", "Jaina", "MAGE", "Fireblast", 2, "img");
    Hero h2 = new Hero("h2", "Rexxar", "HUNTER", "Steady Shot", 2, "img");
    Deck d1 = new Deck(List.of(new CardTypes.CardDefinition("c1", "Yeti", "yeti", "", "MINION", "COMMON", 4, 4, 5, "NEUTRAL", "img", List.of(), List.of(), true)));
    Deck d2 = new Deck(List.of(new CardTypes.CardDefinition("c2", "Yeti", "yeti", "", "MINION", "COMMON", 4, 4, 5, "NEUTRAL", "img", List.of(), List.of(), true)));
    Player p1 = new Player("p1", "One", h1, d1);
    Player p2 = new Player("p2", "Two", h2, d2);
    Game game = new Game("g1", "R1", p1, p2);
    game.start();
    Map<String, Object> state = GameStateMapper.toGameStateFor(game, "p1");
    assertEquals("g1", state.get("gameId"));
    List<Map<String, Object>> players = (List<Map<String, Object>>) state.get("players");
    // Owner sees hand, opponent not
    List<?> p1Hand = (List<?>) players.get(0).get("hand");
    List<?> p2Hand = (List<?>) players.get(1).get("hand");
    assertFalse(p1Hand.isEmpty());
    assertTrue(p2Hand.isEmpty());
  }
}
