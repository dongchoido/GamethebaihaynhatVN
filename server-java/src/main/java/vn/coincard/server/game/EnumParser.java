package vn.coincard.server.game;

import java.util.Locale;

/** Shared strict parser for enum values received from adapters. */
final class EnumParser {
  private EnumParser() {}

  static <E extends Enum<E>> E required(Class<E> enumType, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(enumType.getSimpleName() + " is required.");
    }
    try {
      return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException("Unsupported " + enumType.getSimpleName() + ": " + value, error);
    }
  }
}
