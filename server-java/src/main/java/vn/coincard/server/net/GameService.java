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

/**
 * Mirror of server/src/network/GameService.ts.
 * Gameplay rules live in GameEngine/domain; this class does auth, rooms, broadcast, persistence.
 */
@Service
public class GameService {
  private final GameEngine engine = new GameEngine();
  private final Map<String, String> gameIndex = new ConcurrentHashMap<>(); // roomCode -> gameId
  private final Set<String> savedGames = ConcurrentHashMap.newKeySet();
  private final Set<String> pendingSaves = ConcurrentHashMap.newKeySet();
  private final Set<String> gameOverEmitted = ConcurrentHashMap.newKeySet();
  private final Map<String, Integer> lastTurnBroadcast = new ConcurrentHashMap<>();

  private final RoomManager roomManager;
  private final GameRepository gameRepository;
  private final CatalogRepository catalog;
  private MessageSender sender = new MessageSender() {
    @Override public void toRoom(String r, String e, Object d) {}
    @Override public void toSession(String s, String e, Object d) {}
    @Override public void disconnectSession(String s) {}
  };

  public GameService(RoomManager roomManager, GameRepository gameRepository,
      CatalogRepository catalog) {
    this.roomManager = roomManager;
    this.gameRepository = gameRepository;
    this.catalog = catalog;
  }

