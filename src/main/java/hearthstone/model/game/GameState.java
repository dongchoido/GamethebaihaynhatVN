package hearthstone.model.game;

import java.time.LocalDateTime;

public class GameState {

    public static final int INITIAL_HAND_SIZE = 3;

    private final Player player1;
    private final Player player2;
    private final LocalDateTime startedAt;
    private String currentPlayerId;
    private GameStatus status;
    private String winnerName;
    private String endReason = "";
    private int turnNumber;

    public GameState(Player player1, Player player2) {
        if (player1.getDeckSize() == 0 || player2.getDeckSize() == 0) {
            throw new IllegalArgumentException(
                    "Mỗi người chơi cần có ít nhất 1 lá bài để bắt đầu trận");
        }
        this.player1 = player1;
        this.player2 = player2;
        this.startedAt = LocalDateTime.now();
        this.currentPlayerId = player1.getId();
        this.status = GameStatus.IN_PROGRESS;
        this.turnNumber = 1;

        player1.drawInitialHand(INITIAL_HAND_SIZE);
        player2.drawInitialHand(INITIAL_HAND_SIZE);
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

    public String getEndReason() {
        return endReason;
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
        if (status == GameStatus.FINISHED) {
            throw new IllegalStateException("Trận đấu đã kết thúc");
        }
        Player nextPlayer = getOpponent();
        currentPlayerId = nextPlayer.getId();
        turnNumber++;
        nextPlayer.beginTurn();
    }

    public void finish(String winnerName) {
        finish(winnerName, "");
    }

    public void finish(String winnerName, String reason) {
        if (status == GameStatus.FINISHED) {
            return;
        }
        status = GameStatus.FINISHED;
        this.winnerName = winnerName;
        this.endReason = reason;
    }
}
