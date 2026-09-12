package vn.coincard.server.game;

import java.util.ArrayList;
import java.util.List;

/** Mirror of server/src/game/Player.ts */
public class Player {
  private int mana;
  private int maxMana;
  private List<CardTypes.CardDefinition> hand = new ArrayList<>();
  private final List<Minion> board = new ArrayList<>();
  private int damageDealtValue;
  private int cardsPlayedValue;
  private int minionsSummonedValue;

  public Player(String playerId, String name, Hero hero, Deck deck) {
    this.playerId = playerId;
    this.name = name;
    this.hero = hero;
    this.deck = deck;
  }

  public final String playerId;
  public final String name;
  public final Hero hero;
  private final Deck deck;

  public String id() { return playerId; }
  public int currentMana() { return mana; }
  public int currentMaxMana() { return maxMana; }
  public List<CardTypes.CardDefinition> handCards() { return new ArrayList<>(hand); }
  public int handCount() { return hand.size(); }
  public int boardCount() { return board.size(); }
  public int deckSize() { return deck.size(); }
  public Hero heroState() { return hero; }
  public int damageDealt() { return damageDealtValue; }
  public int cardsPlayed() { return cardsPlayedValue; }
  public int minionsSummoned() { return minionsSummonedValue; }

  public void recordDamage(int amount) {
    if (amount > 0) damageDealtValue += amount;
  }

  public void recordCardPlayed() { cardsPlayedValue++; }

  public boolean hasEnoughMana(int cost) { return mana >= cost; }

  public void spendMana(int amount) {
    if (amount < 0 || amount > mana) throw new IllegalArgumentException("Mana không hợp lệ.");
    mana -= amount;
  }

  public void refillMana() { mana = maxMana; }

  public void increaseMaxMana() { maxMana = Math.min(maxMana + 1, Constants.MAX_MANA); }

  public CardTypes.CardDefinition drawCard() { return deck.drawOne(); }

  public void drawAndAddToHand(int count) {
    for (int i = 0; i < count; i++) {
      if (deck.size() == 0) break;
      addToHand(deck.drawOne());
    }
  }

  /** Opening-hand guarantee: swap most expensive hand card for cheapest deck card (<= maxCost). */
  public void guaranteeCheapOpener(int maxCost) {
    if (hand.stream().anyMatch(c -> c.manaCost() <= maxCost)) return;
    int expensiveIndex = -1;
    for (int i = 0; i < hand.size(); i++) {
      if (expensiveIndex == -1 || hand.get(i).manaCost() > hand.get(expensiveIndex).manaCost()) {
        expensiveIndex = i;
      }
    }
    if (expensiveIndex == -1) return;
    CardTypes.CardDefinition removed = hand.remove(expensiveIndex);
    CardTypes.CardDefinition cheap = deck.drawCheapest(maxCost);
    if (cheap != null) {
      deck.returnToBottom(removed);
      addToHand(cheap);
    } else {
      addToHand(removed);
    }
  }

  /** Defensive copy — board mutation only via Player methods. */
  public List<Minion> getBoard() { return new ArrayList<>(board); }

  public void addToHand(CardTypes.CardDefinition card) {
    if (hand.size() >= Constants.MAX_HAND_SIZE) return; // burn
    hand.add(card);
  }

  public CardTypes.CardDefinition findCardInHand(String instanceId) {
    return hand.stream().filter(c -> c.id().equals(instanceId)).findFirst().orElse(null);
  }

  public CardTypes.CardDefinition removeFromHand(String instanceId) {
    for (int i = 0; i < hand.size(); i++) {
      if (hand.get(i).id().equals(instanceId)) return hand.remove(i);
    }
    throw new GameException.CardNotInHand();
  }

  public void summonMinion(Minion minion) {
    if (!minion.ownerId.equals(id())) throw new IllegalArgumentException("Sai chủ sở hữu minion.");
    if (board.size() >= Constants.MAX_BOARD_SIZE) throw new GameException.BoardFull();
    board.add(minion);
    minionsSummonedValue++;
  }

  public Minion removeMinion(String instanceId) {
    for (int i = 0; i < board.size(); i++) {
      if (board.get(i).instanceId.equals(instanceId)) return board.remove(i);
    }
    return null;
  }

  public Minion findMinion(String instanceId) {
    return board.stream().filter(m -> m.instanceId.equals(instanceId)).findFirst().orElse(null);
  }

  public void removeDeadMinions() {
    board.removeIf(Minion::isDead);
  }

  /** Polymorph-style replace in place: no free slot needed, not counted as a summon. */
  public void replaceMinion(String instanceId, Minion replacement) {
    if (!replacement.ownerId.equals(id())) throw new IllegalArgumentException("Không thể thay minion.");
    for (int i = 0; i < board.size(); i++) {
      if (board.get(i).instanceId.equals(instanceId)) {
        board.set(i, replacement);
        return;
      }
    }
    throw new IllegalArgumentException("Không thể thay minion.");
  }

  /** Opaque undo. */
  public Runnable checkpoint() {
    List<CardTypes.CardDefinition> handSnap = new ArrayList<>(hand);
    List<Minion> boardSnap = new ArrayList<>(board);
    List<Runnable> undoUnits = boardSnap.stream().map(Minion::checkpoint).toList();
    Runnable undoHero = hero.checkpoint();
    Runnable undoDeck = deck.checkpoint();
    int manaSnap = mana, maxManaSnap = maxMana;
    int dmg = damageDealtValue, played = cardsPlayedValue, summoned = minionsSummonedValue;
    return () -> {
      hand = new ArrayList<>(handSnap);
      board.clear();
      board.addAll(boardSnap);
      undoUnits.forEach(Runnable::run);
      undoHero.run();
      undoDeck.run();
      mana = manaSnap;
      maxMana = maxManaSnap;
      damageDealtValue = dmg;
      cardsPlayedValue = played;
      minionsSummonedValue = summoned;
    };
  }
}
