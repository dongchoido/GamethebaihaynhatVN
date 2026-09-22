package vn.coincard.server.game.effects;

import java.util.List;
import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Player;
import vn.coincard.server.model.GameCharacter;

/** Inputs available to one card-effect strategy. */
public class EffectContext {
  public final Player player;
  public final Player opponent;
  public final GameCharacter target; // Hero | Minion | null
  public final List<Minion> areaTargets; // for AOE_* targets, may be null

  public EffectContext(Player player, Player opponent, GameCharacter target, List<Minion> areaTargets) {
    this.player = player;
    this.opponent = opponent;
    this.target = target;
    this.areaTargets = areaTargets;
  }
}
