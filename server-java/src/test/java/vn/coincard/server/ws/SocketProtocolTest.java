package vn.coincard.server.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.coincard.server.game.ErrorCode;
import vn.coincard.server.game.GameException;
import vn.coincard.server.game.HeroClass;

class SocketProtocolTest {
  @Test
  void unknownWireEventsAreInvalidCommands() {
    GameException.InvalidCommand error = assertThrows(GameException.InvalidCommand.class,
        () -> SocketEvent.fromWire("DRAW_CARD"));

    assertEquals(ErrorCode.INVALID_COMMAND, error.errorCode());
    assertEquals(SocketEvent.SUBMIT_LOADOUT, SocketEvent.fromWire("SUBMIT_LOADOUT"));
  }

  @Test
  void typedRequestsTrimValuesAndRejectMalformedPayloads() {
    InboundRequests.SubmitLoadout request = new InboundRequests.SubmitLoadout(" ABC123 ",
        HeroClass.MAGE, List.of(" card-a "));

    assertEquals("ABC123", request.roomCode());
    assertEquals(List.of("card-a"), request.cardSlugs());
    assertThrows(IllegalArgumentException.class,
        () -> new InboundRequests.PlayCard("game", "card", " "));
    assertThrows(IllegalArgumentException.class,
        () -> new InboundRequests.SubmitLoadout("room", HeroClass.MAGE, List.of()));
  }
}
