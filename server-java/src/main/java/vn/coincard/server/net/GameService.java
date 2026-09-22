package vn.coincard.server.net;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import vn.coincard.server.application.ActiveGameRegistry;
import vn.coincard.server.application.CommandExecutionService;
import vn.coincard.server.application.LoadoutService;
import vn.coincard.server.application.MatchService;
import vn.coincard.server.game.ErrorCode;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.GameCommand;
import vn.coincard.server.game.GameException;
import vn.coincard.server.game.GameSessionRegistry;
import vn.coincard.server.game.HeroClass;
import vn.coincard.server.mapper.GameStateMapper;
import vn.coincard.server.room.Room;
import vn.coincard.server.room.RoomManager;
import vn.coincard.server.room.RoomPlayer;
import vn.coincard.server.service.GamePersistenceService;
import vn.coincard.server.service.RoomService;
import vn.coincard.server.ws.SocketEvent;

/** WebSocket-facing facade; game rules and snapshots live in application/domain services. */
@Service
public final class GameService {
  private static final Logger log = LoggerFactory.getLogger(GameService.class);
  private final RoomManager rooms;
  private final RoomService roomService;
  private final LoadoutService loadouts;
  private final MatchService matches;
  private final CommandExecutionService commandExecution;
  private final ActiveGameRegistry activeGames;
  private final GamePersistenceService persistence;
  private final OutboundGameGateway outbound;
  private final GameSessionRegistry sessions;
  private final TaskExecutor taskExecutor;

  public GameService(RoomManager rooms, RoomService roomService, LoadoutService loadouts,
      MatchService matches, CommandExecutionService commandExecution,
      ActiveGameRegistry activeGames, GamePersistenceService persistence,
      OutboundGameGateway outbound, GameSessionRegistry sessions,
      @Qualifier("gameTaskExecutor") TaskExecutor taskExecutor) {
    this.rooms = rooms;
    this.roomService = roomService;
    this.loadouts = loadouts;
    this.matches = matches;
    this.commandExecution = commandExecution;
    this.activeGames = activeGames;
    this.persistence = persistence;
    this.outbound = outbound;
    this.sessions = sessions;
    this.taskExecutor = taskExecutor;
  }

  public void createRoom(String socketId, PlayerSession session, String playerName) {
    roomService.createRoom(socketId, session, playerName);
  }

  public void joinRoom(String socketId, PlayerSession session, String roomCode, String playerName) {
    roomService.joinRoom(socketId, session, roomCode, playerName);
  }

  public void submitLoadout(String socketId, PlayerSession session, HeroClass heroClass,
      String roomCode, List<String> cardSlugs) {
    LoadoutService.Outcome outcome = loadouts.submit(socketId, session, heroClass, roomCode, cardSlugs);
    if (!outcome.shouldStart()) return;
    CompletableFuture.runAsync(() -> startGame(outcome.room()), taskExecutor).whenComplete((ignored, error) -> {
      if (error == null) return;
      outcome.room().cancelStart();
      outcome.room().clearReady();
      log.error("Không thể khởi tạo trận", error);
      outbound.toRoom(outcome.room().getRoomCode(), SocketEvent.ACTION_REJECTED,
          new GameEvents.ActionRejected(ErrorCode.GAME_START_FAILED, startFailureMessage()));
    });
  }

  private void startGame(Room room) {
    MatchService.StartedMatch match = matches.start(room);
    outbound.toRoom(room.getRoomCode(), SocketEvent.GAME_STARTED, new GameEvents.GameStarted(match.gameId()));
    publishViewers(match.viewers());
  }

  public void playCard(String socketId, PlayerSession session, String gameId,
      String cardInstanceId, String targetId) {
    executeAction(socketId, session, gameId,
        playerId -> new GameCommand.PlayCard(gameId, playerId, cardInstanceId, targetId));
  }

  public void attack(String socketId, PlayerSession session, String gameId, String attackerId,
      String targetId) {
    executeAction(socketId, session, gameId,
        playerId -> new GameCommand.Attack(gameId, playerId, attackerId, targetId));
  }

  public void endTurn(String socketId, PlayerSession session, String gameId) {
    executeAction(socketId, session, gameId, playerId -> new GameCommand.EndTurn(gameId, playerId));
  }

  public void useHeroPower(String socketId, PlayerSession session, String gameId) {
    executeAction(socketId, session, gameId,
        playerId -> new GameCommand.UseHeroPower(gameId, playerId));
  }

  public void concede(String socketId, PlayerSession session, String gameId) {
    executeAction(socketId, session, gameId, playerId -> new GameCommand.Concede(gameId, playerId));
  }

  private void executeAction(String socketId, PlayerSession session, String gameId,
      CommandFactory commandFactory) {
    try {
      Room room = roomForGame(gameId);
      room.touch();
      String playerId = playerIdFromGame(gameId, room, socketId, session);
      CommandExecutionService.CommandResult result = commandExecution.execute(
          commandFactory.create(playerId), room);
      publishCommandResult(room, result);
    } catch (RuntimeException error) {
      respondWithError(socketId, error);
    }
  }

  public void handleDisconnect(String socketId) {
    Room room = rooms.findRoomBySocketId(socketId);
    rooms.unindexSocket(socketId);
    if (room == null) return;
    RoomPlayer leftPlayer = room.removeSocket(socketId);
    if (leftPlayer == null) return;
    outbound.toRoom(room.getRoomCode(), SocketEvent.PLAYER_DISCONNECTED,
        new GameEvents.PlayerDisconnected(leftPlayer.playerId()));
  }

