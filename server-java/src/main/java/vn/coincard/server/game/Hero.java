package vn.coincard.server.game;

import vn.coincard.server.model.GameCharacter;

/** Mutable hero — Inheritance: Hero là một GameCharacter. */
public class Hero extends GameCharacter {
  public Hero(String heroId, String name, HeroClass heroClass,
      String powerName, int powerCost, String imagePath) {
    this(heroId, name, heroClass, powerName, powerCost, imagePath, Constants.MAX_HERO_HEALTH);
  }

  public Hero(String heroId, String name, HeroClass heroClass,
      String powerName, int powerCost, String imagePath, int health) {
    super(Constants.MAX_HERO_HEALTH);
    this.heroId = heroId;
    this.name = name;
    this.heroClass = heroClass;
    this.powerName = powerName;
    this.powerCost = powerCost;
    this.imagePath = imagePath;
    setCurrentHealth(Math.min(health, Constants.MAX_HERO_HEALTH));
  }

  private final String heroId;
  private final String name;
  private final HeroClass heroClass;
  private final String powerName;
  private final int powerCost;
  private final String imagePath;

  public String getHeroId() { return heroId; }
  public String getName() { return name; }
  public HeroClass getHeroClass() { return heroClass; }
  public String getPowerName() { return powerName; }
  public int getPowerCost() { return powerCost; }
  public String getImagePath() { return imagePath; }

  @Override
  public int takeDamage(int amount) {
    int before = currentHealth();
    super.takeDamage(amount);
    return before - currentHealth();
  }

  record State(int currentHealth, int maxHealth) {}

  State snapshotState() {
    return new State(currentHealth(), maxHealth());
  }

  void restoreState(State state) {
    setMaxHealth(state.maxHealth());
    setCurrentHealth(state.currentHealth());
  }
}
