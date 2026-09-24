
package com.coincard.core.model.card;

public class MinionCard extends Card {

    private final int attack;
    private final int maxHealth;
    private int currentHealth;
    private boolean canAttack;

    public MinionCard(String code, String name, int manaCost, int attack, int health, String description) {
        super(code, name, manaCost, CardType.MINION, description);
        if (attack < 0 || health <= 0) {
            throw new IllegalArgumentException("Chỉ số Minion không hợp lệ");
        }
        this.attack = attack;
        this.maxHealth = health;
        this.currentHealth = health;
        this.canAttack = false;
    }

    public int getAttack() {
        return attack;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public boolean canAttack() {
        return canAttack;
    }

    public void takeDamage(int amount) {
        currentHealth = Math.max(0, currentHealth - Math.max(0, amount));
    }

    public boolean isDead() {
        return currentHealth == 0;
    }

    public void readyForTurn() {
        canAttack = true;
    }

    public void exhaust() {
        canAttack = false;
    }

    @Override
    public Card copy() {
        return new MinionCard(getCode(), getName(), getManaCost(), attack, maxHealth, getDescription());
    }
}
