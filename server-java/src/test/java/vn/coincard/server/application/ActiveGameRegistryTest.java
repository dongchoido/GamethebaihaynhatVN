package vn.coincard.server.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActiveGameRegistryTest {
  @Test
  void tracksOnlyTheCurrentGameForEachRoom() {
    ActiveGameRegistry registry = new ActiveGameRegistry();
    registry.associate("ROOM1", "game-1");

    assertTrue(registry.isCurrent("ROOM1", "game-1"));
    assertFalse(registry.isCurrent("ROOM1", "game-2"));

    registry.associate("ROOM1", "game-2");
    assertTrue(registry.isCurrent("ROOM1", "game-2"));

    registry.removeRoom("ROOM1");
    assertNull(registry.gameIdForRoom("ROOM1"));
  }
}
