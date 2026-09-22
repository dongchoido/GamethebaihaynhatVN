package vn.coincard.server.game;

/** Closed set of commands accepted by the authoritative game application layer. */
public sealed interface GameCommand
    permits GameCommand.PlayCard, GameCommand.Attack, GameCommand.UseHeroPower,
        GameCommand.EndTurn, GameCommand.Concede {
  String gameId();
  String playerId();

  record PlayCard(String gameId, String playerId, String cardInstanceId, String targetId)
      implements GameCommand {
    public PlayCard {
      gameId = required(gameId, "gameId");
      playerId = required(playerId, "playerId");
      cardInstanceId = required(cardInstanceId, "cardInstanceId");
      targetId = optional(targetId);
    }
  }

  record Attack(String gameId, String playerId, String attackerId, String targetId)
      implements GameCommand {
    public Attack {
      gameId = required(gameId, "gameId");
      playerId = required(playerId, "playerId");
      attackerId = required(attackerId, "attackerId");
      targetId = required(targetId, "targetId");
    }
  }

  record EndTurn(String gameId, String playerId) implements GameCommand {
    public EndTurn {
      gameId = required(gameId, "gameId");
      playerId = required(playerId, "playerId");
    }
  }

  record UseHeroPower(String gameId, String playerId) implements GameCommand {
    public UseHeroPower {
      gameId = required(gameId, "gameId");
      playerId = required(playerId, "playerId");
    }
  }

  record Concede(String gameId, String playerId) implements GameCommand {
    public Concede {
      gameId = required(gameId, "gameId");
      playerId = required(playerId, "playerId");
    }
  }

  private static String required(String value, String field) {
    String normalized = optional(value);
    if (normalized == null) throw new GameException.InvalidCommand(field + " là bắt buộc.");
    return normalized;
  }

  private static String optional(String value) {
    if (value == null) return null;
    String normalized = value.trim();
    if (normalized.isEmpty()) throw new GameException.InvalidCommand("Id không hợp lệ.");
    return normalized;
  }
}
