package hearthstone.persistence;

public class UserStats {

    private final String username;
    private int wins;
    private int losses;

    public UserStats(String username, int wins, int losses) {
        this.username = username;
        this.wins = wins;
        this.losses = losses;
    }

    public String getUsername() {
        return username;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public void addWin() {
        wins++;
    }

    public void addLoss() {
        losses++;
    }
}
