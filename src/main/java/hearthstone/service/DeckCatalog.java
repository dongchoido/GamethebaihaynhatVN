package hearthstone.service;

import hearthstone.model.card.Card;
import hearthstone.model.card.CardType;
import hearthstone.model.card.MinionCard;
import hearthstone.model.card.SpellCard;

import java.util.ArrayList;
import java.util.List;

public class DeckCatalog {

    private final List<DeckOption> options = List.of(
            new DeckOption("EMBER", "Ember Legion",
                    "Minion tấn công mạnh và phép Fireball."),
            new DeckOption("FOREST", "Forest Guard",
                    "Minion nhiều máu và phép hồi phục tốt.")
    );

    public List<DeckOption> getOptions() {
        return options;
    }

    public List<Card> createDeck(String deckCode) {
        return switch (deckCode) {
            case "EMBER" -> createEmberDeck();
            case "FOREST" -> createForestDeck();
            default -> throw new IllegalArgumentException("Deck không tồn tại: " + deckCode);
        };
    }

    private List<Card> createEmberDeck() {
        List<Card> cards = new ArrayList<>();
        addCopies(cards, new MinionCard("FIRE_IMP", "Fire Imp", 1,
                2, 1, "Minion rẻ và nhanh."), 4);
        addCopies(cards, new MinionCard("FLAME_GUARD", "Flame Guard", 3,
                3, 4, "Chiến binh cân bằng."), 4);
        addCopies(cards, new MinionCard("LAVA_GOLEM", "Lava Golem", 5,
                6, 5, "Minion có sức tấn công lớn."), 3);
        addCopies(cards, new SpellCard("FIREBALL", "Fireball", 4,
                CardType.DAMAGE_SPELL, 6, "Gây 6 sát thương lên Hero."), 3);
        addCopies(cards, new SpellCard("WARM_LIGHT", "Warm Light", 2,
                CardType.HEAL_SPELL, 4, "Hồi 4 máu cho Hero."), 2);
        return cards;
    }

    private List<Card> createForestDeck() {
        List<Card> cards = new ArrayList<>();
        addCopies(cards, new MinionCard("SAPLING", "Living Sapling", 1,
                1, 3, "Cây non có nhiều máu."), 4);
        addCopies(cards, new MinionCard("WOLF", "Forest Wolf", 3,
                4, 3, "Thú săn của khu rừng."), 4);
        addCopies(cards, new MinionCard("ANCIENT", "Ancient Protector", 5,
                5, 7, "Minion phòng thủ bền bỉ."), 3);
        addCopies(cards, new SpellCard("THORN", "Thorn Strike", 3,
                CardType.DAMAGE_SPELL, 4, "Gây 4 sát thương lên Hero."), 3);
        addCopies(cards, new SpellCard("RENEW", "Renew", 3,
                CardType.HEAL_SPELL, 7, "Hồi 7 máu cho Hero."), 2);
        return cards;
    }

    private void addCopies(List<Card> target, Card template, int number) {
        for (int i = 0; i < number; i++) {
            target.add(template.copy());
        }
    }
}
