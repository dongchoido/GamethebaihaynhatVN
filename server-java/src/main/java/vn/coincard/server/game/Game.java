package vn.coincard.server.game;

import java.util.List;
import java.util.Objects;

/** Mutable state for one authoritative match. */
public class Game {
  private GameStatus status = GameStatus.WAITING;
  private int turn;
  private String activePlayerId = "";
  private String winnerId;
  private String statusMessage = "";

  Game(String gameId, String roomCode, Player p1, Player p2) {
    this.gameId = required(gameId, "gameId");
    this.roomCode = required(roomCode, "roomCode");
    Player first = Objects.requireNonNull(p1, "p1");
    Player second = Objects.requireNonNull(p2, "p2");
    if (first.id().equals(second.id())) {
      throw new IllegalArgumentException("Hai player phải có id khác nhau.");
    }
    this.players = List.of(first, second);
  }

  private final String gameId;
  private final String roomCode;
  private final List<Player> players;

  public String getGameId() { return gameId; }
  public String getRoomCode() { return roomCode; }
  /** Starts a match with the official opening hand, Coin, and first-turn draw. */
  void start(String firstPlayerId) {
    if (status != GameStatus.WAITING) throw new IllegalStateException("Trận đã bắt đầu.");
    Player first = getPlayerById(firstPlayerId);
    Player second = players.stream().filter(p -> !p.id().equals(firstPlayerId)).findFirst()
        .orElseThrow(() -> new IllegalStateException("Cần đủ hai người chơi."));
    first.drawAndAddToHand(Constants.FIRST_PLAYER_HAND_SIZE);
    second.drawAndAddToHand(Constants.SECOND_PLAYER_HAND_SIZE);
    second.addToHand(TokenCards.coinCard());
    activePlayerId = first.id();
    turn = 1;
    status = GameStatus.PLAYING;
    beginTurnForPlayer(players.indexOf(first));
  }

  public void switchTurn() {
    activePlayerId = getOpponent().id();
    turn++;
    setStatusMessage("");
    beginTurn();
  }

  public boolean isFinished() { return status == GameStatus.FINISHED; }
  public GameStatus status() { return status; }
  public int getCurrentTurn() { return turn; }
  public String getActivePlayerId() { return activePlayerId; }
  public String getWinnerId() { return winnerId; }
  public String getStatusMessage() { return statusMessage; }
  public List<Player> getPlayers() { return List.copyOf(players); }

  public Player getOpponent() {
    return players.stream().filter(p -> !p.id().equals(activePlayerId)).findFirst()
        .orElseThrow(() -> new IllegalStateException("Player index không hợp lệ."));
  }

  public Player getPlayerById(String playerId) {
    return players.stream().filter(p -> p.id().equals(playerId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Player id không tồn tại."));
  }

  void resign(String playerId) {
    getPlayerById(playerId);
    if (isFinished()) return;
    String winner = players.stream().filter(p -> !p.id().equals(playerId)).findFirst()
        .map(Player::id).orElse(null);
    finish(winner, "CONCEDE");
  }

  void finish(String winnerId, String reason) {
    if (isFinished()) return;
    status = GameStatus.FINISHED;
    this.winnerId = winnerId;
    if (reason != null && !reason.isEmpty()) setStatusMessage(reason);
  }

  record State(GameStatus status, int turn, String activePlayerId, String winnerId,
      String statusMessage, List<Player.State> players) {
    State {
      players = List.copyOf(players);
    }
  }

  State snapshotState() {
    return new State(status, turn, activePlayerId, winnerId, statusMessage,
        players.stream().map(Player::snapshotState).toList());
  }

  void restoreState(State state) {
    if (state.players().size() != players.size()) {
      throw new IllegalArgumentException("Sai số lượng player snapshot.");
    }
    for (int index = 0; index < players.size(); index++) {
      players.get(index).restoreState(state.players().get(index));
    }
    status = state.status();
    turn = state.turn();
    activePlayerId = state.activePlayerId();
    winnerId = state.winnerId();
    statusMessage = state.statusMessage();
  }

  private void beginTurn() {
    int index = 0;
    for (int i = 0; i < players.size(); i++) {
      if (players.get(i).id().equals(activePlayerId)) index = i;
    }
    beginTurnForPlayer(index);
  }

  private void beginTurnForPlayer(int index) {
    Player player = players.get(index);
    player.increaseMaxMana();
    player.refillMana();
    player.getBoard().forEach(Minion::startTurn);
    player.drawForTurn();
  }

  private void setStatusMessage(String message) { statusMessage = message; }

  private static String required(String value, String field) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " là bắt buộc.");
    return value;
  }
}
