
package com.coincard.core.model.card;

import java.util.UUID;

public abstract class Card {

    private final String instanceId;
    private final String code;
    private final String name;
    private final int manaCost;
    private final CardType type;
    private final String description;

    protected Card(String code, String name, int manaCost, CardType type, String description) {
        if (manaCost < 0) {
            throw new IllegalArgumentException("Mana không thể âm");
        }
        this.instanceId = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.manaCost = manaCost;
        this.type = type;
        this.description = description;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getManaCost() {
        return manaCost;
    }

    public CardType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public abstract Card copy();
}