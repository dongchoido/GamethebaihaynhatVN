package vn.coincard.server.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import vn.coincard.server.db.Repositories.CatalogRepository;
import vn.coincard.server.db.Repositories.GamePlayerInput;
import vn.coincard.server.db.Repositories.GameResultRepository;
import vn.coincard.server.db.Repositories.HeroRecord;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.CardType;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.Keyword;
import vn.coincard.server.game.Rarity;

/** JDBC adapters for the card catalog and transactional match results. */
@Repository
public class JdbcRepositories {
  private final ObjectMapper mapper = new ObjectMapper();

  public static void initSchema(JdbcTemplate jdbc) {
    jdbc.execute("CREATE TABLE IF NOT EXISTS Card (id TEXT PRIMARY KEY, name TEXT, slug TEXT UNIQUE,"
        + " description TEXT, type TEXT, rarity TEXT, manaCost INTEGER, attack INTEGER, health INTEGER,"
        + " heroClass TEXT, imagePath TEXT, effects TEXT, keywords TEXT, collectible INTEGER DEFAULT 1,"
        + " cardSet TEXT DEFAULT 'coincard-base', createdAt TEXT, updatedAt TEXT)");
    jdbc.execute("CREATE TABLE IF NOT EXISTS Hero (id TEXT PRIMARY KEY, name TEXT,"
        + " heroClass TEXT UNIQUE, powerName TEXT, powerCost INTEGER, imagePath TEXT)");
    jdbc.execute("CREATE TABLE IF NOT EXISTS Game (id TEXT PRIMARY KEY, roomCode TEXT, status TEXT,"
        + " startedAt TEXT, finishedAt TEXT, winnerId TEXT)");
    jdbc.execute("CREATE TABLE IF NOT EXISTS Player (id TEXT PRIMARY KEY, name TEXT, createdAt TEXT)");
    jdbc.execute("CREATE TABLE IF NOT EXISTS GamePlayer (id TEXT PRIMARY KEY, gameId TEXT, playerId TEXT,"
        + " winner INTEGER DEFAULT 0, UNIQUE(gameId, playerId))");
    jdbc.execute("DROP INDEX IF EXISTS Game_roomCode_key");
    jdbc.execute("CREATE INDEX IF NOT EXISTS Game_roomCode_idx ON Game(roomCode)");
  }

  @Repository
  public static class JdbcCatalogRepository implements CatalogRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public JdbcCatalogRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<HeroRecord> heroes() {
      return jdbc.query("SELECT id, name, heroClass, powerName, powerCost, imagePath FROM Hero",
          (rs, i) -> new HeroRecord(rs.getString(1), rs.getString(2), HeroClass.fromWire(rs.getString(3)),
              rs.getString(4), rs.getInt(5), rs.getString(6)));
    }

