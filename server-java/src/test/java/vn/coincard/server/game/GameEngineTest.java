package vn.coincard.server.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Port of the core GameEngine.test.ts cases. */
class GameEngineTest {
  private GameEngine engine;

  static CardTypes.CardDefinition minionCard(String id, int cost, int atk, int hp, String... keywords) {
    return new CardTypes.CardDefinition(id, "Test Minion", "test-minion", "Test.", "MINION",
        "COMMON", cost, atk, hp, "NEUTRAL", "assets/images/Minions/Chilwind Yeti.png",
        List.of(), List.of(keywords), true);
  }

  static CardTypes.CardDefinition spellCard(String id, int cost, CardTypes.EffectDefinition... effects) {
    return new CardTypes.CardDefinition(id, "Target Spell", "target-spell", "Test.", "SPELL",
        "COMMON", cost, 0, 0, "NEUTRAL", "assets/images/Minions/Chilwind Yeti.png",
        List.of(effects), List.of(), true);
  }

  static Hero mage() {
    return new Hero("hero_mage", "Jaina", "MAGE", "Fireblast", 2,
        "assets/images/Heros/Jaina Proudmoore.png");
  }

  static Deck deckOf(int n) {
    List<CardTypes.CardDefinition> cards = new ArrayList<>();
    for (int i = 0; i < n; i++) cards.add(minionCard("c-" + i + "-" + UUID.randomUUID(), 1, 2, 2));
    return new Deck(cards);
  }

  Game setup() {
    engine = new GameEngine();
    Game game = engine.createGame("game-1", "ABC123",
        new GameEngine.PlayerInit("p1", "One", mage(), deckOf(30)),
        new GameEngine.PlayerInit("p2", "Two", mage(), deckOf(30)));
    game.start();
    return game;
  }

  @Test
  void openingHands() {
    Game game = setup();
    assertEquals(3, game.getPlayerById("p1").handCount());
    assertEquals(4, game.getPlayerById("p2").handCount());
  }

  @Test
  void playMinionSpendsManaAndBoards() {
    Game game = setup();
    Player p = game.getPlayerById("p1");
    CardTypes.CardDefinition card = p.handCards().get(0);
    engine.playCard("game-1", "p1", card.id(), null);
    assertEquals(1, p.boardCount());
    assertEquals(0, p.currentMana());
  }

  @Test
  void rejectsWithoutMana() {
    Game game = setup();
    Player p = game.getPlayerById("p1");
    CardTypes.CardDefinition expensive = minionCard("big", 10, 8, 8);
    p.addToHand(expensive);
    assertThrows(GameException.NotEnoughMana.class,
        () -> engine.playCard("game-1", "p1", "big", null));
  }

  @Test
  void rejectsOffTurn() {
    Game game = setup();
    Player p2 = game.getPlayerById("p2");
    CardTypes.CardDefinition card = p2.handCards().get(0);
    assertThrows(GameException.NotPlayerTurn.class,
        () -> engine.playCard("game-1", "p2", card.id(), null));
  }

