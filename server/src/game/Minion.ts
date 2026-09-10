// Minion trên board — damage/attacked qua method (encapsulation), getters public.
export class Minion {
  private health: number;
  private attack: number;
  private canAttackValue: boolean;
  private summonedThisTurnValue: boolean;

  constructor(
    public readonly instanceId: string,
    public readonly cardId: string,
    public readonly name: string,
    attack: number,
    health: number,
    public readonly ownerId: string,
    public readonly hasCharge: boolean,
    public readonly imagePath: string,
  ) {
    this.attack = attack;
    this.health = health;
    this.hasCharge = hasCharge;
    this.summonedThisTurnValue = !hasCharge;
    this.canAttackValue = hasCharge; // Charge → tấn ngay
  }

  get currentHealth(): number {
    return this.health;
  }

  get currentAttack(): number {
    return this.attack;
  }

  get canAttack(): boolean {
    return this.canAttackValue;
  }

  get summonedThisTurn(): boolean {
    return this.summonedThisTurnValue;
  }

  startTurn(): void {
    this.summonedThisTurnValue = false;
    this.canAttackValue = true;
  }

  takeDamage(amount: number): void {
    this.health -= amount;
  }

  heal(amount: number): void {
    this.health += amount;
  }

  modifyAttack(delta: number): void {
    this.attack = Math.max(0, this.attack + delta);
  }

  modifyHealth(delta: number): void {
    this.health += delta;
  }

  markAsAttacked(): void {
    this.canAttackValue = false;
  }

  isDead(): boolean {
    return this.health <= 0;
  }
}