    @Override
    public List<CardTypes.CardDefinition> cards() {
      return jdbc.query(
          "SELECT id, name, slug, description, type, rarity, manaCost, attack, health,"
              + " heroClass, imagePath, effects, keywords, collectible FROM Card WHERE collectible = 1",
          (rs, i) -> {
            try {
              List<CardTypes.EffectDefinition> effects = mapper.readValue(rs.getString("effects"),
                  new TypeReference<List<CardTypes.EffectDefinition>>() {});
              List<Keyword> keywords = mapper.readValue(rs.getString("keywords"),
                  new TypeReference<List<Keyword>>() {});
              return new CardTypes.CardDefinition(
                  rs.getString("id"), rs.getString("name"), rs.getString("slug"),
                  rs.getString("description"), CardType.fromWire(rs.getString("type")),
                  Rarity.fromWire(rs.getString("rarity")), rs.getInt("manaCost"),
                  rs.getInt("attack"), rs.getInt("health"),
                  HeroClass.fromWire(rs.getString("heroClass")), rs.getString("imagePath"),
                  effects, keywords, rs.getInt("collectible") == 1);
            } catch (Exception e) {
              throw new IllegalStateException("Catalog row lỗi.", e);
            }
          });
    }
  }

  @Repository
  public static class JdbcGameRepository implements GameResultRepository {
    private final TransactionTemplate tx;

    public JdbcGameRepository(JdbcTemplate jdbc) {
      this.tx = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
    }

    @Override
    public void recordFinishedGame(String gameId, String roomCode,
        List<GamePlayerInput> players, String winnerId) {
      tx.executeWithoutResult(status -> {
        JdbcTemplate jdbc = new JdbcTemplate(
            ((DataSourceTransactionManager) tx.getTransactionManager()).getDataSource());
        String now = Instant.now().toString();
        int updated = jdbc.update(
            "UPDATE Game SET status='FINISHED', finishedAt=?, winnerId=? WHERE id=?",
            now, winnerId, gameId);
        if (updated == 0) {
          jdbc.update("INSERT INTO Game (id, roomCode, status, startedAt, finishedAt, winnerId)"
              + " VALUES (?, ?, 'FINISHED', ?, ?, ?)", gameId, roomCode, now, now, winnerId);
        }
        for (GamePlayerInput p : players) {
          jdbc.update("INSERT INTO Player (id, name, createdAt) VALUES (?, ?, ?)"
              + " ON CONFLICT(id) DO UPDATE SET name=excluded.name",
              p.playerId(), p.name(), now);
          int gu = jdbc.update("UPDATE GamePlayer SET winner=? WHERE gameId=? AND playerId=?",
              p.winner() ? 1 : 0, gameId, p.playerId());
          if (gu == 0) {
            jdbc.update("INSERT INTO GamePlayer (id, gameId, playerId, winner) VALUES (?, ?, ?, ?)",
                gameId + "-" + p.playerId(), gameId, p.playerId(), p.winner() ? 1 : 0);
          }
        }
      });
    }
  }

  public int countCards(JdbcTemplate jdbc) {
    Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM Card", Integer.class);
    return n == null ? 0 : n;
  }

  public int countHeroes(JdbcTemplate jdbc) {
    Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM Hero", Integer.class);
    return n == null ? 0 : n;
  }

  public void upsertCard(JdbcTemplate jdbc, CardTypes.CardDefinition card, String now) {
    try {
      String effects = mapper.writeValueAsString(card.effects());
      String keywords = mapper.writeValueAsString(card.keywords());
      Object[] args = { card.id(), card.name(), card.slug(), card.description(),
          card.type().name(), card.rarity().name(), card.manaCost(), card.attack(), card.health(),
          card.heroClass().name(), card.imagePath(), effects, keywords, card.collectible() ? 1 : 0,
          now, now };
      int updated = jdbc.update("UPDATE Card SET name=?, slug=?, description=?, type=?, rarity=?,"
          + " manaCost=?, attack=?, health=?, heroClass=?, imagePath=?, effects=?, keywords=?,"
          + " collectible=?, updatedAt=? WHERE slug=?",
          args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
          args[10], args[11], args[12], args[13], args[15], card.slug());
      if (updated == 0) {
        jdbc.update("INSERT INTO Card (id, name, slug, description, type, rarity, manaCost, attack,"
            + " health, heroClass, imagePath, effects, keywords, collectible, createdAt, updatedAt)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", (Object[]) args);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Seed card lỗi.", e);
    }
  }

  public void upsertHero(JdbcTemplate jdbc, String id, String name, HeroClass heroClass,
      String powerName, int powerCost, String imagePath) {
    int updated = jdbc.update("UPDATE Hero SET name=?, powerName=?, powerCost=?, imagePath=?"
        + " WHERE heroClass=?", name, powerName, powerCost, imagePath, heroClass.name());
    if (updated == 0) {
      jdbc.update("INSERT INTO Hero (id, name, heroClass, powerName, powerCost, imagePath)"
          + " VALUES (?, ?, ?, ?, ?, ?)", id, name, heroClass.name(), powerName, powerCost, imagePath);
    }
  }

}
