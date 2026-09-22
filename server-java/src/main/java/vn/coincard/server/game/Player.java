package vn.coincard.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Player state and controlled mutation methods. */
public class Player {
  private int mana;
  private int maxMana;
  private int temporaryMana;
  private int fatigueDamage;
  private boolean heroPowerUsed;
  private final List<CardTypes.CardDefinition> hand = new ArrayList<>();
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

  private final String playerId;
  private final String name;
  private final Hero hero;
  private final Deck deck;

  public String id() { return playerId; }
  public String name() { return name; }
  public int currentMana() { return mana + temporaryMana; }
  public int currentMaxMana() { return maxMana; }
  public int fatigueDamage() { return fatigueDamage; }
  public boolean heroPowerUsed() { return heroPowerUsed; }
  public List<CardTypes.CardDefinition> handCards() { return Collections.unmodifiableList(hand); }
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

  public void spendMana(int amount) {
    if (amount < 0 || amount > currentMana()) throw new IllegalArgumentException("Mana không hợp lệ.");
    int fromTemporary = Math.min(temporaryMana, amount);
    temporaryMana -= fromTemporary;
    mana -= amount - fromTemporary;
  }

  public void refillMana() {
    mana = maxMana;
    temporaryMana = 0;
    heroPowerUsed = false;
  }

  public void gainTemporaryMana(int amount) {
    if (amount < 0) throw new IllegalArgumentException("Temporary mana không hợp lệ.");
    temporaryMana = Math.min(Constants.MAX_MANA - mana, temporaryMana + amount);
  }

  public void markHeroPowerUsed() {
    if (heroPowerUsed) throw new GameException.HeroPowerAlreadyUsed();
    heroPowerUsed = true;
  }

  public void increaseMaxMana() { maxMana = Math.min(maxMana + 1, Constants.MAX_MANA); }

  public void drawAndAddToHand(int count) {
    for (int i = 0; i < count; i++) {
      if (deck.size() == 0) break;
      addToHand(deck.drawOne());
    }
  }

  /** Draws one card, burns it when the hand is full, or applies fatigue. */
  public void drawForTurn() {
    if (deck.size() == 0) {
      fatigueDamage++;
      hero.takeDamage(fatigueDamage);
      return;
    }
    addToHand(deck.drawOne());
  }

  /** Không expose mutable list trực tiếp — chỉ thay đổi qua behavior. */
  public List<Minion> getBoard() { return Collections.unmodifiableList(board); }

  public boolean addToHand(CardTypes.CardDefinition card) {
    if (hand.size() >= Constants.MAX_HAND_SIZE) return false; // burn
    hand.add(card);
    return true;
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
    if (!minion.getOwnerId().equals(id())) throw new IllegalArgumentException("Sai chủ sở hữu minion.");
    if (board.size() >= Constants.MAX_BOARD_SIZE) throw new GameException.BoardFull();
    board.add(minion);
    minionsSummonedValue++;
  }

  public Minion removeMinion(String instanceId) {
    for (int i = 0; i < board.size(); i++) {
      if (board.get(i).getInstanceId().equals(instanceId)) return board.remove(i);
    }
    return null;
  }

  public Minion findMinion(String instanceId) {
    return board.stream().filter(m -> m.getInstanceId().equals(instanceId)).findFirst().orElse(null);
  }

  public void removeDeadMinions() {
    board.removeIf(Minion::isDead);
  }

  /** Polymorph-style replace in place: no free slot needed, not counted as a summon. */
  public void replaceMinion(String instanceId, Minion replacement) {
    if (!replacement.getOwnerId().equals(id())) throw new IllegalArgumentException("Không thể thay minion.");
    for (int i = 0; i < board.size(); i++) {
      if (board.get(i).getInstanceId().equals(instanceId)) {
        board.set(i, replacement);
        return;
      }
    }
    throw new IllegalArgumentException("Không thể thay minion.");
  }

  record State(int mana, int maxMana, int temporaryMana, int fatigueDamage,
      boolean heroPowerUsed, List<CardTypes.CardDefinition> hand, List<Minion.State> board,
      Deck.State deck, Hero.State hero, int damageDealt, int cardsPlayed, int minionsSummoned) {
    State {
      hand = List.copyOf(hand);
      board = List.copyOf(board);
    }
  }

  State snapshotState() {
    return new State(mana, maxMana, temporaryMana, fatigueDamage, heroPowerUsed, hand,
        board.stream().map(Minion::snapshotState).toList(), deck.snapshotState(), hero.snapshotState(),
        damageDealtValue, cardsPlayedValue, minionsSummonedValue);
  }

  void restoreState(State state) {
    hand.clear();
    hand.addAll(state.hand());
    board.clear();
    for (Minion.State minionState : state.board()) {
      minionState.instance().restoreState(minionState);
      board.add(minionState.instance());
    }
    deck.restoreState(state.deck());
    hero.restoreState(state.hero());
    mana = state.mana();
    maxMana = state.maxMana();
    temporaryMana = state.temporaryMana();
    fatigueDamage = state.fatigueDamage();
    heroPowerUsed = state.heroPowerUsed();
    damageDealtValue = state.damageDealt();
    cardsPlayedValue = state.cardsPlayed();
    minionsSummonedValue = state.minionsSummoned();
  }
}
