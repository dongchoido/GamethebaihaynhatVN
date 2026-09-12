package vn.coincard.server.game;

import java.util.List;

/** Mirror of server/src/game/Game.ts */
public class Game {
  private String status = "WAITING";
  private int turn;
  private String activePlayerId = "";
  private String winnerId;
  private String statusMessage = "";
  private int manualDrawTurn = -1;

  public Game(String gameId, String roomCode, Player p1, Player p2) {
    this.gameId = gameId;
    this.roomCode = roomCode;
    this.players = List.of(p1, p2);
  }

  public final String gameId;
  public final String roomCode;
  private final List<Player> players;

  public void start() {
    if (!"WAITING".equals(status)) throw new IllegalStateException("Trận đã bắt đầu.");
    Player first = players.get(0);
    Player second = players.get(1);
    first.drawAndAddToHand(Constants.FIRST_PLAYER_HAND_SIZE);
    second.drawAndAddToHand(Constants.SECOND_PLAYER_HAND_SIZE);
    first.guaranteeCheapOpener(2);
    second.guaranteeCheapOpener(2);
    activePlayerId = first.id();
    turn = 1;
    status = "PLAYING";
    setStatusMessage("HERO 1 STARTS");
    beginTurnForPlayer(0, true);
  }

  public void switchTurn() {
    activePlayerId = getOpponent().id();
    turn++;
    setStatusMessage("");
    beginTurn();
  }

  public boolean isFinished() { return "FINISHED".equals(status); }
  public String getStatus() { return status; }
  public int getCurrentTurn() { return turn; }
  public String getActivePlayerId() { return activePlayerId; }
  public String getWinnerId() { return winnerId; }
  public boolean hasManualDrawnThisTurn() { return manualDrawTurn == turn; }
  public void markManualDraw() { manualDrawTurn = turn; }
  public String getStatusMessage() { return statusMessage; }
  public void setStatusMessage(String message) { statusMessage = message; }
  public List<Player> getPlayers() { return List.copyOf(players); }

  public Player getOpponent() {
    return players.stream().filter(p -> !p.id().equals(activePlayerId)).findFirst()
        .orElseThrow(() -> new IllegalStateException("Player index không hợp lệ."));
  }

  public Player getPlayerById(String playerId) {
    return players.stream().filter(p -> p.id().equals(playerId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Player id không tồn tại."));
  }

  public void resign(String playerId) {
    getPlayerById(playerId);
    if (isFinished()) return;
    String winner = players.stream().filter(p -> !p.id().equals(playerId)).findFirst()
        .map(Player::id).orElse(null);
    finish(winner, "CONCEDE");
  }

  public void finish(String winnerId, String reason) {
    if (isFinished()) return;
    status = "FINISHED";
    this.winnerId = winnerId;
    if (reason != null && !reason.isEmpty()) setStatusMessage(reason);
  }

  private void beginTurn() {
    int index = 0;
    for (int i = 0; i < players.size(); i++) {
      if (players.get(i).id().equals(activePlayerId)) index = i;
    }
    beginTurnForPlayer(index, false);
  }

  private void beginTurnForPlayer(int index, boolean skipDraw) {
    Player player = players.get(index);
    player.increaseMaxMana();
    player.refillMana();
    player.getBoard().forEach(Minion::startTurn);
    if (skipDraw) return;
    if (player.deckSize() > 0) player.addToHand(player.drawCard());
  }
}
