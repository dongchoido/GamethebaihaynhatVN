package vn.coincard.server.model;

/**
 * Abstract base cho mọi thực thể có máu trong game.
 * Thể hiện Inheritance: Hero và Minion cùng là GameCharacter,
 * dùng chung hành vi takeDamage/heal/isDead.
 */
public abstract class GameCharacter {
  private int currentHealth;
  private int maxHealth;

  protected GameCharacter(int maxHealth) {
    this.maxHealth = maxHealth;
    this.currentHealth = maxHealth;
  }

  /** Trả về sát thương thực tế đã trừ (không overkill). */
  public int takeDamage(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Damage cannot be negative");
    }
    int actual = Math.min(Math.max(0, currentHealth), amount);
    currentHealth -= actual;
    return actual;
  }

  public void heal(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Heal amount cannot be negative");
    }
    currentHealth = Math.min(maxHealth, currentHealth + amount);
  }

  public boolean isDead() {
    return currentHealth <= 0;
  }

  public int currentHealth() {
    return currentHealth;
  }

  public int maxHealth() {
    return maxHealth;
  }

  protected void setCurrentHealth(int value) {
    currentHealth = value;
  }

  protected void setMaxHealth(int value) {
    maxHealth = value;
  }
}
