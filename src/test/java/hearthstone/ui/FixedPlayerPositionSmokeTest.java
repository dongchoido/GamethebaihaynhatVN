package hearthstone.ui;

import hearthstone.model.card.*;
import hearthstone.model.game.*;
import hearthstone.persistence.FileGameRepository;
import javax.swing.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Exercise both fixed sides through real button actions, without showing a window. */
public class FixedPlayerPositionSmokeTest {
    public static void main(String[] args) throws Exception {
        var data = Files.createTempDirectory("fixed-player-ui-");
        SoundPlayer.setEnabled(false);
        SwingUtilities.invokeAndWait(() -> {
            GameFrame frame = null;
            try {
                Player first = new Player("Đức Anh", "EMBER", "Ember Legion", deck());
                Player second = new Player("Opponent", "FOREST", "Forest Guard", deck());
                GameState game = new GameState(first, second);
                MinionCard a = new MinionCard("A", "A", 0, 2, 3, "Test");
                MinionCard b = new MinionCard("B", "B", 0, 2, 5, "Test");
                first.summon(a);
                second.summon(b);
                a.readyForTurn();
                frame = new GameFrame(game, new FileGameRepository(data));
                JButton firstHero = (JButton) field(frame, "player1HeroButton");
                JButton secondHero = (JButton) field(frame, "player2HeroButton");
                JPanel firstBoard = (JPanel) field(frame, "player1Board");
                JPanel secondBoard = (JPanel) field(frame, "player2Board");
                JButton endTurn = (JButton) field(frame, "endTurnButton");
                require(secondHero.isEnabled(), "Opponent Hero unavailable on first turn");
                ((JButton) firstBoard.getComponent(0)).doClick();
                secondHero.doClick();
                require(second.getHero().getHealth() == 30, "Defense was bypassed");
                ((JButton) secondBoard.getComponent(0)).doClick();
                require(a.getCurrentHealth() == 1 && b.getCurrentHealth() == 3, "Player 1 attack routed incorrectly");
                endTurn.doClick();
                require(firstHero.getText().contains("Đức Anh") && secondHero.getText().contains("Opponent"), "Names swapped");
                require(firstHero.isEnabled(), "Opponent Hero unavailable on second turn");
                ((JButton) secondBoard.getComponent(0)).doClick();
                secondHero.doClick();
                require(first.getHero().getHealth() == 30, "Clicking own Hero attacked someone else");
                ((JButton) firstBoard.getComponent(0)).doClick();
                require(first.getBoard().isEmpty() && b.getCurrentHealth() == 1, "Player 2 attack routed incorrectly");
                endTurn.doClick();
                require(game.getCurrentPlayer() == first, "Turn did not switch back");
                endTurn.doClick();
                ((JButton) secondBoard.getComponent(0)).doClick();
                firstHero.doClick();
                require(first.getHero().getHealth() == 28 && second.getHero().getHealth() == 30, "Fixed lower Hero target failed");
                require(firstHero.getText().contains("Đức Anh") && firstHero.getText().contains("28/30"), "Wrong Hero information");
                require(secondHero.getText().contains("Opponent"), "Player 2 moved after attack");
                JPanel hand = (JPanel) field(frame, "handPanel");
                int secondHandSize = second.getHand().size();
                ((JButton) hand.getComponent(0)).doClick();
                require(second.getHand().size() == secondHandSize - 1, "Hand no longer follows active player");
                System.out.println("FIXED_PLAYER_POSITION_SMOKE_TEST_OK");
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            } finally {
                if (frame != null) {
                    try {
                        Timer timer = (Timer) field(frame, "messageTimer");
                        if (timer != null) timer.stop();
                    } catch (Exception ignored) { }
                    frame.dispose();
                }
            }
        });
    }

    private static Object field(GameFrame frame, String name) throws Exception {
        var field = GameFrame.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(frame);
    }

    private static List<Card> deck() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            cards.add(new SpellCard("H", "Heal", 0, CardType.HEAL_SPELL, 1, "Test"));
        }
        return cards;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
