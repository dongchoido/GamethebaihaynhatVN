package hearthstone.persistence;

import hearthstone.model.game.GameState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

public class FileGameRepository {

    private final Path dataDirectory;
    private final Path usersFile;
    private final Path decksFile;
    private final Path matchesFile;

    public FileGameRepository() {
        this(Path.of("data"));
    }

    public FileGameRepository(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.usersFile = dataDirectory.resolve("users.tsv");
        this.decksFile = dataDirectory.resolve("decks.tsv");
        this.matchesFile = dataDirectory.resolve("matches.tsv");
        initializeFiles();
    }

    public synchronized void ensureUser(String username) {
        String safeName = sanitize(username);
        Map<String, UserStats> users = readUsers();
        if (!users.containsKey(safeName)) {
            users.put(safeName, new UserStats(safeName, 0, 0));
            writeUsers(users.values());
        }
    }

    public synchronized void recordDeckSelection(String username,
                                                 String deckCode,
                                                 String deckName) {
        String line = sanitize(username) + "\t"
                + sanitize(deckCode) + "\t"
                + sanitize(deckName);
        try {
            List<String> existing = Files.readAllLines(decksFile, StandardCharsets.UTF_8);
            if (!existing.contains(line)) {
                appendLine(decksFile, line);
            }
        } catch (IOException exception) {
            throw storageError(exception);
        }
    }

    public synchronized void recordMatch(GameState game) {
        String player1Name = sanitize(game.getPlayer1().getName());
        String player2Name = sanitize(game.getPlayer2().getName());
        String winnerName = sanitize(game.getWinnerName());

        Map<String, UserStats> users = readUsers();
        users.putIfAbsent(player1Name, new UserStats(player1Name, 0, 0));
        users.putIfAbsent(player2Name, new UserStats(player2Name, 0, 0));

        UserStats winner = users.get(winnerName);
        UserStats loser = winnerName.equals(player1Name)
                ? users.get(player2Name)
                : users.get(player1Name);
        winner.addWin();
        loser.addLoss();
        writeUsers(users.values());

        String matchLine = LocalDateTime.now() + "\t"
                + player1Name + "\t"
                + player2Name + "\t"
                + winnerName + "\t"
                + game.getTurnNumber();
        appendLine(matchesFile, matchLine);
    }

    public synchronized List<MatchRecord> getMatches() {
        List<MatchRecord> matches = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(matchesFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                if (parts.length == 5) {
                    matches.add(new MatchRecord(
                            LocalDateTime.parse(parts[0]),
                            parts[1], parts[2], parts[3],
                            Integer.parseInt(parts[4])
                    ));
                }
            }
        } catch (IOException | RuntimeException exception) {
            throw storageError(exception);
        }
        Collections.reverse(matches);
        return matches;
    }

    public synchronized List<UserStats> getUsers() {
        return new ArrayList<>(readUsers().values());
    }

    private Map<String, UserStats> readUsers() {
        Map<String, UserStats> users = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(usersFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                if (parts.length == 3) {
                    users.put(parts[0], new UserStats(
                            parts[0],
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2])
                    ));
                }
            }
            return users;
        } catch (IOException | NumberFormatException exception) {
            throw storageError(exception);
        }
    }

    private void writeUsers(Collection<UserStats> users) {
        List<String> lines = users.stream()
                .map(user -> sanitize(user.getUsername()) + "\t"
                        + user.getWins() + "\t" + user.getLosses())
                .toList();
        try {
            Files.write(usersFile, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw storageError(exception);
        }
    }

    private void initializeFiles() {
        try {
            Files.createDirectories(dataDirectory);
            createIfMissing(usersFile);
            createIfMissing(decksFile);
            createIfMissing(matchesFile);
        } catch (IOException exception) {
            throw storageError(exception);
        }
    }

    private void createIfMissing(Path file) throws IOException {
        if (Files.notExists(file)) {
            Files.createFile(file);
        }
    }

    private void appendLine(Path file, String line) {
        try {
            Files.writeString(file, line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw storageError(exception);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim()
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private IllegalStateException storageError(Exception cause) {
        return new IllegalStateException("Không thể đọc hoặc ghi dữ liệu game", cause);
    }
}
