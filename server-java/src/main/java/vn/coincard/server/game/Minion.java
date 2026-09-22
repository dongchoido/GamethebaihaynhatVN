package vn.coincard.server.game;

import vn.coincard.server.model.GameCharacter;

/** Mutable minion — Inheritance: Minion là một GameCharacter. */
public class Minion extends GameCharacter {
  private int attack;
  private boolean canAttackValue;

  public Minion(String instanceId, String cardId, String name,
      int attack, int health, String ownerId, boolean hasCharge, String imagePath) {
    this(instanceId, cardId, name, attack, health, ownerId, hasCharge, imagePath, false);
  }

  public Minion(String instanceId, String cardId, String name,
      int attack, int health, String ownerId, boolean hasCharge, String imagePath, boolean hasTaunt) {
    super(health);
    this.instanceId = instanceId;
    this.cardId = cardId;
    this.name = name;
    this.attack = attack;
    this.ownerId = ownerId;
    this.imagePath = imagePath;
    this.hasTaunt = hasTaunt;
    this.canAttackValue = hasCharge;
  }

  private final String instanceId;
  private final String cardId;
  private final String name;
  private final String ownerId;
  private final String imagePath;
  private final boolean hasTaunt;

  public String getInstanceId() { return instanceId; }
  public String getCardId() { return cardId; }
  public String getName() { return name; }
  public String getOwnerId() { return ownerId; }
  public String getImagePath() { return imagePath; }
  public boolean hasTaunt() { return hasTaunt; }

  public int currentAttack() { return attack; }
  public boolean canAttack() { return canAttackValue; }

  public void startTurn() {
    canAttackValue = true;
  }

  @Override
  public int takeDamage(int amount) {
    int before = currentHealth();
    super.takeDamage(amount);
    return before - currentHealth();
  }

  public void modifyAttack(int delta) {
    attack = Math.max(0, attack + delta);
  }

  public void modifyHealth(int delta) {
    int newHealth = currentHealth() + delta;
    int newMax = maxHealth() + Math.max(0, delta);
    setMaxHealth(newMax);
    setCurrentHealth(newHealth);
  }

  public void markAsAttacked() { canAttackValue = false; }

  /** The instance reference preserves board identity when a failed transform is restored. */
  record State(Minion instance, int attack, int currentHealth, int maxHealth, boolean canAttack) {}

  State snapshotState() {
    return new State(this, attack, currentHealth(), maxHealth(), canAttackValue);
  }

  void restoreState(State state) {
    if (state.instance() != this) throw new IllegalArgumentException("Sai minion snapshot.");
    attack = state.attack();
    setMaxHealth(state.maxHealth());
    setCurrentHealth(state.currentHealth());
    canAttackValue = state.canAttack();
  }
}
