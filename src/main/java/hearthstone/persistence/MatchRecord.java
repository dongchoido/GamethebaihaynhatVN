package hearthstone.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record MatchRecord(
        LocalDateTime finishedAt,
        String player1,
        String player2,
        String winner,
        int turns
) {
    public String getFormattedTime() {
        return finishedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
