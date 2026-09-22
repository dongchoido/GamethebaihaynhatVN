package vn.coincard.server.application;

import java.util.List;
import org.springframework.stereotype.Service;
import vn.coincard.server.db.Repositories.GamePlayerInput;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.GameCommand;
import vn.coincard.server.game.GameCommandHandler;
import vn.coincard.server.mapper.GameStateDto;
import vn.coincard.server.mapper.GameStateMapper;
import vn.coincard.server.room.Room;
import vn.coincard.server.service.GamePersistenceService;

/** Executes a domain command and captures all viewer state while the game lock is held. */
@Service
public final class CommandExecutionService {
  private final GameCommandHandler commands;

  public CommandExecutionService(GameCommandHandler commands) {
    this.commands = commands;
  }

  public CommandResult execute(GameCommand command, Room room) {
    return commands.handle(command, game -> capture(room, game));
  }

  public GameStateDto.GameState stateFor(String gameId, String viewerPlayerId) {
    return commands.read(gameId, game -> GameStateMapper.toGameStateFor(game, viewerPlayerId));
  }

  public boolean isFinished(String gameId) {
    return commands.read(gameId, Game::isFinished);
  }

  private CommandResult capture(Room room, Game game) {
    List<MatchService.ViewerState> viewers = room.getPlayers().stream()
        .filter(player -> player.socketId() != null)
        .map(player -> new MatchService.ViewerState(player.socketId(),
            GameStateMapper.toGameStateFor(game, player.playerId())))
        .toList();
    GamePersistenceService.FinishedGame finishedGame = null;
    if (game.isFinished()) {
      List<GamePlayerInput> players = game.getPlayers().stream()
          .map(player -> new GamePlayerInput(player.id(), player.name(),
              game.getWinnerId() != null && game.getWinnerId().equals(player.id())))
          .toList();
      finishedGame = new GamePersistenceService.FinishedGame(game.getGameId(), game.getRoomCode(),
          players, game.getWinnerId());
    }
    return new CommandResult(viewers, finishedGame, game.getWinnerId());
  }

  public record CommandResult(List<MatchService.ViewerState> viewers,
      GamePersistenceService.FinishedGame finishedGame, String winnerId) {
    public CommandResult {
      viewers = List.copyOf(viewers);
    }
  }
}
