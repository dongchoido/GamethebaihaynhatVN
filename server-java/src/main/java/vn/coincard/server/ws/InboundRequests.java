package vn.coincard.server.ws;

import java.util.List;
import vn.coincard.server.game.HeroClass;

/** Typed request payloads parsed by the WebSocket adapter after JSON-tree validation. */
public final class InboundRequests {
  private InboundRequests() {}

  public record CreateRoom(String playerName) {
    public CreateRoom {
      playerName = required(playerName, "playerName", 24);
    }
  }

  public record JoinRoom(String roomCode, String playerName) {
    public JoinRoom {
      roomCode = required(roomCode, "roomCode", 6);
      playerName = required(playerName, "playerName", 24);
    }
  }

  public record SubmitLoadout(String roomCode, HeroClass heroClass, List<String> cardSlugs) {
    public SubmitLoadout {
      roomCode = required(roomCode, "roomCode", 6);
      if (heroClass == null) throw new IllegalArgumentException("heroClass is required.");
      cardSlugs = validatedList(cardSlugs, "cardSlugs", 30, 64);
    }
  }

  public record PlayCard(String gameId, String cardInstanceId, String targetId) {
    public PlayCard {
      gameId = required(gameId, "gameId", 64);
      cardInstanceId = required(cardInstanceId, "cardInstanceId", 128);
      targetId = optional(targetId, "targetId", 128);
    }
  }

  public record Attack(String gameId, String attackerId, String targetId) {
    public Attack {
      gameId = required(gameId, "gameId", 64);
      attackerId = required(attackerId, "attackerId", 128);
      targetId = required(targetId, "targetId", 128);
    }
  }

  public record GameAction(String gameId) {
    public GameAction {
      gameId = required(gameId, "gameId", 64);
    }
  }

  public record ReconnectGame(String sessionToken) {
    public ReconnectGame {
      sessionToken = required(sessionToken, "sessionToken", 128);
    }
  }

  private static String required(String value, String field, int maxLength) {
    String normalized = optional(value, field, maxLength);
    if (normalized == null) throw new IllegalArgumentException(field + " is required.");
    return normalized;
  }

  private static String optional(String value, String field, int maxLength) {
    if (value == null) return null;
    String normalized = value.trim();
    if (normalized.isEmpty() || normalized.length() > maxLength) {
      throw new IllegalArgumentException(field + " is invalid.");
    }
    return normalized;
  }

  private static List<String> validatedList(List<String> values, String field, int maxItems,
      int maxItemLength) {
    if (values == null || values.isEmpty() || values.size() > maxItems) {
      throw new IllegalArgumentException(field + " is invalid.");
    }
    return values.stream().map(value -> required(value, field, maxItemLength)).toList();
  }
}
