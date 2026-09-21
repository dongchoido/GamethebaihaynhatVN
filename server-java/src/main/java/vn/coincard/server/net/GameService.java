package vn.coincard.server.net;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import vn.coincard.server.db.Repositories;
import vn.coincard.server.db.Repositories.CatalogRepository;
import vn.coincard.server.db.Repositories.GamePlayerInput;
import vn.coincard.server.db.Repositories.GameRepository;
import vn.coincard.server.game.CardTypes;
import vn.coincard.server.game.Constants;
import vn.coincard.server.game.Deck;
import vn.coincard.server.game.EffectResolver;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.GameEngine;
import vn.coincard.server.game.GameException;
import vn.coincard.server.game.Hero;
import vn.coincard.server.game.Player;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.room.RoomPlayer;

/** Coordinates authentication, rooms, state broadcasts and result persistence — SRP: gameplay. */
@Service
public class GameService {
  private final GameEngine engine = new GameEngine();
  private final Map<String, String> gameIndex = new ConcurrentHashMap<>(); // roomCode -> gameId

  private final RoomManager roomManager;
  private final GameRepository gameRepository;
  private final CatalogRepository catalog;
  private final vn.coincard.server.service.GamePersistenceService persistence;
  private final vn.coincard.server.service.RoomService roomService;
  private MessageSender sender = new MessageSender() {
    @Override public void toRoom(String r, String e, Object d) {}
    @Override public void toSession(String s, String e, Object d) {}
    @Override public void disconnectSession(String s) {}
  };

  public GameService(RoomManager roomManager, GameRepository gameRepository,
      CatalogRepository catalog, vn.coincard.server.service.GamePersistenceService persistence,
      vn.coincard.server.service.RoomService roomService) {
    this.roomManager = roomManager;
    this.gameRepository = gameRepository;
    this.catalog = catalog;
    this.persistence = persistence;
    this.roomService = roomService;
  }

  public void setSender(MessageSender sender) {
    this.sender = sender;
    this.roomService.setSender(sender);
  }
  public RoomManager rooms() { return roomManager; }

