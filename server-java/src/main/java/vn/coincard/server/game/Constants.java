package vn.coincard.server.game;

/** Game tuning numbers. */
public final class Constants {
  public static final int MAX_HERO_HEALTH = 30;
  public static final int MAX_MANA = 10;
  public static final int MAX_BOARD_SIZE = 7;
  public static final int MAX_HAND_SIZE = 6;
  public static final int FIRST_PLAYER_HAND_SIZE = 3;
  public static final int SECOND_PLAYER_HAND_SIZE = 4;
  public static final int DECK_SIZE = 30;
  public static final long ROOM_IDLE_TTL_MS = 10 * 60 * 1000L;
  public static final long ROOM_SWEEP_INTERVAL_MS = 60 * 1000L;

  private Constants() {}
}
