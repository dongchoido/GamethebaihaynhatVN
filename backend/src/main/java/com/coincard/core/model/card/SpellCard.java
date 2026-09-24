
package com.coincard.core.model.card;


public class SpellCard extends Card {

    private final int power;

    public SpellCard(String code, String name, int manaCost,CardType type, int power, String description) {
        super(code, name, manaCost, type, description);
        if (type != CardType.DAMAGE_SPELL && type != CardType.HEAL_SPELL) {
            throw new IllegalArgumentException("Loại Spell không hợp lệ");
        }
        if (power <= 0) {
            throw new IllegalArgumentException("Sức mạnh Spell phải lớn hơn 0");
        }
        this.power = power;
    }

    public int getPower() {
        return power;
    }

    @Override
    public Card copy() {
        return new SpellCard(getCode(), getName(), getManaCost(), getType(), power, getDescription());
    }
}
