package vn.coincard.server.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Mirror of server/src/game/GameEngine.ts */
public class GameEngine {
  private final Map<String, Game> games = new ConcurrentHashMap<>();
  private final EffectResolver resolver = new EffectResolver();

  public Game createGame(String gameId, String roomCode, PlayerInit p1, PlayerInit p2) {
    Game game = new Game(gameId, roomCode,
        new Player(p1.playerId(), p1.name(), p1.hero(), p1.deck()),
        new Player(p2.playerId(), p2.name(), p2.hero(), p2.deck()));
    games.put(gameId, game);
    return game;
  }

  public record PlayerInit(String playerId, String name, Hero hero, Deck deck) {}

  public void playCard(String gameId, String playerId, String cardInstanceId, String targetId) {
    Game game = ensurePlaying(gameId);
    assertActivePlayer(game, playerId);
    Player player = game.getPlayerById(playerId);
    Player opponent = game.getOpponent();

    // PHASE 1: VALIDATE — no mutation.
    CardTypes.CardDefinition card = player.findCardInHand(cardInstanceId);
    if (card == null) throw new GameException.CardNotInHand();
    if (card.manaCost() > player.currentMana()) throw new GameException.NotEnoughMana();
    if ("MINION".equals(card.type()) && player.boardCount() >= Constants.MAX_BOARD_SIZE) {
      throw new GameException.BoardFull();
    }
    if (!"MINION".equals(card.type())) {
      for (CardTypes.EffectDefinition effect : card.effects()) {
        resolver.validate(player, opponent, effect, targetForEffect(effect, targetId));
      }
    }

    List<Runnable> undo = List.of(player.checkpoint(), opponent.checkpoint());
    try {
      // PHASE 2: COMMIT.
      player.removeFromHand(cardInstanceId);
      player.spendMana(card.manaCost());
      player.recordCardPlayed();

      if ("MINION".equals(card.type())) {
        player.summonMinion(new Minion(UUID.randomUUID().toString(),
            card.id(), card.name(), card.attack(), card.health(), player.id(),
            card.keywords().contains("CHARGE"), card.imagePath(),
            card.keywords().contains("TAUNT")));
      } else {
        for (CardTypes.EffectDefinition effect : card.effects()) {
          resolver.resolve(game, player, opponent, effect, targetForEffect(effect, targetId));
        }
        player.removeDeadMinions();
        opponent.removeDeadMinions();
      }
      checkWinner(game);
    } catch (RuntimeException error) {
      undo.forEach(Runnable::run);
      throw error;
    }
  }

  public void attack(String gameId, String playerId, String attackerId, String targetId) {
    Game game = ensurePlaying(gameId);
    Player player = game.getPlayerById(playerId);
    Player opponent = game.getOpponent();
    assertActivePlayer(game, playerId);
    CombatService.resolveAttack(player, opponent, attackerId, targetId);
    player.removeDeadMinions();
    opponent.removeDeadMinions();
    checkWinner(game);
  }

  public void endTurn(String gameId, String playerId) {
    Game game = ensurePlaying(gameId);
    assertActivePlayer(game, playerId);
    game.switchTurn();
  }

  public void drawCard(String gameId, String playerId) {
    Game game = ensurePlaying(gameId);
    assertActivePlayer(game, playerId);
    Player player = game.getPlayerById(playerId);
    if (player.handCount() >= Constants.MAX_HAND_SIZE) throw new GameException.HandFull();
    if (player.deckSize() <= 0) throw new GameException.DeckEmpty();
    if (game.hasManualDrawnThisTurn()) throw new GameException.AlreadyDrew();
    player.addToHand(player.drawCard());
    game.markManualDraw();
  }

  public void useHeroPower(String gameId, String playerId) {
    Game game = ensurePlaying(gameId);
    Player player = game.getPlayerById(playerId);
    Hero hero = player.heroState();
    assertActivePlayer(game, playerId);
    if (player.currentMana() < hero.powerCost) throw new GameException.NotEnoughMana();
    HeroPower power = HeroPower.forClass(hero.heroClass);
    power.validate(player);
    List<Runnable> undo = new ArrayList<>();
    for (Player p : game.getPlayers()) undo.add(p.checkpoint());
    try {
      player.spendMana(hero.powerCost);
      power.execute(game, player);
      checkWinner(game);
    } catch (RuntimeException error) {
      undo.forEach(Runnable::run);
      throw error;
    }
  }

  public void concede(String gameId, String playerId) {
    Game game = ensurePlaying(gameId);
    game.resign(playerId);
  }

  public Game getGame(String gameId) {
    Game game = games.get(gameId);
    if (game == null) throw new IllegalArgumentException("Game không tồn tại.");
    return game;
  }

  public void removeGame(String gameId) {
    games.remove(gameId);
  }

  private Game ensurePlaying(String gameId) {
    Game game = getGame(gameId);
    if (!"PLAYING".equals(game.getStatus())) throw new GameException.GameNotRunning();
    return game;
  }

  private void assertActivePlayer(Game game, String playerId) {
    if (!game.getActivePlayerId().equals(playerId)) throw new GameException.NotPlayerTurn();
  }

  private String targetForEffect(CardTypes.EffectDefinition effect, String targetId) {
    return CardTypes.effectNeedsTarget(effect.target()) ? targetId : null;
  }

  private void checkWinner(Game game) {
    List<Player> players = game.getPlayers();
    if (players.stream().allMatch(p -> p.heroState().isDead())) {
      game.finish(null, "DRAW");
      return;
    }
    for (Player p : players) {
      if (p.heroState().isDead()) {
        String winner = players.stream().filter(x -> !x.id().equals(p.id()))
            .findFirst().map(Player::id).orElse(null);
        game.finish(winner, "");
        return;
      }
    }
  }
}
