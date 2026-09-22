package vn.coincard.server.db;

import java.util.List;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.HeroClass;

/** Repository interfaces keep game services independent from JDBC. */
public final class Repositories {
  private Repositories() {}

  public record HeroRecord(String id, String name, HeroClass heroClass,
      String powerName, int powerCost, String imagePath) {}

  public interface CatalogRepository {
    List<HeroRecord> heroes();
    List<CardTypes.CardDefinition> cards();
  }

  public record GamePlayerInput(String playerId, String name, boolean winner) {}

  public interface GameResultRepository {
    void recordFinishedGame(String gameId, String roomCode,
        List<GamePlayerInput> players, String winnerId);
  }
}
