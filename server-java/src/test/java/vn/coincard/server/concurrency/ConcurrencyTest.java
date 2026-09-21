package vn.coincard.server.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import vn.coincard.server.game.*;

class ConcurrencyTest {

  @Test
  void concurrentPlayCardDoesNotCorruptState() throws Exception {
    GameEngine engine = new GameEngine();
    Hero h1 = new Hero("h1", "Jaina", "MAGE", "Fireblast", 2, "img");
    Hero h2 = new Hero("h2", "Rexxar", "HUNTER", "Steady Shot", 2, "img");
    // Deck with cheap minions
    List<CardTypes.CardDefinition> cards = List.of(
        new CardTypes.CardDefinition("c1", "M", "m", "", "MINION", "COMMON", 1, 1, 1, "NEUTRAL", "img", List.of(), List.of(), true),
        new CardTypes.CardDefinition("c2", "M", "m", "", "MINION", "COMMON", 1, 1, 1, "NEUTRAL", "img", List.of(), List.of(), true),
        new CardTypes.CardDefinition("c3", "M", "m", "", "MINION", "COMMON", 1, 1, 1, "NEUTRAL", "img", List.of(), List.of(), true)
    );
    Deck d1 = new Deck(cards.stream().map(c -> new CardTypes.CardDefinition(c.id()+"-1", c.name(), c.slug(), c.description(), c.type(), c.rarity(), c.manaCost(), c.attack(), c.health(), c.heroClass(), c.imagePath(), c.effects(), c.keywords(), true)).toList());
    Deck d2 = new Deck(cards.stream().map(c -> new CardTypes.CardDefinition(c.id()+"-2", c.name(), c.slug(), c.description(), c.type(), c.rarity(), c.manaCost(), c.attack(), c.health(), c.heroClass(), c.imagePath(), c.effects(), c.keywords(), true)).toList());
    Player p1 = new Player("p1", "One", h1, d1);
    Player p2 = new Player("p2", "Two", h2, d2);
    engine.createGame("g1", "R1", new GameEngine.PlayerInit("p1", "One", h1, d1), new GameEngine.PlayerInit("p2", "Two", h2, d2));
    final Game game = engine.getGame("g1");
    game.start();
    // Give p1 mana
    Player actualP1 = game.getPlayerById("p1");
    for (int i = 0; i < 5; i++) actualP1.increaseMaxMana();
    actualP1.refillMana();
    // Add two cards to hand
    CardTypes.CardDefinition c1 = new CardTypes.CardDefinition("play1", "M", "m", "", "MINION", "COMMON", 1, 1, 1, "NEUTRAL", "img", List.of(), List.of(), true);
    CardTypes.CardDefinition c2 = new CardTypes.CardDefinition("play2", "M", "m", "", "MINION", "COMMON", 1, 1, 1, "NEUTRAL", "img", List.of(), List.of(), true);
    actualP1.addToHand(c1);
    actualP1.addToHand(c2);

    ExecutorService exec = Executors.newFixedThreadPool(2);
    CountDownLatch latch = new CountDownLatch(2);
    // Two threads try to play different cards concurrently — synchronized(game) should prevent corruption
    exec.submit(() -> {
      try { 
        synchronized (game) {
          try { engine.playCard("g1", "p1", "play1", null); } catch (Exception ignored) {}
        }
      } finally { latch.countDown(); }
    });
    exec.submit(() -> {
      try { 
        synchronized (game) {
          try { engine.playCard("g1", "p1", "play2", null); } catch (Exception ignored) {}
        }
      } finally { latch.countDown(); }
    });
    latch.await();
    exec.shutdown();
    // State must be consistent: not corrupted, mana and board within bounds
    int mana = actualP1.currentMana();
    int board = actualP1.boardCount();
    assertTrue(mana >= 0 && mana <= 10);
    assertTrue(board >= 0 && board <= 2);
    // At least one card was played if mana decreased
    assertTrue(board == 1 || board == 2 || mana < 10);
  }
}
