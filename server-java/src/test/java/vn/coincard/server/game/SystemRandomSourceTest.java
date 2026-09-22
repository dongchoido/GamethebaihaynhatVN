package vn.coincard.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class SystemRandomSourceTest {
  @Test
  void configuredSeedProducesRepeatableValues() {
    SystemRandomSource first = new SystemRandomSource("101");
    SystemRandomSource second = new SystemRandomSource("101");

    assertEquals(first.nextInt(1000), second.nextInt(1000));
    assertEquals(first.nextInt(1000), second.nextInt(1000));
  }

  @Test
  void invalidConfiguredSeedFallsBackToProductionRandomness() {
    assertDoesNotThrow(() -> new SystemRandomSource("not-a-number").nextInt(2));
  }
}
