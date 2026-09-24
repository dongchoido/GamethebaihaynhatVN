package hearthstone.engine;

import hearthstone.model.card.Card;
import hearthstone.model.card.MinionCard;
import hearthstone.model.game.GameState;
import hearthstone.model.game.Player;

import java.util.ArrayList;
import java.util.List;

public class EngineSmokeTest {

    public static void main(String[] args) {
        Player player1 = new Player("A", "TEST", "Test Deck", createDeck());
        Player player2 = new Player("B", "TEST", "Test Deck", createDeck());
        GameState game = new GameState(player1, player2);
        GameEngine engine = new GameEngine();

        Card card = player1.getHand().get(0);
        engine.playCard(game, player1.getId(), card.getInstanceId());
        require(player1.getBoard().size() == 1, "Không triệu hồi được Minion");
        require(player1.getCurrentMana() == 0, "Không trừ đúng mana");

        engine.endTurn(game, player1.getId());
        engine.endTurn(game, player2.getId());
        MinionCard attacker = player1.getBoard().get(0);
        engine.attack(game, player1.getId(), attacker.getInstanceId(), GameEngine.HERO_TARGET);
        require(player2.getHero().getHealth() == 28, "Attack không gây đúng damage");

        System.out.println("ENGINE_SMOKE_TEST_OK");
    }

    private static List<Card> createDeck() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            cards.add(new MinionCard("SOLDIER", "Soldier", 1,
                    2, 2, "Basic minion"));
        }
        return cards;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
