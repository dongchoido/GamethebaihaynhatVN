package vn.coincard.server.game.effects;

import java.util.List;
import vn.coincard.server.game.Game;
import vn.coincard.server.game.Minion;
import vn.coincard.server.game.Player;

/** Mirror of EffectContext.ts */
public class EffectContext {
  public final Game game;
  public final Player player;
  public final Player opponent;
  public final Object target; // Minion | Player | null
  public final int value;
  public final List<Minion> areaTargets; // for AOE_* targets, may be null

  public EffectContext(Game game, Player player, Player opponent,
      Object target, int value, List<Minion> areaTargets) {
    this.game = game;
    this.player = player;
    this.opponent = opponent;
    this.target = target;
    this.value = value;
    this.areaTargets = areaTargets;
  }
}
