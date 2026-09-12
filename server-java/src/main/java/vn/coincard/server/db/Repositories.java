package vn.coincard.server.db;

import java.util.List;
import vn.coincard.server.game.CardTypes;

/** Repository interfaces — service depends on these, never on JDBC/Prisma. */
public final class Repositories {
  private Repositories() {}

  public record HeroRecord(String id, String name, String heroClass,
      String powerName, int powerCost, String imagePath) {}

  public interface CatalogRepository {
    List<HeroRecord> heroes();
    List<CardTypes.CardDefinition> cards();
  }

  public record GamePlayerInput(String playerId, String name, boolean winner) {}

  public interface GameRepository {
    void recordFinishedGame(String gameId, String roomCode,
        List<GamePlayerInput> players, String winnerId);
  }
}
