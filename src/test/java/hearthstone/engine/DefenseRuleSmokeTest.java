package hearthstone.engine;

import hearthstone.model.card.Card;
import hearthstone.model.card.CardType;
import hearthstone.model.card.MinionCard;
import hearthstone.model.card.SpellCard;
import hearthstone.model.game.GameState;
import hearthstone.model.game.Player;

import java.util.ArrayList;
import java.util.List;

public class DefenseRuleSmokeTest {
    public static void main(String[] args) {
        testSummonDelayAndDefendingMinion();
        testAllDefendersMustBeDefeated();
        testDamageSpellWithDefenders();
        System.out.println("DEFENSE_RULE_SMOKE_TEST_OK");
    }

    private static void testSummonDelayAndDefendingMinion() {
        Fixture fixture = new Fixture();
        fixture.summonAttacker();
        expectRuleFailure(fixture::attackHero);
        require(fixture.defender.getHero().getHealth() == 30,
                "Minion mới triệu hồi không được đánh Hero ngay");
        fixture.summonDefenderAndReturnTurn();
        MinionCard guard = fixture.defender.getBoard().get(0);
        require(!guard.canAttack(), "Lá phòng thủ mới triệu hồi vẫn đang chờ lượt");
        fixture.assertHeroProtected();
        fixture.attackMinion(guard);
        require(fixture.defender.getBoard().isEmpty(), "Phải loại lá phòng thủ đã chết khỏi bàn");
        require(fixture.attacker.getCurrentHealth() == 4, "Minion phòng thủ phải phản đòn");
        require(!fixture.attacker.canAttack(), "Tấn công lá phòng thủ phải tiêu hao lượt đánh");
        expectRuleFailure(fixture::attackHero);
        fixture.nextOwnTurn();
        fixture.attackHero();
        require(fixture.defender.getHero().getHealth() == 27,
                "Được đánh Hero khi bàn đối phương không còn Minion");
    }

    private static void testAllDefendersMustBeDefeated() {
        Fixture fixture = new Fixture();
        fixture.summonAttacker();
        fixture.summonDefenderAndReturnTurn();
        MinionCard secondGuard = guard();
        fixture.defender.summon(secondGuard);
        fixture.assertHeroProtected();
        fixture.attackMinion(fixture.defender.getBoard().get(0));
        require(fixture.defender.getBoard().size() == 1, "Chỉ lá phòng thủ bị hạ được loại khỏi bàn");
        fixture.nextOwnTurn();
        fixture.assertHeroProtected();
        fixture.attackMinion(secondGuard);
        fixture.nextOwnTurn();
        fixture.attackHero();
        require(fixture.defender.getHero().getHealth() == 27,
                "Chỉ được đánh Hero sau khi hạ hết các lá phòng thủ");
    }

    private static void testDamageSpellWithDefenders() {
        Fixture fixture = new Fixture();
        fixture.summonAttacker();
        fixture.summonDefenderAndReturnTurn();
        Card spell = new SpellCard("SPELL", "Damage", 0,
                CardType.DAMAGE_SPELL, 4, "Damage spell");
        fixture.owner.getHand().add(spell);
        fixture.engine.playCard(fixture.game, fixture.owner.getId(), spell.getInstanceId());
        require(fixture.defender.getHero().getHealth() == 26,
                "Damage Spell vẫn gây sát thương trực tiếp theo luật của phép");
        require(fixture.defender.hasDefendingMinions(), "Phép không loại lá phòng thủ khỏi bàn");
    }

    private static MinionCard guard() {
        return new MinionCard("GUARD", "Guard", 1, 1, 2, "Defending minion");
    }

    private static List<Card> deck(MinionCard template) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            cards.add(template.copy());
        }
        return cards;
    }

    private static void expectRuleFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Tấn công không hợp lệ phải bị từ chối");
        } catch (GameRuleException expected) {
            // Expected: rejected attacks must leave the match unchanged.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class Fixture {
        final GameEngine engine = new GameEngine();
        final Player owner = new Player("A", "TEST", "Test",
                deck(new MinionCard("ATTACKER", "Attacker", 1, 3, 5, "Attacking minion")));
        final Player defender = new Player("B", "TEST", "Test", deck(guard()));
        final GameState game = new GameState(owner, defender);
        MinionCard attacker;

        void summonAttacker() {
            attacker = (MinionCard) owner.getHand().get(0);
            engine.playCard(game, owner.getId(), attacker.getInstanceId());
        }

        void summonDefenderAndReturnTurn() {
            engine.endTurn(game, owner.getId());
            engine.playCard(game, defender.getId(), defender.getHand().get(0).getInstanceId());
            engine.endTurn(game, defender.getId());
        }

        void nextOwnTurn() {
            engine.endTurn(game, owner.getId());
            engine.endTurn(game, defender.getId());
        }

        void attackHero() {
            engine.attack(game, owner.getId(), attacker.getInstanceId(), GameEngine.HERO_TARGET);
        }

        void attackMinion(MinionCard target) {
            engine.attack(game, owner.getId(), attacker.getInstanceId(), target.getInstanceId());
        }

        void assertHeroProtected() {
            int health = defender.getHero().getHealth();
            int attackerHealth = attacker.getCurrentHealth();
            int guardHealth = defender.getBoard().get(0).getCurrentHealth();
            expectRuleFailure(this::attackHero);
            require(defender.getHero().getHealth() == health, "Đòn bị chặn không được gây sát thương Hero");
            require(attacker.canAttack(), "Đòn bị chặn không được tiêu hao lượt đánh");
            require(attacker.getCurrentHealth() == attackerHealth, "Đòn bị chặn không làm mất máu Minion");
            require(defender.getBoard().get(0).getCurrentHealth() == guardHealth,
                    "Đòn bị chặn không làm mất máu lá phòng thủ");
        }
    }
}
