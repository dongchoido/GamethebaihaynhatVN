package hearthstone.ui;

import hearthstone.model.card.*;
import hearthstone.model.game.*;
import hearthstone.persistence.FileGameRepository;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Run with a desktop JDK; frames are constructed without showing a window. */
public class EndGameSmokeTest {
    public static void main(String[] args) throws Exception {
        Path data = Files.createTempDirectory("hearthstone-endgame-test-");
        SoundPlayer.setEnabled(false);
        SwingUtilities.invokeAndWait(() -> {
            GameFrame frame = null;
            try {
                FileGameRepository repository = new FileGameRepository(data);
                Player owner = new Player("A", "TEST", "Test", deck(1));
                Player opponent = new Player("B", "TEST", "Test", deck(8));
                GameState game = new GameState(owner, opponent);
                MinionCard minion = new MinionCard("M", "Minion", 0, 3, 3, "Test");
                minion.readyForTurn();
                owner.summon(minion);
                frame = new GameFrame(game, repository);
                // Suppress the modal dialog so this smoke test can run unattended.
                field("resultShown").setBoolean(frame, true);
                JPanel hand = (JPanel) field("handPanel").get(frame);
                ((JButton) hand.getComponent(0)).doClick();
                require(game.getStatus() == GameStatus.FINISHED, "Final card did not end game");
                require("B".equals(game.getWinnerName()), "Wrong exhaustion winner");
                JLabel message = (JLabel) field("messageLabel").get(frame);
                require(message.getText().contains("B"), "Winner banner missing");
                require(message.getToolTipText().contains("deck và trên tay"), "Reason missing");
                require(!((JButton) field("opponentHeroButton").get(frame)).isEnabled(),
                        "Hero still clickable after game ends");
                JPanel board = (JPanel) field("currentBoard").get(frame);
                JButton minionButton = (JButton) board.getComponent(0);
                require(!minionButton.isEnabled(), "Minion still clickable after game ends");
                require(!((JButton) field("endTurnButton").get(frame)).isVisible(), "Turn button still visible");
                require(((JButton) field("mainMenuButton").get(frame)).isVisible(), "Menu button missing");
                String result = message.getText();
                minionButton.doClick();
                Method select = GameFrame.class.getDeclaredMethod("selectAttacker", MinionCard.class);
                select.setAccessible(true);
                select.invoke(frame, minion);
                require(result.equals(message.getText()), "Selecting a minion replaced result banner");
                Method refresh = GameFrame.class.getDeclaredMethod("refreshGame");
                refresh.setAccessible(true);
                refresh.invoke(frame);
                refresh.invoke(frame);
                require(result.equals(message.getText()), "Refreshing replaced result banner");
                require(repository.getMatches().size() == 1, "Result recorded more than once");
                require(repository.getMatches().get(0).winner().equals("B"), "Saved winner incorrect");
                var winner = repository.getUsers().stream().filter(user -> user.getUsername().equals("B")).findFirst().orElseThrow();
                var loser = repository.getUsers().stream().filter(user -> user.getUsername().equals("A")).findFirst().orElseThrow();
                require(winner.getWins() == 1 && loser.getLosses() == 1, "Stats counted incorrectly");
                require(!SoundPlayer.isEnabled(), "Mute state lost");
                System.out.println("END_GAME_UI_SMOKE_TEST_OK");
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            } finally {
                if (frame != null) frame.dispose();
            }
        });
    }

    private static Field field(String name) throws Exception {
        Field field = GameFrame.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static List<Card> deck(int size) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < size; i++)
            cards.add(new SpellCard("H", "Heal", 0, CardType.HEAL_SPELL, 1, "Test"));
        return cards;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
