package hearthstone.engine;

import hearthstone.model.card.*;
import hearthstone.model.game.*;

import java.util.ArrayList;
import java.util.List;

public class ExhaustionSmokeTest {
    private static final GameEngine ENGINE = new GameEngine();
    private static int passed;

    public static void main(String[] args) {
        check("empty deck with cards in hand", ExhaustionSmokeTest::emptyDeckWithHand);
        check("empty hand with cards in deck", ExhaustionSmokeTest::emptyHandWithDeck);
        check("player 1 uses last card", () -> lastCardLoses(false));
        check("player 2 uses last card", () -> lastCardLoses(true));
        check("last minion still causes loss", ExhaustionSmokeTest::lastMinionLoses);
        check("last damage spell kills hero", ExhaustionSmokeTest::lastSpellWins);
        check("last nonlethal damage spell", ExhaustionSmokeTest::lastNonlethalSpellLoses);
        check("hand limited to seven cards", ExhaustionSmokeTest::handLimit);
        check("last draw discarded with full hand", ExhaustionSmokeTest::lastDrawWithFullHand);
        check("drawing from empty deck causes no damage", ExhaustionSmokeTest::emptyDraw);
        check("unaffordable last card is retained", ExhaustionSmokeTest::unaffordableCard);
        check("full board retains last minion", ExhaustionSmokeTest::fullBoard);
        check("finished match blocks all actions", ExhaustionSmokeTest::finishedMatchIsStable);
        check("empty starting decks rejected", ExhaustionSmokeTest::invalidInitialDecks);
        check("one-card starting decks supported", ExhaustionSmokeTest::smallInitialDecks);
        check("ready attacker cannot bypass exhaustion", ExhaustionSmokeTest::exhaustedBeforeAttack);
        check("wrong player cannot spend last card", ExhaustionSmokeTest::wrongPlayer);
        check("bounded matches reach exhaustion", ExhaustionSmokeTest::simulatedMatches);
        System.out.println("EXHAUSTION_SMOKE_TEST_OK cases=" + passed);
    }

    private static void emptyDeckWithHand() {
        GameState game = game(4, 4);
        nextTurn(game);
        require(game.getCurrentPlayer().getDeckSize() == 0, "Last deck card should be drawn");
        require(game.getCurrentPlayer().getHand().size() == 4, "Drawn card should stay in hand");
        nextTurn(game);
        nextTurn(game);
        require(game.getStatus() == GameStatus.IN_PROGRESS, "A nonempty hand protects from exhaustion");
        require(game.getPlayer1().getHero().getHealth() == 30
                && game.getPlayer2().getHero().getHealth() == 30, "No fatigue damage");
    }

    private static void emptyHandWithDeck() {
        GameState game = game(8, 8);
        Player owner = game.getCurrentPlayer();
        while (!owner.getHand().isEmpty()) playFirst(game);
        require(owner.getDeckSize() == 5, "Playing cards must not consume deck");
        require(game.getStatus() == GameStatus.IN_PROGRESS, "Empty hand alone is not a loss");
        nextTurn(game);
        nextTurn(game);
        require(owner.getHand().size() == 1, "Player must be able to draw again");
    }

    private static void lastCardLoses(boolean secondPlayer) {
        GameState game = secondPlayer ? game(8, 1) : game(1, 8);
        if (secondPlayer) nextTurn(game);
        Player loser = game.getCurrentPlayer();
        String winner = game.getOpponent().getName();
        playFirst(game);
        require(game.getStatus() == GameStatus.FINISHED, "Playing final card must end match");
        require(winner.equals(game.getWinnerName()), "Wrong winner for exhaustion");
        require(loser.hasNoCards(), "Loss requires both hand and deck empty");
        require(game.getEndReason().contains("deck và trên tay"), "Explain exhaustion");
        require(loser.getHero().getHealth() == 30, "Exhaustion must not fake lethal damage");
    }

    private static void lastMinionLoses() {
        GameState game = game(1, 8);
        Player owner = game.getCurrentPlayer();
        owner.getHand().clear();
        owner.getHand().add(new MinionCard("M", "Minion", 0, 2, 3, "Test"));
        playFirst(game);
        require(owner.getBoard().size() == 1, "Last minion resolves before exhaustion");
        require(game.getStatus() == GameStatus.FINISHED, "Board minions cannot prevent exhaustion");
        require(game.getOpponent().getName().equals(game.getWinnerName()), "Owner should lose");
    }

