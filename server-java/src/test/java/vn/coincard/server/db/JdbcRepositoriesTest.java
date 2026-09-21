package vn.coincard.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import vn.coincard.server.db.Repositories.GamePlayerInput;

class JdbcRepositoriesTest {
  @TempDir
  Path tempDir;

  @Test
  void savesRematchesInTheSameRoom() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.sqlite.JDBC");
    dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("test.db"));
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    JdbcRepositories.initSchema(jdbc);
    JdbcRepositories.JdbcGameRepository repository =
        new JdbcRepositories.JdbcGameRepository(jdbc);
    List<GamePlayerInput> players = List.of(
        new GamePlayerInput("p1", "One", true),
        new GamePlayerInput("p2", "Two", false));

    repository.recordFinishedGame("game-1", "ABC123", players, "p1");
    repository.recordFinishedGame("game-2", "ABC123", players, "p2");

    assertEquals(2, jdbc.queryForObject(
        "SELECT COUNT(*) FROM Game WHERE roomCode='ABC123'", Integer.class));
    assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM GamePlayer", Integer.class));
  }
}
