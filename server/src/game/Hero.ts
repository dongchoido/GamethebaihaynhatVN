import { MAX_HERO_HEALTH } from './constants.js';

// Hero của một player — health được quản lý bên trong class (encapsulation).
export class Hero {
  private health: number;

  constructor(
    public readonly heroId: string,
    public readonly name: string,
    public readonly heroClass: string,
    public readonly powerName: string,
    public readonly powerCost: number,
    public readonly imagePath: string,
    health: number = MAX_HERO_HEALTH,
  ) {
    this.health = health;
  }

  get currentHealth(): number {
    return this.health;
  }

  get maxHealth(): number {
    return MAX_HERO_HEALTH;
  }

  takeDamage(amount: number): void {
    this.health -= amount;
  }

  heal(amount: number): void {
    this.health = Math.min(this.maxHealth, this.health + amount);
  }

  isDead(): boolean {
    return this.health <= 0;
  }
}
