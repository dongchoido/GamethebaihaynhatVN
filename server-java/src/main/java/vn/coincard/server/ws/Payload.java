package vn.coincard.server.ws;

import java.util.Map;
import vn.coincard.server.game.GameException;

/** Strict validation helpers for incoming WebSocket payloads. */
public final class Payload {
  private Payload() {}

  public static String requiredString(Map<String, Object> payload, String field, int maxLength) {
    Object value = payload.get(field);
    if (!(value instanceof String s) || s.trim().isEmpty() || s.length() > maxLength) {
      throw new GameException.InvalidPayload(field + " không hợp lệ.");
    }
    return s.trim();
  }

  public static String optionalString(Map<String, Object> payload, String field, int maxLength) {
    Object value = payload.get(field);
    if (value == null) return null;
    if (!(value instanceof String s) || s.isEmpty() || s.length() > maxLength) {
      throw new GameException.InvalidPayload(field + " không hợp lệ.");
    }
    return s;
  }
}