  public void reconnect(String socketId, PlayerSession session, String sessionToken) {
    Room existing = rooms.findRoomBySocketId(socketId);
    if (existing != null) {
      boolean mine = existing.getPlayers().stream().anyMatch(player -> socketId.equals(player.socketId())
          && sessionToken.equals(player.sessionToken()));
      if (!mine) throw new GameException.ReconnectFailed();
    }
    Room room = rooms.findRoomBySessionToken(sessionToken);
    if (room == null) throw new GameException.ReconnectFailed();
    RoomPlayer player = room.getPlayerBySession(sessionToken);
    if (player == null) throw new GameException.ReconnectFailed();
    if (player.socketId() != null && !player.socketId().equals(socketId)) {
      rooms.unindexSocket(player.socketId());
      outbound.disconnectSession(player.socketId());
    }
    room.bindSocket(player.playerId(), socketId);
    session.bindSessionToken(sessionToken);
    rooms.indexPlayer(room, room.getPlayerBySession(sessionToken));
    room.touch();
    outbound.toRoom(room.getRoomCode(), SocketEvent.PLAYER_JOINED, new GameEvents.PlayerJoined(
        room.getRoomCode(), null, null, GameStateMapper.toRoomSnapshot(room)));

    String gameId = activeGames.gameIdForRoom(room.getRoomCode());
    if (gameId == null) return;
    outbound.toSession(socketId, SocketEvent.GAME_STATE_UPDATED,
        new GameEvents.GameStateUpdated(commandExecution.stateFor(gameId, player.playerId())));
  }

  public void rematch(String socketId, PlayerSession session, String gameId) {
    Room room = roomForGame(gameId);
    if (!commandExecution.isFinished(gameId)) {
      reject(socketId, ErrorCode.REMATCH_NOT_ALLOWED, "Trận chưa kết thúc, không thể tái đấu.");
      return;
    }
    if (room.isStarting()) {
      reject(socketId, ErrorCode.REMATCH_IN_PROGRESS, "Đang tạo trận tái đấu, vui lòng đợi.");
      return;
    }
    String playerId = playerIdFromGame(gameId, room, socketId, session);
    if (room.voteRematch(playerId) < 2) return;
    if (!room.tryStartRematch()) {
      reject(socketId, ErrorCode.REMATCH_IN_PROGRESS, "Đang tạo trận tái đấu, vui lòng đợi.");
      return;
    }
    String oldGameId = gameId;
    CompletableFuture.runAsync(() -> startGame(room), taskExecutor).whenComplete((ignored, error) -> {
      if (error == null) {
        sessions.remove(oldGameId);
        persistence.removeGame(oldGameId);
        return;
      }
      room.cancelStart();
      log.error("Không thể tạo trận tái đấu", error);
      outbound.toRoom(room.getRoomCode(), SocketEvent.ACTION_REJECTED,
          new GameEvents.ActionRejected(ErrorCode.REMATCH_FAILED, startFailureMessage()));
    });
  }

  private void publishViewers(List<MatchService.ViewerState> viewers) {
    for (MatchService.ViewerState viewer : viewers) {
      outbound.toSession(viewer.socketId(), SocketEvent.GAME_STATE_UPDATED,
          new GameEvents.GameStateUpdated(viewer.state()));
    }
  }

  private void publishCommandResult(Room room, CommandExecutionService.CommandResult result) {
    publishViewers(result.viewers());
    GamePersistenceService.FinishedGame finishedGame = result.finishedGame();
    if (finishedGame == null) return;
    persistence.saveGame(finishedGame, activeGames.gameIdForRoom(room.getRoomCode()), 0);
    if (persistence.shouldEmitGameOver(finishedGame.gameId())) {
      persistence.markGameOverEmitted(finishedGame.gameId());
      outbound.toRoom(room.getRoomCode(), SocketEvent.GAME_OVER,
          new GameEvents.GameOver(result.winnerId()));
    }
  }

  private Room roomForGame(String gameId) {
    String roomCode = sessions.withLockedGame(gameId, Game::getRoomCode);
    return rooms.getRoom(roomCode);
  }

  private String playerIdFromGame(String gameId, Room room, String socketId, PlayerSession session) {
    if (!activeGames.isCurrent(room.getRoomCode(), gameId)) {
      throw new GameException.GameNotRunning();
    }
    RoomPlayer roomPlayer = room.getPlayerBySession(session.sessionToken());
    if (roomPlayer == null || !socketId.equals(roomPlayer.socketId())) {
      throw new GameException.InvalidPayload("Player không tồn tại trong room.");
    }
    return roomPlayer.playerId();
  }

  private void respondWithError(String socketId, RuntimeException error) {
    if (error instanceof GameException gameError) {
      reject(socketId, gameError.errorCode(), gameError.getMessage());
      return;
    }
    log.warn("Command thất bại cho session {}", socketId, error);
    reject(socketId, ErrorCode.INTERNAL_ERROR, "Server không thể xử lý yêu cầu này.");
  }

  private void reject(String socketId, ErrorCode code, String message) {
    outbound.toSession(socketId, SocketEvent.ACTION_REJECTED,
        new GameEvents.ActionRejected(code, message));
  }

  private String startFailureMessage() {
    return "Không thể khởi tạo trận. Vui lòng thử lại.";
  }

  @FunctionalInterface
  private interface CommandFactory {
    GameCommand create(String playerId);
  }
}
