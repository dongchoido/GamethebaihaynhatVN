import type { CardDefinition } from '@coincard/shared';
import type { PrismaClient } from '@prisma/client';

// Dependency Inversion — GameService phụ thuộc interface, không trực tiếp Prisma.
export interface ICardRepository {
  findAll(): Promise<CardDefinition[]>;
  findBySlug(slug: string): Promise<CardDefinition | null>;
}

export interface IGameRepository {
  recordFinishedGame(input: {
    gameId: string;
    roomCode: string;
    players: { playerId: string; name: string; winner: boolean }[];
    winnerId: string | null;
  }): Promise<void>;
}

export class PrismaCardRepository implements ICardRepository {
  constructor(private readonly client: PrismaClient) {}

  async findAll(): Promise<CardDefinition[]> {
    return (await this.client.card.findMany()) as unknown as CardDefinition[];
  }

  async findBySlug(slug: string): Promise<CardDefinition | null> {
    const record = await this.client.card.findUnique({ where: { slug } });
    return (record as unknown as CardDefinition) || null;
  }
}

export class PrismaGameRepository implements IGameRepository {
  constructor(private readonly client: PrismaClient) {}

  async recordFinishedGame(input: {
    gameId: string;
    roomCode: string;
    players: { playerId: string; name: string; winner: boolean }[];
    winnerId: string | null;
  }): Promise<void> {
    await this.client.game.upsert({
      where: { id: input.gameId },
      update: {
        status: 'FINISHED',
        finishedAt: new Date(),
        winnerId: input.winnerId,
      },
      create: {
        id: input.gameId,
        roomCode: input.roomCode,
        status: 'FINISHED',
        finishedAt: new Date(),
        winnerId: input.winnerId,
      },
    });

    for (const p of input.players) {
      // Upsert player if not exist (pragma: name unique-ish)
      await this.client.player.upsert({
        where: { id: p.playerId },
        update: { name: p.name },
        create: { id: p.playerId, name: p.name },
      });
      await this.client.gamePlayer.upsert({
        where: { gameId_playerId: { gameId: input.gameId, playerId: p.playerId } },
        update: { winner: p.winner },
        create: {
          id: `${input.gameId}-${p.playerId}`,
          gameId: input.gameId,
          playerId: p.playerId,
          winner: p.winner,
        },
      });
    }
  }
}
