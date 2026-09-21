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

  protected GameCharacter(int currentHealth, int maxHealth) {
    this.maxHealth = maxHealth;
    this.currentHealth = Math.min(currentHealth, maxHealth);
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

  public int getCurrentHealth() {
    return currentHealth;
  }

  public int getMaxHealth() {
    return maxHealth;
  }

  // Giữ API cũ cho tương thích với code hiện tại
  public int currentHealth() {
    return getCurrentHealth();
  }

  public int maxHealth() {
    return getMaxHealth();
  }

  protected void setCurrentHealth(int value) {
    currentHealth = value;
  }

  protected void setMaxHealth(int value) {
    maxHealth = value;
  }

  /** Dùng để checkpoint/rollback (Memento). */
  public Runnable checkpointHealth() {
    int h = currentHealth;
    int mh = maxHealth;
    return () -> {
      currentHealth = h;
      maxHealth = mh;
    };
  }

  public Runnable checkpoint() {
    return checkpointHealth();
  }

  public abstract String getCharacterType();
}
