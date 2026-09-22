package vn.coincard.server.catalog;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.coincard.server.db.Repositories;
import vn.coincard.server.db.Repositories.CatalogRepository;
import vn.coincard.server.game.CardTypes;

/** Immutable in-memory catalog loaded once after database seeding. */
@Component
public final class CatalogCache {
  private final CatalogRepository repository;
  private volatile List<Repositories.HeroRecord> heroes = List.of();
  private volatile List<CardTypes.CardDefinition> cards = List.of();

  public CatalogCache(CatalogRepository repository) {
    this.repository = repository;
  }

  public void reload() {
    heroes = List.copyOf(repository.heroes());
    cards = List.copyOf(repository.cards());
  }

  public List<Repositories.HeroRecord> heroes() {
    return heroes;
  }

  public List<CardTypes.CardDefinition> cards() {
    return cards;
  }
}
