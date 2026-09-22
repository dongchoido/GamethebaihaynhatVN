package vn.coincard.server.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import vn.coincard.server.game.CardType;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.Deck;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.GameException;
import vn.coincard.server.game.GameFactory;
import vn.coincard.server.game.GameTestAccess;
import vn.coincard.server.game.GameTestHarness;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.Player;
import vn.coincard.server.game.Rarity;

class ConcurrencyTest {
  @Test
  void concurrentDuplicateCommandCommitsOnlyOnce() throws Exception {
    GameTestHarness engine = new GameTestHarness();
    Hero firstHero = new Hero("h1", "Jaina", HeroClass.MAGE, "Fireblast", 2, "img");
    Hero secondHero = new Hero("h2", "Rexxar", HeroClass.HUNTER, "Steady Shot", 2, "img");
    List<CardTypes.CardDefinition> cards = List.of(
        minion("deck-1", "deck-1"), minion("deck-2", "deck-2"), minion("deck-3", "deck-3"),
        minion("deck-4", "deck-4"));
    Game game = engine.createGame("g1", "R1",
        new GameFactory.PlayerInit("p1", "One", firstHero, new Deck(cards)),
        new GameFactory.PlayerInit("p2", "Two", secondHero, new Deck(cards)));
    GameTestAccess.start(game, "p1");
    Player player = game.getPlayerById("p1");
    player.refillMana();
    CardTypes.CardDefinition duplicateCommandCard = minion("play-once", "play-once");
    player.addToHand(duplicateCommandCard);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger rejected = new AtomicInteger();
    try {
      Future<?> first = executor.submit(() -> playSameCard(engine, ready, release, rejected));
      Future<?> second = executor.submit(() -> playSameCard(engine, ready, release, rejected));
      assertTrue(ready.await(2, java.util.concurrent.TimeUnit.SECONDS));
      release.countDown();
      first.get();
      second.get();
    } finally {
      executor.shutdownNow();
    }

    assertEquals(1, player.cardsPlayed());
    assertEquals(1, player.boardCount());
    assertEquals(1, rejected.get());
    assertTrue(player.currentMana() >= 0 && player.currentMana() <= 10);
  }

  private void playSameCard(GameTestHarness engine, CountDownLatch ready, CountDownLatch release,
      AtomicInteger rejected) {
    ready.countDown();
    try {
      release.await();
      engine.playCard("g1", "p1", "play-once", null);
    } catch (GameException.CardNotInHand expected) {
      rejected.incrementAndGet();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Test thread interrupted", interrupted);
    }
  }

  private CardTypes.CardDefinition minion(String id, String slug) {
    return new CardTypes.CardDefinition(id, "M", slug, "", CardType.MINION, Rarity.COMMON,
        1, 1, 1, HeroClass.NEUTRAL, "img", List.of(), List.of(), true);
  }
}
