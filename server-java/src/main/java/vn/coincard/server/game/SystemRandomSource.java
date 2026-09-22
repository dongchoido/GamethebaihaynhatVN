package vn.coincard.server.game;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Production random source; tests can provide a fixed implementation. */
@Component
public final class SystemRandomSource implements RandomSource {
  private static final Logger log = LoggerFactory.getLogger(SystemRandomSource.class);
  private final Random seeded;

  public SystemRandomSource(@Value("${coincard.random-seed:}") String configuredSeed) {
    seeded = parseSeed(configuredSeed);
  }

  @Override
  public synchronized int nextInt(int bound) {
    return seeded == null ? ThreadLocalRandom.current().nextInt(bound) : seeded.nextInt(bound);
  }

  private Random parseSeed(String configuredSeed) {
    if (configuredSeed == null || configuredSeed.isBlank()) return null;
    try {
      return new Random(Long.parseLong(configuredSeed));
    } catch (NumberFormatException error) {
      log.warn("Bỏ qua COINCARD_RANDOM_SEED không hợp lệ.");
      return null;
    }
  }
}
