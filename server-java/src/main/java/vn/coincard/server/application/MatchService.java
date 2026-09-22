package vn.coincard.server.application;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import vn.coincard.server.catalog.CatalogCache;
import vn.coincard.server.db.Repositories;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.Deck;
import vn.coincard.server.game.DeckFactory;
import vn.coincard.server.game.GameFactory;
import vn.coincard.server.game.GameEngine;
import vn.coincard.server.game.GameSessionRegistry;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.game.RandomSource;
import vn.coincard.server.mapper.GameStateDto;
import vn.coincard.server.mapper.GameStateMapper;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomPlayer;

/** Builds and starts a fresh aggregate from two previously validated room loadouts. */
@Service
public final class MatchService {
  private final CatalogCache catalog;
  private final RandomSource random;
  private final GameFactory gameFactory;
  private final GameEngine engine;
  private final GameSessionRegistry sessions;
  private final ActiveGameRegistry activeGames;

  public MatchService(CatalogCache catalog, RandomSource random, GameFactory gameFactory,
      GameEngine engine, GameSessionRegistry sessions, ActiveGameRegistry activeGames) {
    this.catalog = catalog;
    this.random = random;
    this.gameFactory = gameFactory;
    this.engine = engine;
    this.sessions = sessions;
    this.activeGames = activeGames;
  }

  public StartedMatch start(Room room) {
    String gameId = UUID.randomUUID().toString();
    List<RoomPlayer> players = room.getPlayers();
    if (players.size() != 2) throw new IllegalStateException("Room cần đủ hai player để start.");

    List<Repositories.HeroRecord> heroes = catalog.heroes();
    List<CardTypes.CardDefinition> cards = catalog.cards();
    var game = gameFactory.create(gameId, room.getRoomCode(),
        buildPlayerInit(players.get(0), heroes, cards), buildPlayerInit(players.get(1), heroes, cards));
    sessions.register(game);
    try {
      RoomPlayer first = players.get(random.nextInt(players.size()));
      List<ViewerState> viewers = sessions.withLockedGame(gameId, lockedGame -> {
        engine.start(lockedGame, first.playerId());
        return room.getPlayers().stream()
            .filter(player -> player.socketId() != null)
            .map(player -> new ViewerState(player.socketId(),
                GameStateMapper.toGameStateFor(lockedGame, player.playerId())))
            .toList();
      });
      room.markStarted();
      activeGames.associate(room.getRoomCode(), gameId);
      return new StartedMatch(gameId, viewers);
    } catch (RuntimeException error) {
      sessions.remove(gameId);
      throw error;
    }
  }

  private GameFactory.PlayerInit buildPlayerInit(RoomPlayer roomPlayer,
      List<Repositories.HeroRecord> heroes, List<CardTypes.CardDefinition> cardCatalog) {
    if (heroes.isEmpty()) throw new IllegalStateException("Chưa seed heroes.");
    HeroClass selectedClass = roomPlayer.heroClass();
    if (selectedClass == null) throw new IllegalStateException("Chưa chọn hero.");
    Repositories.HeroRecord heroRecord = heroes.stream()
        .filter(hero -> hero.heroClass() == selectedClass)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Hero không có trong catalog."));
    List<CardTypes.CardDefinition> selectedCards = DeckFactory.validate(selectedClass,
        roomPlayer.cardSlugs(), cardCatalog);
    List<CardTypes.CardDefinition> deckCards = selectedCards.stream()
        .map(card -> new CardTypes.CardDefinition(
            card.id() + "-" + UUID.randomUUID(), card.name(), card.slug(), card.description(),
            card.type(), card.rarity(), card.manaCost(), card.attack(), card.health(),
            card.heroClass(), card.imagePath(), card.effects(), card.keywords(), true))
        .collect(Collectors.toList());
    Hero hero = new Hero(heroRecord.id(), heroRecord.name(), heroRecord.heroClass(),
        heroRecord.powerName(), heroRecord.powerCost(), heroRecord.imagePath());
    return new GameFactory.PlayerInit(roomPlayer.playerId(), roomPlayer.name(), hero,
        new Deck(deckCards, random));
  }

  public record ViewerState(String socketId, GameStateDto.GameState state) {}

  public record StartedMatch(String gameId, List<ViewerState> viewers) {
    public StartedMatch {
      viewers = List.copyOf(viewers);
    }
  }
}
