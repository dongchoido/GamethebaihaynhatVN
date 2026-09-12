package vn.coincard.server.game;

/** Mirror of server/src/game/Hero.ts */
public class Hero {
  private int health;

  public Hero(String heroId, String name, String heroClass,
      String powerName, int powerCost, String imagePath) {
    this(heroId, name, heroClass, powerName, powerCost, imagePath, Constants.MAX_HERO_HEALTH);
  }

  public Hero(String heroId, String name, String heroClass,
      String powerName, int powerCost, String imagePath, int health) {
    this.heroId = heroId;
    this.name = name;
    this.heroClass = heroClass;
    this.powerName = powerName;
    this.powerCost = powerCost;
    this.imagePath = imagePath;
    this.health = health;
  }

  public final String heroId;
  public final String name;
  public final String heroClass;
  public final String powerName;
  public final int powerCost;
  public final String imagePath;

  public int currentHealth() { return health; }
  public int maxHealth() { return Constants.MAX_HERO_HEALTH; }

  /** Returns actual damage applied (no overkill). */
  public int takeDamage(int amount) {
    if (amount < 0) throw new IllegalArgumentException("Damage không hợp lệ.");
    int actual = Math.min(Math.max(0, health), amount);
    health -= actual;
    return actual;
  }

  public void heal(int amount) {
    if (amount < 0) throw new IllegalArgumentException("Heal không hợp lệ.");
    health = Math.min(maxHealth(), health + amount);
  }

  public boolean isDead() { return health <= 0; }

  /** Opaque undo. */
  public Runnable checkpoint() {
    int h = health;
    return () -> health = h;
  }
}