  public void setSender(MessageSender sender) { this.sender = sender; }
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
          String gameId = gameIndex.get(room.roomCode);
          if (gameId != null) {
            engine.removeGame(gameId);
            gameIndex.remove(room.roomCode);
            savedGames.remove(gameId);
            gameOverEmitted.remove(gameId);
          }
          lastTurnBroadcast.remove(room.roomCode);
          roomManager.deleteRoom(room.roomCode);
        }
      } catch (RuntimeException ignored) {
        // Cleanup must never crash the server.
      }
    }, Constants.ROOM_SWEEP_INTERVAL_MS, Constants.ROOM_SWEEP_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }

  // ---------- rooms ----------

  public void createRoom(String socketId, Map<String, Object> session, String playerName) {
    if (roomManager.findRoomBySocketId(socketId) != null) {
      throw new IllegalArgumentException("Bạn đã ở trong phòng.");
    }
    Room room = roomManager.createRoom();
    String token = UUID.randomUUID().toString();
    String playerId = "player-" + UUID.randomUUID();
    RoomPlayer first = new RoomPlayer(playerId, playerName, token, socketId, null, false);
    room.addPlayer(first);
    roomManager.indexPlayer(room, first);
    session.put("sessionToken", token);
    room.touch();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("roomCode", room.roomCode);
    data.put("playerId", playerId);
    data.put("sessionToken", token);
    data.put("players", roomSnapshot(room));
    sender.toSession(socketId, "ROOM_CREATED", data);
  }

  public void joinRoom(String socketId, Map<String, Object> session, String roomCode, String playerName) {
    if (roomManager.findRoomBySocketId(socketId) != null) {
      throw new IllegalArgumentException("Bạn đã ở trong phòng.");
    }
    Room room = roomManager.getRoom(roomCode);
    String token = UUID.randomUUID().toString();
    String playerId = "player-" + UUID.randomUUID();
    RoomPlayer second = new RoomPlayer(playerId, playerName, token, socketId, null, false);
    room.addPlayer(second);
    roomManager.indexPlayer(room, second);
    session.put("sessionToken", token);
    room.touch();
    List<Map<String, Object>> players = roomSnapshot(room);
    Map<String, Object> mine = new LinkedHashMap<>();
    mine.put("roomCode", room.roomCode);
    mine.put("playerId", playerId);
    mine.put("sessionToken", token);
    mine.put("players", players);
    sender.toSession(socketId, "PLAYER_JOINED", mine);
    Map<String, Object> others = new LinkedHashMap<>();
    others.put("roomCode", room.roomCode);
    others.put("players", players);
    broadcastExcept(room, socketId, "PLAYER_JOINED", others);
    if (room.isFull()) {
      sender.toRoom(room.roomCode, "ROOM_READY", Map.of("roomCode", room.roomCode));
    }
  }

  public void selectDeck(String socketId, Map<String, Object> session, String heroId, String roomCode) {
    Room room = roomManager.getRoom(roomCode);
    if (room.isStarted() || room.isStarting()) return; // double-start guard
    RoomPlayer player = room.getPlayerBySession(tokenOf(session));
    if (player == null || !socketId.equals(player.socketId)) {
      throw new IllegalArgumentException("Player không tồn tại trong room.");
    }
    if (!List.of("MAGE", "HUNTER", "PALADIN", "PRIEST", "WARLOCK").contains(heroId)) {
      throw new IllegalArgumentException("Hero không hợp lệ.");
    }
    room.selectHero(player.playerId, heroId);
    room.touch();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("playerId", player.playerId);
    data.put("heroId", heroId);
    sender.toRoom(room.roomCode, "DECK_SELECTED", data);

    if (room.isFull() && room.getPlayers().stream().allMatch(p -> p.ready) && room.tryStart()) {
      CompletableFuture.runAsync(() -> startGame(room)).whenComplete((v, error) -> {
        if (error != null) {
          room.cancelStart();
          for (RoomPlayer p : room.getPlayers()) {
            RoomPlayer live = roomPlayer(room, p.playerId);
            if (live != null) live.ready = false;
          }
          sender.toRoom(room.roomCode, "ACTION_REJECTED", Map.of(
              "code", "GAME_START_FAILED",
              "message", errorMessage(error)));
        }
      });
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
    Game game = engine.createGame(gameId, room.roomCode,
        buildPlayerInit(players.get(0), heroes, cardCatalog),
        buildPlayerInit(players.get(1), heroes, cardCatalog));
    game.start();
    room.markStarted();
    gameIndex.put(room.roomCode, gameId);
    sender.toRoom(room.roomCode, "GAME_STARTED", Map.of("gameId", gameId));
    broadcastState(room, game);
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
    Game game = engine.getGame(gameId);
    Room room = roomManager.getRoom(game.roomCode);
    room.touch();
    try {
      engine.playCard(gameId, playerIdFromGame(game, socketId, session), cardInstanceId, targetId);
    } catch (RuntimeException error) {
      respondWithError(socketId, error);
      return;
    }
    broadcastState(room, game);
  }

  public void attack(String socketId, Map<String, Object> session,
      String gameId, String attackerId, String targetId) {
    Game game = engine.getGame(gameId);
    Room room = roomManager.getRoom(game.roomCode);
    room.touch();
    try {
      engine.attack(gameId, playerIdFromGame(game, socketId, session), attackerId, targetId);
    } catch (RuntimeException error) {
      respondWithError(socketId, error);
      return;
    }
    broadcastState(room, game);
  }

  public void endTurn(String socketId, Map<String, Object> session, String gameId) {
    Game game = engine.getGame(gameId);
    Room room = roomManager.getRoom(game.roomCode);
    room.touch();
    try {
      engine.endTurn(gameId, playerIdFromGame(game, socketId, session));
    } catch (RuntimeException error) {
      respondWithError(socketId, error);
      return;
    }
    broadcastState(room, game);
  }

  public void useHeroPower(String socketId, Map<String, Object> session, String gameId) {
    Game game = engine.getGame(gameId);
    Room room = roomManager.getRoom(game.roomCode);
    room.touch();
    try {
      engine.useHeroPower(gameId, playerIdFromGame(game, socketId, session));
    } catch (RuntimeException error) {
      respondWithError(socketId, error);
      return;
    }
    broadcastState(room, game);
  }

  public void drawCard(String socketId, Map<String, Object> session, String gameId) {
    Game game = engine.getGame(gameId);
    Room room = roomManager.getRoom(game.roomCode);
    room.touch();
    try {
      engine.drawCard(gameId, playerIdFromGame(game, socketId, session));
    } catch (RuntimeException error) {
      respondWithError(socketId, error);
      return;
    }
    broadcastState(room, game);
  }

  public void concede(String socketId, Map<String, Object> session, String gameId) {
    Game game = engine.getGame(gameId);
    Room room = roomManager.getRoom(game.roomCode);
    room.touch();
    try {
      engine.concede(gameId, playerIdFromGame(game, socketId, session));
    } catch (RuntimeException error) {
      respondWithError(socketId, error);
      return;
    }
    broadcastState(room, game);
  }

  public void handleDisconnect(String socketId) {
    Room room = roomManager.findRoomBySocketId(socketId);
    roomManager.unindexSocket(socketId);
    if (room == null) return;
    RoomPlayer leftPlayer = room.removeSocket(socketId);
    if (leftPlayer == null) return;
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("playerId", leftPlayer.playerId);
    sender.toRoom(room.roomCode, "PLAYER_DISCONNECTED", data);
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
    roomManager.indexPlayer(room, roomPlayer(room, player.playerId));
    room.touch();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("roomCode", room.roomCode);
    data.put("players", roomSnapshot(room));
    sender.toRoom(room.roomCode, "PLAYER_JOINED", data);

    String gameId = gameIndex.get(room.roomCode);
    if (gameId != null) {
      Game game = engine.getGame(gameId);
      sender.toSession(socketId, "GAME_STATE_UPDATED",
          Map.of("gameState", toStateFor(game, player.playerId)));
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
    Room room = roomManager.getRoom(game.roomCode);
    if (room.isStarting()) {
      sender.toSession(socketId, "ACTION_REJECTED", Map.of(
          "code", "REMATCH_IN_PROGRESS",
          "message", "Đang tạo trận tái đấu, vui lòng đợi."));
      return;
    }
    String playerId = playerIdFromGame(game, socketId, session);
    int votes = room.voteRematch(playerId);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("playerId", playerId);
    data.put("votes", votes);
    data.put("required", 2);
    sender.toRoom(room.roomCode, "REMATCH_REQUESTED", data);
    if (votes < 2) return;
    if (!room.tryStartRematch()) {
      sender.toSession(socketId, "ACTION_REJECTED", Map.of(
          "code", "REMATCH_IN_PROGRESS",
          "message", "Đang tạo trận tái đấu, vui lòng đợi."));
      return;
    }
    String oldGameId = game.gameId;
    CompletableFuture.runAsync(() -> startGame(room)).whenComplete((v, error) -> {
      if (error == null) {
        engine.removeGame(oldGameId);
        savedGames.remove(oldGameId);
        gameOverEmitted.remove(oldGameId);
      } else {
        room.cancelStart();
        sender.toRoom(room.roomCode, "ACTION_REJECTED", Map.of(
            "code", "REMATCH_FAILED",
            "message", errorMessage(error)));
      }
    });
  }

  // ---------- state ----------

  private List<Map<String, Object>> roomSnapshot(Room room) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (RoomPlayer p : room.getPlayers()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("playerId", p.playerId);
      m.put("name", p.name);
      m.put("heroClass", p.heroClass);
      m.put("ready", p.ready);
      m.put("connected", p.socketId != null);
      out.add(m);
    }
    return out;
  }

  Map<String, Object> toStateFor(Game game, String viewerPlayerId) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("gameId", game.gameId);
    m.put("roomCode", game.roomCode);
    m.put("status", game.getStatus());
    m.put("turn", game.getCurrentTurn());
    m.put("activePlayerId", game.getActivePlayerId());
    List<Map<String, Object>> players = new ArrayList<>();
    for (Player p : game.getPlayers()) players.add(playerToState(p, p.id().equals(viewerPlayerId)));
    m.put("players", players);
    m.put("winnerId", game.getWinnerId());
    m.put("statusMessage", game.getStatusMessage());
    m.put("manualDrawUsed", game.hasManualDrawnThisTurn());
    return m;
  }

  private Map<String, Object> playerToState(Player player, boolean isOwner) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("playerId", player.id());
    List<Map<String, Object>> hand = new ArrayList<>();
    if (isOwner) {
      for (CardTypes.CardDefinition c : player.handCards()) hand.add(CardTypes.cardToState(c));
    }
    m.put("hand", hand);
    m.put("handCount", player.handCount());
    List<Map<String, Object>> board = new ArrayList<>();
    for (vn.coincard.server.game.Minion minion : player.getBoard()) {
      board.add(EffectResolver.minionToState(minion));
    }
    m.put("board", board);
    m.put("deckCount", player.deckSize());
    m.put("mana", player.currentMana());
    m.put("maxMana", player.currentMaxMana());
    m.put("hero", EffectResolver.heroToState(player.heroState()));
    m.put("damageDealt", player.damageDealt());
    m.put("cardsPlayed", player.cardsPlayed());
    m.put("minionsSummoned", player.minionsSummoned());
    return m;
  }

  private void broadcastState(Room room, Game game) {
    for (RoomPlayer rp : room.getPlayers()) {
      if (rp.socketId == null) continue;
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("gameState", toStateFor(game, rp.playerId));
      sender.toSession(rp.socketId, "GAME_STATE_UPDATED", data);
    }
    Integer last = lastTurnBroadcast.get(room.roomCode);
    if (last == null || last != game.getCurrentTurn()) {
      lastTurnBroadcast.put(room.roomCode, game.getCurrentTurn());
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("activePlayerId", game.getActivePlayerId());
      data.put("turn", game.getCurrentTurn());
      sender.toRoom(room.roomCode, "TURN_CHANGED", data);
    }
    if (game.isFinished()) {
      if (!savedGames.contains(game.gameId) && !pendingSaves.contains(game.gameId)) {
        pendingSaves.add(game.gameId);
        saveGame(game, room, 0);
      }
      if (!gameOverEmitted.contains(game.gameId)) {
        gameOverEmitted.add(game.gameId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("winnerId", game.getWinnerId());
        sender.toRoom(room.roomCode, "GAME_OVER", data);
      }
    }
  }

  private void saveGame(Game game, Room room, int attempt) {
    CompletableFuture.runAsync(() -> {
      try {
        List<GamePlayerInput> players = new ArrayList<>();
        for (Player p : game.getPlayers()) {
          players.add(new GamePlayerInput(p.id(), p.name, game.getWinnerId() != null
              && game.getWinnerId().equals(p.id())));
        }
        gameRepository.recordFinishedGame(game.gameId, room.roomCode, players, game.getWinnerId());
        if (gameIdForRoom(room.roomCode) != null
            && gameIdForRoom(room.roomCode).equals(game.gameId)) {
          savedGames.add(game.gameId);
        }
        pendingSaves.remove(game.gameId);
      } catch (RuntimeException error) {
        System.err.println("Không lưu được kết quả " + game.gameId + ": " + error.getMessage());
        if (attempt < 2) {
          try {
            Thread.sleep(1000L * (attempt + 1));
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
          saveGame(game, room, attempt + 1);
        } else {
          pendingSaves.remove(game.gameId);
        }
      }
    });
  }

  private String gameIdForRoom(String roomCode) {
    return gameIndex.get(roomCode);
  }

  private String tokenOf(Map<String, Object> session) {
    Object token = session.get("sessionToken");
    return token instanceof String s ? s : "";
  }

  private String playerIdFromGame(Game game, String socketId, Map<String, Object> session) {
    Room room = roomManager.getRoom(game.roomCode);
    String current = gameIndex.get(room.roomCode);
    if (current == null || !current.equals(game.gameId)) {
      throw new IllegalArgumentException("Trận này không còn hoạt động.");
    }
    RoomPlayer roomPlayer = room.getPlayerBySession(tokenOf(session));
    if (roomPlayer == null || !socketId.equals(roomPlayer.socketId)) {
      throw new IllegalArgumentException("Player không tồn tại trong room.");
    }
    return roomPlayer.playerId;
  }

  private RoomPlayer roomPlayer(Room room, String playerId) {
    return room.getPlayers().stream().filter(p -> p.playerId.equals(playerId)).findFirst().orElse(null);
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
