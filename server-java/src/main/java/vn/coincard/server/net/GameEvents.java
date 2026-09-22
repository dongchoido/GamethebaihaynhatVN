package vn.coincard.server.net;

import java.util.List;
import vn.coincard.server.game.ErrorCode;
import vn.coincard.server.mapper.GameStateDto;

/** Immutable outbound protocol payloads. Event names are defined by SocketEvent. */
public final class GameEvents {
  private GameEvents() {}

  public record RoomCreated(String roomCode, String playerId, String sessionToken,
      List<GameStateDto.RoomPlayerState> players) implements OutboundEvent {
    public RoomCreated {
      players = List.copyOf(players);
    }
  }

  public record PlayerJoined(String roomCode, String playerId, String sessionToken,
      List<GameStateDto.RoomPlayerState> players) implements OutboundEvent {
    public PlayerJoined {
      players = List.copyOf(players);
    }
  }

  public record RoomReady(String roomCode) implements OutboundEvent {}

  public record LoadoutAccepted(String roomCode, List<GameStateDto.RoomPlayerState> players)
      implements OutboundEvent {
    public LoadoutAccepted {
      players = List.copyOf(players);
    }
  }

  public record GameStarted(String gameId) implements OutboundEvent {}

  public record GameStateUpdated(GameStateDto.GameState gameState) implements OutboundEvent {}

  public record GameOver(String winnerId) implements OutboundEvent {}

  public record PlayerDisconnected(String playerId) implements OutboundEvent {}

  public record ActionRejected(ErrorCode code, String message) implements OutboundEvent {}
}