  @jakarta.annotation.PostConstruct
  public void startCleanup() {
    ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "room-cleanup");
      t.setDaemon(true);
      return t;
    });
    timer.scheduleAtFixedRate(() -> {
      try {
        for (Room room : roomManager.listRooms()) {
          if (!room.isIdle(Constants.ROOM_IDLE_TTL_MS)) continue;
          String gameId = gameIndex.get(room.getRoomCode());
          if (gameId != null) {
            engine.removeGame(gameId);
            gameIndex.remove(room.getRoomCode());
            persistence.removeGame(gameId);
          }
          roomManager.deleteRoom(room.getRoomCode());
        }
      } catch (RuntimeException ignored) {
        // Cleanup must never crash the server.
      }
    }, Constants.ROOM_SWEEP_INTERVAL_MS, Constants.ROOM_SWEEP_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }

  // ---------- rooms — delegate sang RoomService để thể hiện SRP

  public void createRoom(String socketId, Map<String, Object> session, String playerName) {
    roomService.createRoom(socketId, session, playerName);
  }

  public void joinRoom(String socketId, Map<String, Object> session, String roomCode, String playerName) {
    roomService.joinRoom(socketId, session, roomCode, playerName);
  }

  public void selectDeck(String socketId, Map<String, Object> session, String heroId, String roomCode) {
    Room room = roomManager.getRoom(roomCode);
    synchronized (room) {
      if (room.isStarted() || room.isStarting()) return;
      RoomPlayer player = room.getPlayerBySession(tokenOf(session));
      if (player == null || !socketId.equals(player.socketId)) {
        throw new IllegalArgumentException("Player không tồn tại trong room.");
      }
      if (!List.of("MAGE", "HUNTER", "PALADIN", "PRIEST", "WARLOCK").contains(heroId)) {
        throw new IllegalArgumentException("Hero không hợp lệ.");
      }
      room.selectHero(player.playerId, heroId);
      if (room.isFull() && room.getPlayers().stream().allMatch(p -> p.ready) && room.tryStart()) {
        CompletableFuture.runAsync(() -> startGame(room)).whenComplete((v, error) -> {
          if (error != null) {
            room.cancelStart();
            room.clearReady();
            System.err.println("Không thể khởi tạo trận: " + errorMessage(error));
            sender.toRoom(room.getRoomCode(), "ACTION_REJECTED", Map.of(
                "code", "GAME_START_FAILED",
                "message", errorMessage(error)));
          }
        });
      }
    }
  }

  private void startGame(Room room) {
    String gameId = UUID.randomUUID().toString();
    List<Repositories.HeroRecord> heroes = catalog.heroes();
    List<CardTypes.CardDefinition> cardCatalog = catalog.cards();
    if (cardCatalog.size() < Constants.DECK_SIZE) {
      throw new IllegalStateException("Catalog chưa đủ bài. Hãy kiểm tra seed.");
    }
    List<RoomPlayer> players = room.getPlayers();
    if (players.size() < 2) throw new IllegalStateException("Room cần đủ 2 player để start.");
    Game game = engine.createGame(gameId, room.getRoomCode(),
        buildPlayerInit(players.get(0), heroes, cardCatalog),
        buildPlayerInit(players.get(1), heroes, cardCatalog));
    game.start();
    room.markStarted();
    gameIndex.put(room.getRoomCode(), gameId);
    synchronized (game) {
      sender.toRoom(room.getRoomCode(), "GAME_STARTED", Map.of("gameId", gameId));
      broadcastState(room, game);
    }
  }

  private GameEngine.PlayerInit buildPlayerInit(RoomPlayer roomPlayer,
      List<Repositories.HeroRecord> heroes, List<CardTypes.CardDefinition> cardCatalog) {
    if (heroes.isEmpty()) throw new IllegalStateException("Chưa seed heroes.");
    Repositories.HeroRecord heroRecord = heroes.stream()
        .filter(h -> h.id().equals(roomPlayer.heroClass) || h.heroClass().equals(roomPlayer.heroClass))
        .findFirst().orElse(heroes.get(0));
    List<CardTypes.CardDefinition> deckCards = cardCatalog.stream()
        .sorted((a, b) -> Integer.compare(a.manaCost(), b.manaCost()))
        .limit(Constants.DECK_SIZE)
        .map(c -> new CardTypes.CardDefinition(
            c.id() + "-" + UUID.randomUUID(), c.name(), c.slug(), c.description(),
            c.type(), c.rarity(), c.manaCost(), c.attack(), c.health(), c.heroClass(),
            c.imagePath(), c.effects(), c.keywords(), true))
        .collect(Collectors.toList());
    Hero hero = new Hero(heroRecord.id(), heroRecord.name(), heroRecord.heroClass(),
        heroRecord.powerName(), heroRecord.powerCost(), heroRecord.imagePath());
    return new GameEngine.PlayerInit(roomPlayer.playerId, roomPlayer.name, hero, new Deck(deckCards));
  }

  // ---------- actions ----------

  public void playCard(String socketId, Map<String, Object> session,
      String gameId, String cardInstanceId, String targetId) {
    executeAction(socketId, session, gameId,
        playerId -> engine.playCard(gameId, playerId, cardInstanceId, targetId));
  }

  public void attack(String socketId, Map<String, Object> session,
      String gameId, String attackerId, String targetId) {
    executeAction(socketId, session, gameId,
        playerId -> engine.attack(gameId, playerId, attackerId, targetId));
  }

  public void endTurn(String socketId, Map<String, Object> session, String gameId) {
    executeAction(socketId, session, gameId, playerId -> engine.endTurn(gameId, playerId));
  }

  public void useHeroPower(String socketId, Map<String, Object> session, String gameId) {
    executeAction(socketId, session, gameId, playerId -> engine.useHeroPower(gameId, playerId));
  }

  public void drawCard(String socketId, Map<String, Object> session, String gameId) {
    executeAction(socketId, session, gameId, playerId -> engine.drawCard(gameId, playerId));
  }

  public void concede(String socketId, Map<String, Object> session, String gameId) {
    executeAction(socketId, session, gameId, playerId -> engine.concede(gameId, playerId));
  }

  private void executeAction(String socketId, Map<String, Object> session, String gameId,
      GameAction action) {
    Game game = engine.getGame(gameId);
    Room room = roomManager.getRoom(game.getRoomCode());
    room.touch();
    synchronized (game) {
      try {
        action.run(playerIdFromGame(game, socketId, session));
      } catch (RuntimeException error) {
        respondWithError(socketId, error);
        return;
      }
      broadcastState(room, game);
    }
  }

  @FunctionalInterface
  private interface GameAction {
    void run(String playerId);
  }

  public void handleDisconnect(String socketId) {
    Room room = roomManager.findRoomBySocketId(socketId);
    roomManager.unindexSocket(socketId);
    if (room == null) return;
    RoomPlayer leftPlayer = room.removeSocket(socketId);
    if (leftPlayer == null) return;
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("playerId", leftPlayer.playerId);
    sender.toRoom(room.getRoomCode(), "PLAYER_DISCONNECTED", data);
  }

  public void reconnect(String socketId, Map<String, Object> session, String sessionToken) {
    Room existing = roomManager.findRoomBySocketId(socketId);
    if (existing != null) {
      boolean mine = existing.getPlayers().stream()
          .anyMatch(p -> socketId.equals(p.socketId) && sessionToken.equals(p.sessionToken));
      if (!mine) throw new GameException.ReconnectFailed();
    }
    Room room = roomManager.findRoomBySessionToken(sessionToken);
    if (room == null) throw new GameException.ReconnectFailed();
    RoomPlayer player = room.getPlayerBySession(sessionToken);
    if (player == null) throw new GameException.ReconnectFailed();
    if (player.socketId != null && !player.socketId.equals(socketId)) {
      roomManager.unindexSocket(player.socketId);
      sender.disconnectSession(player.socketId);
    }
    room.bindSocket(player.playerId, socketId);
    session.put("sessionToken", sessionToken);
    roomManager.indexPlayer(room, room.getPlayerBySession(sessionToken));
    room.touch();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("roomCode", room.getRoomCode());
    data.put("players", roomSnapshot(room));
    sender.toRoom(room.getRoomCode(), "PLAYER_JOINED", data);

    String gameId = gameIndex.get(room.getRoomCode());
    if (gameId != null) {
      Game game = engine.getGame(gameId);
      synchronized (game) {
        sender.toSession(socketId, "GAME_STATE_UPDATED",
            Map.of("gameState", toStateFor(game, player.playerId)));
      }
    }
  }

  public void rematch(String socketId, Map<String, Object> session, String gameId) {
    Game game = engine.getGame(gameId);
    if (!game.isFinished()) {
      sender.toSession(socketId, "ACTION_REJECTED", Map.of(
          "code", "REMATCH_NOT_ALLOWED",
          "message", "Trận chưa kết thúc, không thể tái đấu."));
      return;
    }
    Room room = roomManager.getRoom(game.getRoomCode());
    if (room.isStarting()) {
      sender.toSession(socketId, "ACTION_REJECTED", Map.of(
          "code", "REMATCH_IN_PROGRESS",
          "message", "Đang tạo trận tái đấu, vui lòng đợi."));
      return;
    }
    String playerId = playerIdFromGame(game, socketId, session);
    int votes = room.voteRematch(playerId);
    if (votes < 2) return;
    if (!room.tryStartRematch()) {
      sender.toSession(socketId, "ACTION_REJECTED", Map.of(
          "code", "REMATCH_IN_PROGRESS",
          "message", "Đang tạo trận tái đấu, vui lòng đợi."));
      return;
    }
    String oldGameId = game.getGameId();
    CompletableFuture.runAsync(() -> startGame(room)).whenComplete((v, error) -> {
      if (error == null) {
        engine.removeGame(oldGameId);
        persistence.removeGame(oldGameId);
      } else {
        room.cancelStart();
        sender.toRoom(room.getRoomCode(), "ACTION_REJECTED", Map.of(
            "code", "REMATCH_FAILED",
            "message", errorMessage(error)));
      }
    });
  }

  // ---------- state ----------

  private List<Map<String, Object>> roomSnapshot(Room room) {
    return vn.coincard.server.mapper.GameStateMapper.toRoomSnapshot(room);
  }

  Map<String, Object> toStateFor(Game game, String viewerPlayerId) {
    return vn.coincard.server.mapper.GameStateMapper.toGameStateFor(game, viewerPlayerId);
  }

  private Map<String, Object> playerToState(Player player, boolean isOwner) {
    return vn.coincard.server.mapper.GameStateMapper.toPlayerState(player, isOwner);
  }

  private void broadcastState(Room room, Game game) {
    for (RoomPlayer rp : room.getPlayers()) {
      if (rp.socketId == null) continue;
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("gameState", toStateFor(game, rp.playerId));
      sender.toSession(rp.socketId, "GAME_STATE_UPDATED", data);
    }
    if (game.isFinished()) {
      if (persistence.shouldSave(game.getGameId())) {
        persistence.saveGame(game, room, gameIdForRoom(room.getRoomCode()), 0);
      }
      if (persistence.shouldEmitGameOver(game.getGameId())) {
        persistence.markGameOverEmitted(game.getGameId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("winnerId", game.getWinnerId());
        sender.toRoom(room.getRoomCode(), "GAME_OVER", data);
      }
    }
  }

  private void saveGame(Game game, Room room, int attempt) {
    persistence.saveGame(game, room, gameIdForRoom(room.getRoomCode()), attempt);
  }

  private String gameIdForRoom(String roomCode) {
    return gameIndex.get(roomCode);
  }

  private String tokenOf(Map<String, Object> session) {
    Object token = session.get("sessionToken");
    return token instanceof String s ? s : "";
  }

  private String playerIdFromGame(Game game, String socketId, Map<String, Object> session) {
    Room room = roomManager.getRoom(game.getRoomCode());
    String current = gameIndex.get(room.getRoomCode());
    if (current == null || !current.equals(game.getGameId())) {
      throw new IllegalArgumentException("Trận này không còn hoạt động.");
    }
    RoomPlayer roomPlayer = room.getPlayerBySession(tokenOf(session));
    if (roomPlayer == null || !socketId.equals(roomPlayer.socketId)) {
      throw new IllegalArgumentException("Player không tồn tại trong room.");
    }
    return roomPlayer.playerId;
  }

  private void respondWithError(String socketId, RuntimeException error) {
    Map<String, Object> data = new LinkedHashMap<>();
    if (error instanceof GameException ge) {
      data.put("code", ge.code());
      data.put("message", ge.getMessage());
    } else {
      data.put("code", "UNKNOWN");
      data.put("message", error.getMessage() != null ? error.getMessage() : "Unknown error.");
    }
    sender.toSession(socketId, "ACTION_REJECTED", data);
  }

  private String errorMessage(Throwable error) {
    Throwable cause = error.getCause() != null ? error.getCause() : error;
    return cause.getMessage() != null ? cause.getMessage() : "Không thể khởi tạo trận.";
  }

  private void broadcastExcept(Room room, String exceptSocketId, String event, Object data) {
    for (RoomPlayer p : room.getPlayers()) {
      if (p.socketId != null && !p.socketId.equals(exceptSocketId)) {
        sender.toSession(p.socketId, event, data);
      }
    }
  }
}
