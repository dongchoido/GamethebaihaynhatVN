package hearthstone.model.game;

import hearthstone.model.card.Card;
import hearthstone.model.card.MinionCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Player {

    public static final int MAX_HAND_SIZE = 10;
    public static final int MAX_BOARD_SIZE = 7;
    public static final int MAX_MANA = 10;

    private final String id;
    private final String name;
    private final String deckCode;
    private final String deckName;
    private final Hero hero;
    private final List<Card> deck;
    private final List<Card> hand;
    private final List<MinionCard> board;
    private int totalMana;
    private int currentMana;

    public Player(String name, String deckCode, String deckName, List<Card> cards) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.deckCode = deckCode;
        this.deckName = deckName;
        this.hero = new Hero("Guardian");
        this.deck = new ArrayList<>(cards);
        this.hand = new ArrayList<>();
        this.board = new ArrayList<>();
        Collections.shuffle(this.deck);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDeckCode() {
        return deckCode;
    }

    public String getDeckName() {
        return deckName;
    }

    public Hero getHero() {
        return hero;
    }

    public List<Card> getHand() {
        return hand;
    }

    public List<MinionCard> getBoard() {
        return board;
    }

    public boolean hasDefendingMinions() {
        return board.stream().anyMatch(minion -> !minion.isDead());
    }

    public int getDeckSize() {
        return deck.size();
    }

    public int getTotalMana() {
        return totalMana;
    }

    public int getCurrentMana() {
        return currentMana;
    }

    public void drawCard() {
        if (deck.isEmpty()) {
            hero.takeDamage(1);
            return;
        }

        Card card = deck.remove(deck.size() - 1);
        if (hand.size() < MAX_HAND_SIZE) {
            hand.add(card);
        }
    }

    public void drawInitialHand(int amount) {
        for (int i = 0; i < amount; i++) {
            drawCard();
        }
    }

    public void beginFirstTurn() {
        totalMana = 1;
        currentMana = 1;
    }

    public void beginTurn() {
        totalMana = Math.min(MAX_MANA, totalMana + 1);
        currentMana = totalMana;
        board.forEach(MinionCard::readyForTurn);
        drawCard();
    }

    public boolean hasEnoughMana(int amount) {
        return currentMana >= amount;
    }

    public void spendMana(int amount) {
        if (!hasEnoughMana(amount)) {
            throw new IllegalStateException("Không đủ mana");
        }
        currentMana -= amount;
    }

   public Card findCardInHand(String cardId) {
    for (Card card : hand) {
        if (card.getInstanceId().equals(cardId)) {
            return card;
        }
    }

    throw new IllegalArgumentException("Không tìm thấy bài trên tay");
}

    public MinionCard findMinion(String minionId) {
        return board.stream()
                .filter(minion -> minion.getInstanceId().equals(minionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Minion"));
    }

    public void removeCardFromHand(Card card) {
        hand.remove(card);
    }

    public void summon(MinionCard minion) {
        if (board.size() >= MAX_BOARD_SIZE) {
            throw new IllegalStateException("Bàn đã đủ 7 Minion");
        }
        board.add(minion);
    }

    public void removeDeadMinions() {
        board.removeIf(MinionCard::isDead);
    }
}
