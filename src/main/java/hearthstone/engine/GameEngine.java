package hearthstone.engine;

import hearthstone.model.card.Card;
import hearthstone.model.card.CardType;
import hearthstone.model.card.MinionCard;
import hearthstone.model.card.SpellCard;
import hearthstone.model.game.GameState;
import hearthstone.model.game.GameStatus;
import hearthstone.model.game.Player;

public class GameEngine {

    public static final String HERO_TARGET = "HERO";

    public void playCard(GameState game, String playerId, String cardId) {
        Player player = requireCurrentPlayer(game, playerId);
        Player opponent = game.getOpponent();
        Card card = player.findCardInHand(cardId);

        if (!player.hasEnoughMana(card.getManaCost())) {
            throw new GameRuleException("Không đủ mana để đánh lá này");
        }

        if (card instanceof MinionCard minion) {
            if (player.getBoard().size() >= Player.MAX_BOARD_SIZE) {
                throw new GameRuleException("Bàn đã đủ 7 Minion");
            }
            player.spendMana(card.getManaCost());
            player.removeCardFromHand(card);
            player.summon(minion);
        } else if (card instanceof SpellCard spell) {
            player.spendMana(card.getManaCost());
            player.removeCardFromHand(card);
            resolveSpell(spell, player, opponent);
        }

        cleanAndCheckWinner(game);
    }

    public void attack(GameState game, String playerId,
                       String attackerId, String targetId) {
        Player player = requireCurrentPlayer(game, playerId);
        Player opponent = game.getOpponent();
        MinionCard attacker = player.findMinion(attackerId);

        if (!attacker.canAttack()) {
            throw new GameRuleException("Minion này chưa thể tấn công");
        }

        if (HERO_TARGET.equals(targetId)) {
            opponent.getHero().takeDamage(attacker.getAttack());
        } else {
            MinionCard defender = opponent.findMinion(targetId);
            int attackerDamage = attacker.getAttack();
            int defenderDamage = defender.getAttack();
            defender.takeDamage(attackerDamage);
            attacker.takeDamage(defenderDamage);
        }

        attacker.exhaust();
        cleanAndCheckWinner(game);
    }

    public void endTurn(GameState game, String playerId) {
        requireCurrentPlayer(game, playerId);
        game.switchTurn();
        cleanAndCheckWinner(game);
    }

    private Player requireCurrentPlayer(GameState game, String playerId) {
        if (game.getStatus() == GameStatus.FINISHED) {
            throw new GameRuleException("Trận đấu đã kết thúc");
        }
        if (!game.getCurrentPlayerId().equals(playerId)) {
            throw new GameRuleException("Chưa đến lượt người chơi này");
        }
        return game.getCurrentPlayer();
    }

    private void resolveSpell(SpellCard spell, Player player, Player opponent) {
        if (spell.getType() == CardType.DAMAGE_SPELL) {
            opponent.getHero().takeDamage(spell.getPower());
        } else if (spell.getType() == CardType.HEAL_SPELL) {
            player.getHero().heal(spell.getPower());
        }
    }

    private void cleanAndCheckWinner(GameState game) {
        game.getPlayer1().removeDeadMinions();
        game.getPlayer2().removeDeadMinions();

        if (game.getPlayer1().getHero().isDead()) {
            game.finish(game.getPlayer2().getName());
        } else if (game.getPlayer2().getHero().isDead()) {
            game.finish(game.getPlayer1().getName());
        }
    }
}
