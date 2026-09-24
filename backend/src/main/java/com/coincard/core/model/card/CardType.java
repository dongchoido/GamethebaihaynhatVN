package com.coincard.core.model.card;

public enum CardType {
    MINION("Minion"),
    DAMAGE_SPELL("Damage Spell"),
    HEAL_SPELL("Heal Spell");

    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
