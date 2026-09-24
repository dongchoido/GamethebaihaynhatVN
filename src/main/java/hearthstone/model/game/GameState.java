package hearthstone.model.game;

import java.time.LocalDateTime;

public class GameState {

    private final Player player1;
    private final Player player2;
    private final LocalDateTime startedAt;
    private String currentPlayerId;
    private GameStatus status;
    private String winnerName;
    private int turnNumber;

    public GameState(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.startedAt = LocalDateTime.now();
        this.currentPlayerId = player1.getId();
        this.status = GameStatus.IN_PROGRESS;
        this.turnNumber = 1;

        player1.drawInitialHand(3);
        player2.drawInitialHand(3);
        player1.beginFirstTurn();
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public GameStatus getStatus() {
        return status;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public Player getCurrentPlayer() {
        return player1.getId().equals(currentPlayerId) ? player1 : player2;
    }

    public Player getOpponent() {
        return player1.getId().equals(currentPlayerId) ? player2 : player1;
    }

    public void switchTurn() {
        Player nextPlayer = getOpponent();
        currentPlayerId = nextPlayer.getId();
        turnNumber++;
        nextPlayer.beginTurn();
    }

    public void finish(String winnerName) {
        status = GameStatus.FINISHED;
        this.winnerName = winnerName;
    }
}
