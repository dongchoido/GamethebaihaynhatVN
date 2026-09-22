package vn.coincard.server.game;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Injectable randomness boundary for deterministic game tests. */
@FunctionalInterface
public interface RandomSource {
  int nextInt(int bound);

  default <T> void shuffle(List<T> values) {
    for (int index = values.size() - 1; index > 0; index--) {
      Collections.swap(values, index, nextInt(index + 1));
    }
  }

  static RandomSource threadLocal() {
    return bound -> ThreadLocalRandom.current().nextInt(bound);
  }
}