    private static void lastSpellWins() {
        GameState game = game(1, 8);
        Player owner = game.getCurrentPlayer();
        owner.getHand().clear();
        owner.getHand().add(new SpellCard("D", "Damage", 0, CardType.DAMAGE_SPELL, 30, "Test"));
        playFirst(game);
        require(owner.hasNoCards(), "Final spell consumed");
        require(owner.getName().equals(game.getWinnerName()), "Lethal effect resolves before exhaustion");
        require(game.getEndReason().contains("hết máu"), "Wrong lethal reason");
    }

    private static void lastNonlethalSpellLoses() {
        GameState game = game(1, 8);
        Player owner = game.getCurrentPlayer();
        owner.getHand().clear();
        owner.getHand().add(new SpellCard("D", "Damage", 0, CardType.DAMAGE_SPELL, 4, "Test"));
        playFirst(game);
        require(game.getOpponent().getHero().getHealth() == 26, "Nonlethal effect should resolve");
        require(game.getOpponent().getName().equals(game.getWinnerName()), "Caster must then lose");
    }

    private static void handLimit() {
        GameState game = game(16, 16);
        for (int i = 0; i < 24; i++) {
            nextTurn(game);
            require(game.getPlayer1().getHand().size() <= 7, "Player 1 hand exceeded cap");
            require(game.getPlayer2().getHand().size() <= 7, "Player 2 hand exceeded cap");
        }
        require(game.getPlayer1().getHand().size() == 7, "Player 1 hand cap incorrect");
        require(game.getPlayer2().getHand().size() == 7, "Player 2 hand cap incorrect");
        require(game.getCurrentPlayer().getTotalMana() == 10, "Mana cap changed");
    }

    private static void lastDrawWithFullHand() {
        GameState game = game(8, 8);
        Player owner = game.getPlayer2();
        while (owner.getHand().size() < 7) owner.drawCard();
        Card original = owner.getHand().get(0);
        require(owner.getDeckSize() == 1, "Expected one remaining deck card");
        nextTurn(game);
        require(owner.getDeckSize() == 0 && owner.getHand().size() == 7, "Burn last draw with full hand");
        require(owner.getHand().contains(original), "Existing hand must not be replaced");
        require(game.getStatus() == GameStatus.IN_PROGRESS, "Full hand prevents exhaustion loss");
    }

    private static void emptyDraw() {
        Player owner = new Player("A", "T", "Test", cards(1));
        owner.drawCard();
        for (int i = 0; i < 20; i++) owner.drawCard();
        require(owner.getHand().size() == 1, "Empty draw changed hand");
        require(owner.getHero().getHealth() == 30, "Old fatigue rule must be removed");
    }

    private static void unaffordableCard() {
        GameState game = game(1, 8);
        Player owner = game.getCurrentPlayer();
        owner.getHand().clear();
        Card expensive = new SpellCard("E", "Expensive", 10, CardType.HEAL_SPELL, 1, "Test");
        owner.getHand().add(expensive);
        expectRule(() -> playFirst(game));
        require(game.getStatus() == GameStatus.IN_PROGRESS, "Invalid play cannot cause exhaustion");
        require(owner.getHand().contains(expensive) && owner.getCurrentMana() == 1,
                "Invalid play changed hand or mana");
    }

    private static void fullBoard() {
        GameState game = game(1, 8);
        Player owner = game.getCurrentPlayer();
        owner.getHand().clear();
        owner.getHand().add(new MinionCard("M", "Minion", 0, 1, 2, "Test"));
        for (int i = 0; i < Player.MAX_BOARD_SIZE; i++)
            owner.summon(new MinionCard("B", "Board", 0, 1, 2, "Test"));
        expectRule(() -> playFirst(game));
        require(game.getStatus() == GameStatus.IN_PROGRESS && owner.getHand().size() == 1,
                "Full-board rejection consumed final card");
    }

