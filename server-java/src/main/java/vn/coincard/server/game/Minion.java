package vn.coincard.server.game;

/** Mirror of server/src/game/Minion.ts */
public class Minion {
  private int health;
  private int maximumHealth;
  private int attack;
  private boolean canAttackValue;
  private boolean summonedThisTurnValue;

  public Minion(String instanceId, String cardId, String name,
      int attack, int health, String ownerId, boolean hasCharge, String imagePath) {
    this(instanceId, cardId, name, attack, health, ownerId, hasCharge, imagePath, false);
  }

  public Minion(String instanceId, String cardId, String name,
      int attack, int health, String ownerId, boolean hasCharge, String imagePath, boolean hasTaunt) {
    this.instanceId = instanceId;
    this.cardId = cardId;
    this.name = name;
    this.attack = attack;
    this.health = health;
    this.maximumHealth = health;
    this.ownerId = ownerId;
    this.hasCharge = hasCharge;
    this.imagePath = imagePath;
    this.hasTaunt = hasTaunt;
    this.summonedThisTurnValue = !hasCharge;
    this.canAttackValue = hasCharge;
  }

  public final String instanceId;
  public final String cardId;
  public final String name;
  public final String ownerId;
  public final boolean hasCharge;
  public final String imagePath;
  public final boolean hasTaunt;

  public int currentHealth() { return health; }
  public int currentAttack() { return attack; }
  public int maxHealth() { return maximumHealth; }
  public boolean canAttack() { return canAttackValue; }
  public boolean summonedThisTurn() { return summonedThisTurnValue; }

  public void startTurn() {
    summonedThisTurnValue = false;
    canAttackValue = true;
  }

  /** Returns actual damage applied (no overkill). */
  public int takeDamage(int amount) {
    if (amount < 0) throw new IllegalArgumentException("Damage không hợp lệ.");
    int actual = Math.min(Math.max(0, health), amount);
    health -= actual;
    return actual;
  }

  public void heal(int amount) {
    if (amount < 0) throw new IllegalArgumentException("Heal không hợp lệ.");
    health = Math.min(maximumHealth, health + amount);
  }

  public void modifyAttack(int delta) {
    attack = Math.max(0, attack + delta);
  }

  public void modifyHealth(int delta) {
    health += delta;
    if (delta > 0) maximumHealth += delta;
  }

  public void markAsAttacked() { canAttackValue = false; }
  public boolean isDead() { return health <= 0; }

  /** Opaque undo. */
  public Runnable checkpoint() {
    int h = health, mh = maximumHealth, a = attack;
    boolean c = canAttackValue, s = summonedThisTurnValue;
    return () -> {
      health = h; maximumHealth = mh; attack = a;
      canAttackValue = c; summonedThisTurnValue = s;
    };
  }
}
