package vn.coincard.server.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Creates the schema and synchronizes the card/hero catalog from data/cards.json. */
@Component
public class DatabaseSeeder implements CommandLineRunner {
  private final JdbcTemplate jdbc;
  private final JdbcRepositories repos;
  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${coincard.cards-path}")
  private String cardsPath;

  public DatabaseSeeder(JdbcTemplate jdbc, JdbcRepositories repos) {
    this.jdbc = jdbc;
    this.repos = repos;
  }

  @Override
  public void run(String... args) throws Exception {
    JdbcRepositories.initSchema(jdbc);
    boolean firstSeed = repos.countCards(jdbc) == 0 || repos.countHeroes(jdbc) == 0;
    List<Map<String, Object>> cards = mapper.readValue(
        Files.readString(Path.of(cardsPath)), new TypeReference<List<Map<String, Object>>>() {});
    String now = Instant.now().toString();
    for (Map<String, Object> card : cards) {
      repos.upsertCard(jdbc, card, now);
    }
    String[][] heroes = {
        {"hero_mage", "Jaina Proudmoore", "MAGE", "Fireblast", "2", "assets/images/Heros/Jaina Proudmoore.png"},
        {"hero_hunter", "Rexxar", "HUNTER", "Steady Shot", "2", "assets/images/Heros/Rexxar.png"},
        {"hero_paladin", "Uther Lightbringer", "PALADIN", "Reinforce", "2", "assets/images/Heros/Uther Lightbringer.png"},
        {"hero_priest", "Anduin Wrynn", "PRIEST", "Lesser Heal", "2", "assets/images/Heros/Anduin-Wrynn.png"},
        {"hero_warlock", "Gul'dan", "WARLOCK", "Life Tap", "2", "assets/images/Heros/Gul'dan.png"},
    };
    for (String[] h : heroes) {
      repos.upsertHero(jdbc, h[0], h[1], h[2], h[3], Integer.parseInt(h[4]), h[5]);
    }
    if (firstSeed) {
      System.out.println("Seeded " + cards.size() + " cards and " + heroes.length + " heroes.");
    }
  }
}
