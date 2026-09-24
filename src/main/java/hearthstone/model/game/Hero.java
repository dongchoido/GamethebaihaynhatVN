package hearthstone.model.game;

public class Hero {

    public static final int MAX_HEALTH = 30;

    private final String name;
    private int health;

    public Hero(String name) {
        this.name = name;
        this.health = MAX_HEALTH;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public void takeDamage(int amount) {
        health = Math.max(0, health - Math.max(0, amount));
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + Math.max(0, amount));
    }

    public boolean isDead() {
        return health == 0;
    }
}
