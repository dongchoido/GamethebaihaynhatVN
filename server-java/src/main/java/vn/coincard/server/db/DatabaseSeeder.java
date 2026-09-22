package vn.coincard.server.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.coincard.server.catalog.CatalogCache;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.web.ApplicationReadiness;

/** Creates the schema and synchronizes the card/hero catalog from data/cards.json. */
@Component
public class DatabaseSeeder implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
  private final JdbcTemplate jdbc;
  private final JdbcRepositories repos;
  private final CatalogCache catalogCache;
  private final ApplicationReadiness readiness;
  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${coincard.cards-path}")
  private String cardsPath;

  public DatabaseSeeder(JdbcTemplate jdbc, JdbcRepositories repos, CatalogCache catalogCache,
      ApplicationReadiness readiness) {
    this.jdbc = jdbc;
    this.repos = repos;
    this.catalogCache = catalogCache;
    this.readiness = readiness;
  }

  @Override
  public void run(String... args) throws Exception {
    JdbcRepositories.initSchema(jdbc);
    boolean firstSeed = repos.countCards(jdbc) == 0 || repos.countHeroes(jdbc) == 0;
    List<CatalogSeedCard> cards = mapper.readValue(
        Files.readString(Path.of(cardsPath)),
        mapper.getTypeFactory().constructCollectionType(List.class, CatalogSeedCard.class));
    String now = Instant.now().toString();
    for (CatalogSeedCard card : cards) {
      repos.upsertCard(jdbc, card.toCardDefinition(), now);
    }
    Object[][] heroes = {
        {"hero_mage", "Jaina Proudmoore", HeroClass.MAGE, "Fireblast", 2, "assets/images/Heros/Jaina Proudmoore.png"},
        {"hero_hunter", "Rexxar", HeroClass.HUNTER, "Steady Shot", 2, "assets/images/Heros/Rexxar.png"},
        {"hero_paladin", "Uther Lightbringer", HeroClass.PALADIN, "Reinforce", 2, "assets/images/Heros/Uther Lightbringer.png"},
        {"hero_priest", "Anduin Wrynn", HeroClass.PRIEST, "Lesser Heal", 2, "assets/images/Heros/Anduin-Wrynn.png"},
        {"hero_warlock", "Gul'dan", HeroClass.WARLOCK, "Life Tap", 2, "assets/images/Heros/Gul'dan.png"},
    };
    for (Object[] h : heroes) {
      repos.upsertHero(jdbc, (String) h[0], (String) h[1], (HeroClass) h[2], (String) h[3],
          (Integer) h[4], (String) h[5]);
    }
    if (firstSeed) {
      log.info("Seeded {} cards and {} heroes.", cards.size(), heroes.length);
    }
    catalogCache.reload();
    readiness.markReady();
  }
}