    private static void finishedMatchIsStable() {
        GameState game = game(1, 8);
        Player owner = game.getCurrentPlayer();
        Card last = owner.getHand().get(0);
        playFirst(game);
        String winner = game.getWinnerName();
        String reason = game.getEndReason();
        int turn = game.getTurnNumber();
        int deck = game.getOpponent().getDeckSize();
        expectRule(() -> ENGINE.endTurn(game, owner.getId()));
        expectRule(() -> ENGINE.playCard(game, owner.getId(), last.getInstanceId()));
        expectRule(() -> ENGINE.attack(game, owner.getId(), "missing", GameEngine.HERO_TARGET));
        try {
            game.switchTurn();
            throw new AssertionError("Finished game allowed turn switch");
        } catch (IllegalStateException expected) { }
        game.finish(owner.getName(), "overwrite");
        require(winner.equals(game.getWinnerName()) && reason.equals(game.getEndReason()),
                "Result was overwritten");
        require(game.getTurnNumber() == turn && game.getOpponent().getDeckSize() == deck,
                "Finished match changed turn or deck");
    }

    private static void invalidInitialDecks() {
        for (int[] sizes : new int[][]{{0, 8}, {8, 0}, {0, 0}}) {
            try {
                game(sizes[0], sizes[1]);
                throw new AssertionError("Empty starting deck accepted");
            } catch (IllegalArgumentException expected) { }
        }
    }

    private static void smallInitialDecks() {
        GameState game = game(1, 1);
        require(game.getStatus() == GameStatus.IN_PROGRESS, "One-card hand should allow a match");
        require(game.getPlayer1().getHand().size() == 1 && game.getPlayer2().getHand().size() == 1,
                "Small initial decks dealt incorrectly");
        nextTurn(game);
        require(game.getStatus() == GameStatus.IN_PROGRESS, "Empty deck alone cannot end match");
    }

    private static void exhaustedBeforeAttack() {
        GameState game = game(1, 8);
        Player owner = game.getCurrentPlayer();
        owner.getHand().clear();
        MinionCard attacker = new MinionCard("M", "Attacker", 0, 30, 2, "Test");
        attacker.readyForTurn();
        owner.summon(attacker);
        expectRule(() -> ENGINE.attack(game, owner.getId(), attacker.getInstanceId(), GameEngine.HERO_TARGET));
        require(game.getOpponent().getHero().getHealth() == 30, "Exhausted player attacked");
        require(game.getOpponent().getName().equals(game.getWinnerName()), "Exhausted player must lose");
    }

    private static void wrongPlayer() {
        GameState game = game(8, 1);
        Player opponent = game.getOpponent();
        Card card = opponent.getHand().get(0);
        expectRule(() -> ENGINE.playCard(game, opponent.getId(), card.getInstanceId()));
        require(opponent.getHand().size() == 1 && game.getStatus() == GameStatus.IN_PROGRESS,
                "Wrong-turn rejection consumed last card");
    }

    private static void simulatedMatches() {
        for (int left = 1; left <= 16; left++) {
            for (int right = 1; right <= 16; right++) {
                GameState game = game(left, right);
                int actions = 0;
                while (game.getStatus() == GameStatus.IN_PROGRESS && actions++ < 100) {
                    if (!game.getCurrentPlayer().getHand().isEmpty()) playFirst(game);
                    if (game.getStatus() == GameStatus.IN_PROGRESS) nextTurn(game);
                    require(game.getPlayer1().getHand().size() <= 7
                            && game.getPlayer2().getHand().size() <= 7, "Simulation exceeded hand cap");
                }
                require(game.getStatus() == GameStatus.FINISHED, "Simulation failed to terminate");
                Player loser = game.getWinnerName().equals(game.getPlayer1().getName())
                        ? game.getPlayer2() : game.getPlayer1();
                require(loser.hasNoCards(), "Simulation declared premature exhaustion");
            }
        }
    }

    private static GameState game(int left, int right) {
        return new GameState(new Player("A", "T", "Test", cards(left)),
                new Player("B", "T", "Test", cards(right)));
    }

    private static List<Card> cards(int count) {
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < count; i++)
            result.add(new SpellCard("H", "Heal", 0, CardType.HEAL_SPELL, 1, "Test"));
        return result;
    }

    private static void playFirst(GameState game) {
        ENGINE.playCard(game, game.getCurrentPlayerId(), game.getCurrentPlayer().getHand().get(0).getInstanceId());
    }

    private static void nextTurn(GameState game) {
        ENGINE.endTurn(game, game.getCurrentPlayerId());
    }

    private static void expectRule(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected rule rejection");
        } catch (GameRuleException expected) { }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void check(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS " + name);
    }
}