  @Test
  void summoningSickness() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    CardTypes.CardDefinition card = p1.handCards().get(0);
    engine.playCard("game-1", "p1", card.id(), null);
    Minion minion = p1.getBoard().get(0);
    assertThrows(GameException.class,
        () -> engine.attack("game-1", "p1", minion.instanceId, "p2"));
  }

  @Test
  void endTurnSwitchesAndRampsMana() {
    Game game = setup();
    engine.endTurn("game-1", "p1");
    assertEquals("p2", game.getActivePlayerId());
    assertEquals(1, game.getPlayerById("p2").currentMaxMana());
  }

  @Test
  void minionKillsHeroAndFinishes() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    Player p2 = game.getPlayerById("p2");
    p2.heroState().takeDamage(29);
    CardTypes.CardDefinition card = p1.handCards().get(0);
    engine.playCard("game-1", "p1", card.id(), null);
    Minion minion = p1.getBoard().get(0);
    engine.endTurn("game-1", "p1");
    engine.endTurn("game-1", "p2");
    engine.attack("game-1", "p1", minion.instanceId, "p2");
    assertTrue(game.isFinished());
    assertEquals("p1", game.getWinnerId());
  }

  @Test
  void boardFullRejectIsAtomic() {
    Game game = setup();
    Player p = game.getPlayerById("p1");
    for (CardTypes.CardDefinition c : new ArrayList<>(p.handCards())) {
      p.removeFromHand(c.id());
    }
    p.refillMana();
    int played = 0;
    while (played < Constants.MAX_BOARD_SIZE) {
      int need = Math.min(6 - p.handCount(), Constants.MAX_BOARD_SIZE - played);
      for (int i = 0; i < need; i++) p.addToHand(minionCard("z-" + played + "-" + i, 0, 1, 1));
      int batch = p.handCount();
      for (int i = 0; i < batch && played < Constants.MAX_BOARD_SIZE; i++) {
        CardTypes.CardDefinition c = p.handCards().get(p.handCount() - 1);
        engine.playCard("game-1", "p1", c.id(), null);
        played++;
      }
    }
    assertEquals(Constants.MAX_BOARD_SIZE, p.boardCount());
    CardTypes.CardDefinition extra = minionCard("extra", 0, 1, 1);
    p.addToHand(extra);
    int manaBefore = p.currentMana(), maxBefore = p.currentMaxMana(), handBefore = p.handCount();
    assertThrows(GameException.BoardFull.class,
        () -> engine.playCard("game-1", "p1", "extra", null));
    assertEquals(manaBefore, p.currentMana());
    assertEquals(maxBefore, p.currentMaxMana());
    assertEquals(handBefore, p.handCount());
    assertNotNull(p.findCardInHand("extra"));
  }

  @Test
  void spellWithoutTargetRejectsAtomically() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    CardTypes.CardDefinition spell = spellCard("s1", 0,
        new CardTypes.EffectDefinition("DAMAGE", 2, "ANY_MINION", null, null, null));
    p1.addToHand(spell);
    int manaBefore = p1.currentMana(), handBefore = p1.handCount();
    assertThrows(GameException.class, () -> engine.playCard("game-1", "p1", "s1", null));
    assertEquals(manaBefore, p1.currentMana());
    assertEquals(handBefore, p1.handCount());
    assertNotNull(p1.findCardInHand("s1"));
  }

  @Test
  void enemyOnlySpellRejectsOwnMinion() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    Player p2 = game.getPlayerById("p2");
    p1.summonMinion(new Minion("own-1", "c", "Own", 2, 5, "p1", false, "img"));
    p2.summonMinion(new Minion("foe-1", "c", "Foe", 2, 5, "p2", false, "img"));
    CardTypes.CardDefinition curse = spellCard("curse", 0,
        new CardTypes.EffectDefinition("DAMAGE", 2, "ENEMY_MINION", null, null, null));
    p1.addToHand(curse);
    int handBefore = p1.handCount();
    assertThrows(GameException.class, () -> engine.playCard("game-1", "p1", "curse", "own-1"));
    assertEquals(handBefore, p1.handCount());
    assertEquals(5, p1.findMinion("own-1").currentHealth());
    engine.playCard("game-1", "p1", "curse", "foe-1");
    assertEquals(3, p2.findMinion("foe-1").currentHealth());
  }

  @Test
  void siphonSoulCastsAndHeals() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    Player p2 = game.getPlayerById("p2");
    p2.summonMinion(new Minion("big-1", "c", "Big", 6, 6, "p2", false, "img"));
    p1.heroState().takeDamage(10);
    CardTypes.CardDefinition siphon = spellCard("siphon", 0,
        new CardTypes.EffectDefinition("DESTROY", 0, "ANY_MINION", null, null, null),
        new CardTypes.EffectDefinition("HEAL", 3, "FRIENDLY_HERO", null, null, null));
    p1.addToHand(siphon);
    engine.playCard("game-1", "p1", "siphon", "big-1");
    assertEquals(0, p2.boardCount());
    assertEquals(23, p1.heroState().currentHealth());
  }

  @Test
  void attackGuardsAndTaunt() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    Player p2 = game.getPlayerById("p2");
    Minion attacker = new Minion("atk-1", "c", "Atk", 3, 3, "p1", true, "img");
    p1.summonMinion(attacker);
    p2.summonMinion(new Minion("foe-2", "c", "Foe", 2, 2, "p2", true, "img"));
    assertThrows(GameException.class, () -> engine.attack("game-1", "p1", "foe-2", "p2"));
    assertThrows(GameException.class, () -> engine.attack("game-1", "p1", "atk-1", "p1"));
    p2.removeMinion("foe-2");
    engine.attack("game-1", "p1", "atk-1", "p2");
    assertEquals(27, p2.heroState().currentHealth());

    Minion attacker2 = new Minion("atk-2", "c", "Atk", 3, 5, "p1", true, "img");
    p1.summonMinion(attacker2);
    p2.summonMinion(new Minion("taunt-1", "c", "Taunt", 2, 4, "p2", false, "img", true));
    p2.summonMinion(new Minion("plain-1", "c", "Plain", 2, 4, "p2", false, "img"));
    assertThrows(GameException.MinionsBlockHero.class,
        () -> engine.attack("game-1", "p1", "atk-2", "p2"));
    assertThrows(GameException.TauntRequired.class,
        () -> engine.attack("game-1", "p1", "atk-2", "plain-1"));
    engine.attack("game-1", "p1", "atk-2", "taunt-1");
    assertEquals(1, p2.findMinion("taunt-1").currentHealth());
  }

  @Test
  void mustClearMinionsBeforeHero() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    Player p2 = game.getPlayerById("p2");
    Minion a = new Minion("a3", "c", "A", 3, 3, "p1", true, "img");
    Minion b = new Minion("b3", "c", "B", 3, 3, "p1", true, "img");
    p1.summonMinion(a);
    p1.summonMinion(b);
    p2.summonMinion(new Minion("f4", "c", "F", 2, 2, "p2", false, "img"));
    GameException ex = assertThrows(GameException.class,
        () -> engine.attack("game-1", "p1", "a3", "p2"));
    assertEquals("MINIONS_BLOCK_HERO", ex.code());
    assertTrue(a.canAttack());
    engine.attack("game-1", "p1", "a3", "f4");
    assertEquals(0, p2.boardCount());
    engine.attack("game-1", "p1", "b3", "p2");
    assertEquals(27, p2.heroState().currentHealth());
  }

  @Test
  void handLimitAndManualDraw() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    for (CardTypes.CardDefinition c : new ArrayList<>(p1.handCards())) p1.removeFromHand(c.id());
    for (int i = 0; i < 8; i++) p1.addToHand(minionCard("h" + i, 0, 1, 1));
    assertEquals(6, p1.handCount());
    assertThrows(GameException.HandFull.class, () -> engine.drawCard("game-1", "p1"));
  }

  @Test
  void bothHeroesDieIsDraw() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    Player p2 = game.getPlayerById("p2");
    p1.heroState().takeDamage(29);
    p2.heroState().takeDamage(29);
    CardTypes.CardDefinition boom = spellCard("boom", 0,
        new CardTypes.EffectDefinition("DAMAGE", 30, "ENEMY_HERO", null, null, null),
        new CardTypes.EffectDefinition("DAMAGE", 30, "FRIENDLY_HERO", null, null, null));
    p1.addToHand(boom);
    engine.playCard("game-1", "p1", "boom", null);
    assertTrue(game.isFinished());
    assertNull(game.getWinnerId());
  }

  @Test
  void statsCountActualDamage() {
    Game game = setup();
    Player p1 = game.getPlayerById("p1");
    Player p2 = game.getPlayerById("p2");
    CardTypes.CardDefinition charger = minionCard("chg", 0, 3, 3, "CHARGE");
    p1.addToHand(charger);
    engine.playCard("game-1", "p1", "chg", null);
    assertEquals(1, p1.cardsPlayed());
    Minion m = p1.getBoard().stream().filter(x -> x.cardId.equals("chg")).findFirst().orElseThrow();
    engine.attack("game-1", "p1", m.instanceId, "p2");
    assertEquals(3, p1.damageDealt());
    assertEquals(27, p2.heroState().currentHealth());
  }
}
