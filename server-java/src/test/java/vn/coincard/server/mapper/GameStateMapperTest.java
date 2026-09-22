package vn.coincard.server.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.coincard.server.game.CardType;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.Deck;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.GameFactory;
import vn.coincard.server.game.GameTestAccess;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Rarity;

class GameStateMapperTest {

  @Test
  void mapsMinionAndHero() {
    Hero hero = new Hero("h1", "Jaina", HeroClass.MAGE, "Fireblast", 2, "img");
    Minion m = new Minion("m1", "c1", "Yeti", 4, 5, "p1", true, "img", true);
    GameStateDto.HeroState hm = GameStateMapper.toHeroState(hero);
    GameStateDto.MinionState mm = GameStateMapper.toMinionState(m);
    assertEquals("h1", hm.heroId());
    assertEquals(HeroClass.MAGE, hm.heroClass());
    assertEquals(30, hm.maxHealth());
    assertEquals("m1", mm.instanceId());
    assertEquals(4, mm.attack());
    assertTrue(mm.hasTaunt());
  }

  @Test
  void mapsGameStateForViewer() {
    Hero h1 = new Hero("h1", "Jaina", HeroClass.MAGE, "Fireblast", 2, "img");
    Hero h2 = new Hero("h2", "Rexxar", HeroClass.HUNTER, "Steady Shot", 2, "img");
    Deck d1 = new Deck(List.of(new CardTypes.CardDefinition("c1", "Yeti", "yeti", "",
        CardType.MINION, Rarity.COMMON, 4, 4, 5, HeroClass.NEUTRAL, "img",
        List.of(), List.of(), true)));
    Deck d2 = new Deck(List.of(new CardTypes.CardDefinition("c2", "Yeti", "yeti", "",
        CardType.MINION, Rarity.COMMON, 4, 4, 5, HeroClass.NEUTRAL, "img",
        List.of(), List.of(), true)));
    Game game = new GameFactory().create("g1", "R1",
        new GameFactory.PlayerInit("p1", "One", h1, d1),
        new GameFactory.PlayerInit("p2", "Two", h2, d2));
    GameTestAccess.start(game, "p1");
    GameStateDto.GameState state = GameStateMapper.toGameStateFor(game, "p1");
    assertEquals("g1", state.gameId());
    List<GameStateDto.PlayerState> players = state.players();
    // Owner sees hand, opponent not
    List<?> p1Hand = players.get(0).hand();
    List<?> p2Hand = players.get(1).hand();
    assertFalse(p1Hand.isEmpty());
    assertTrue(p2Hand.isEmpty());
  }
}
