package vn.coincard.server.game;

import java.util.List;
import java.util.UUID;

/** Stateless domain service that validates and applies one command to one aggregate. */
public final class GameEngine {
  private final EffectResolver resolver;
  private final HeroPowerRegistry heroPowerRegistry;

  public GameEngine() {
    this(new EffectResolver(), new HeroPowerRegistry(HeroPower.defaults()));
  }

  public GameEngine(EffectResolver resolver, HeroPowerRegistry heroPowerRegistry) {
    this.resolver = resolver;
    this.heroPowerRegistry = heroPowerRegistry;
  }

  /** Starts a waiting aggregate; session ownership remains with the application layer. */
  public void start(Game game, String firstPlayerId) {
    game.start(firstPlayerId);
  }

  void execute(Game game, GameCommand command) {
    if (!game.getGameId().equals(command.gameId())) {
      throw new GameException.InvalidCommand("Command không thuộc trận hiện tại.");
    }
    ensurePlaying(game);
    if (command instanceof GameCommand.PlayCard playCard) {
      playCard(game, playCard.playerId(), playCard.cardInstanceId(), playCard.targetId());
    } else if (command instanceof GameCommand.Attack attack) {
      attack(game, attack.playerId(), attack.attackerId(), attack.targetId());
    } else if (command instanceof GameCommand.UseHeroPower useHeroPower) {
      useHeroPower(game, useHeroPower.playerId());
    } else if (command instanceof GameCommand.EndTurn endTurn) {
      endTurn(game, endTurn.playerId());
    } else if (command instanceof GameCommand.Concede concede) {
      concede(game, concede.playerId());
    } else {
      throw new GameException.InvalidCommand("Command chưa được hỗ trợ.");
    }
  }

  void playCard(Game game, String playerId, String cardInstanceId, String targetId) {
    assertActivePlayer(game, playerId);
    Player player = game.getPlayerById(playerId);
    Player opponent = game.getOpponent();

    CardTypes.CardDefinition card = player.findCardInHand(cardInstanceId);
    if (card == null) throw new GameException.CardNotInHand();
    if (card.manaCost() > player.currentMana()) throw new GameException.NotEnoughMana();
    if (card.type() == CardType.MINION && player.boardCount() >= Constants.MAX_BOARD_SIZE) {
      throw new GameException.BoardFull();
    }
    if (card.type() != CardType.MINION) {
      for (CardTypes.EffectDefinition effect : card.effects()) {
        resolver.validate(player, opponent, effect, targetForEffect(effect, targetId));
      }
    }

    GameMemento memento = GameMemento.capture(game);
    try {
      player.removeFromHand(cardInstanceId);
      player.spendMana(card.manaCost());
      player.recordCardPlayed();

      if (card.type() == CardType.MINION) {
        player.summonMinion(new Minion(UUID.randomUUID().toString(),
            card.id(), card.name(), card.attack(), card.health(), player.id(),
            card.keywords().contains(Keyword.CHARGE), card.imagePath(),
            card.keywords().contains(Keyword.TAUNT)));
      } else {
        for (CardTypes.EffectDefinition effect : card.effects()) {
          resolver.resolve(player, opponent, effect, targetForEffect(effect, targetId));
        }
        player.removeDeadMinions();
        opponent.removeDeadMinions();
      }
      checkWinner(game);
    } catch (RuntimeException error) {
      memento.restore();
      throw error;
    }
  }

  void attack(Game game, String playerId, String attackerId, String targetId) {
    Player player = game.getPlayerById(playerId);
    Player opponent = game.getOpponent();
    assertActivePlayer(game, playerId);
    CombatService.resolveAttack(player, opponent, attackerId, targetId);
    player.removeDeadMinions();
    opponent.removeDeadMinions();
    checkWinner(game);
  }

  void endTurn(Game game, String playerId) {
    assertActivePlayer(game, playerId);
    game.switchTurn();
    checkWinner(game);
  }

  void useHeroPower(Game game, String playerId) {
    Player player = game.getPlayerById(playerId);
    Hero hero = player.heroState();
    assertActivePlayer(game, playerId);
    if (player.currentMana() < hero.getPowerCost()) throw new GameException.NotEnoughMana();
    HeroPower power = heroPowerRegistry.forClass(hero.getHeroClass());
    power.validate(player);
    GameMemento memento = GameMemento.capture(game);
    try {
      player.markHeroPowerUsed();
      player.spendMana(hero.getPowerCost());
      power.execute(game, player);
      checkWinner(game);
    } catch (RuntimeException error) {
      memento.restore();
      throw error;
    }
  }

  void concede(Game game, String playerId) {
    game.resign(playerId);
  }

  private void ensurePlaying(Game game) {
    if (game.status() != GameStatus.PLAYING) throw new GameException.GameNotRunning();
  }

  private void assertActivePlayer(Game game, String playerId) {
    if (!game.getActivePlayerId().equals(playerId)) throw new GameException.NotPlayerTurn();
  }

  private String targetForEffect(CardTypes.EffectDefinition effect, String targetId) {
    return CardTypes.effectNeedsTarget(effect.target()) ? targetId : null;
  }

  private void checkWinner(Game game) {
    List<Player> players = game.getPlayers();
    if (players.stream().allMatch(player -> player.heroState().isDead())) {
      game.finish(null, "DRAW");
      return;
    }
    for (Player player : players) {
      if (player.heroState().isDead()) {
        String winner = players.stream().filter(candidate -> !candidate.id().equals(player.id()))
            .findFirst().map(Player::id).orElse(null);
        game.finish(winner, "");
        return;
      }
    }
  }
}
