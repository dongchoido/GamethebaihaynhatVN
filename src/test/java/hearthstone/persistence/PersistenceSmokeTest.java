package hearthstone.persistence;

import hearthstone.model.card.Card;
import hearthstone.model.card.MinionCard;
import hearthstone.model.game.GameState;
import hearthstone.model.game.Player;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PersistenceSmokeTest {

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Cần truyền thư mục test");
        }
        FileGameRepository repository = new FileGameRepository(Path.of(args[0]));
        repository.ensureUser("A");
        repository.ensureUser("B");
        repository.recordDeckSelection("A", "TEST", "Test Deck");

        GameState game = new GameState(
                new Player("A", "TEST", "Test Deck", deck()),
                new Player("B", "TEST", "Test Deck", deck())
        );
        game.finish("A");
        repository.recordMatch(game);

        if (repository.getUsers().size() != 2 || repository.getMatches().size() != 1) {
            throw new AssertionError("Persistence smoke test failed");
        }
        System.out.println("PERSISTENCE_SMOKE_TEST_OK");
    }

    private static List<Card> deck() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cards.add(new MinionCard("TEST", "Test", 1, 1, 1, "Test"));
        }
        return cards;
    }
}
