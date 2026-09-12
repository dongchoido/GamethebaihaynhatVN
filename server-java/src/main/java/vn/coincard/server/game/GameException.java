package vn.coincard.server.game;

/**
 * Domain errors mapped to ACTION_REJECTED { code, message }.
 * Mirror of server/src/game/errors.ts (GameRuleError hierarchy).
 */
public abstract class GameException extends RuntimeException {
  public abstract String code();

  protected GameException(String message) {
    super(message);
  }

  public static final class NotPlayerTurn extends GameException {
    public NotPlayerTurn() { super("It is not your turn."); }
    @Override public String code() { return "NOT_PLAYER_TURN"; }
  }

  public static final class NotEnoughMana extends GameException {
    public NotEnoughMana() { super("Not enough mana."); }
    @Override public String code() { return "NOT_ENOUGH_MANA"; }
  }

  public static final class InvalidTarget extends GameException {
    public InvalidTarget(String message) { super(message); }
    public InvalidTarget() { super("Invalid target."); }
    @Override public String code() { return "INVALID_TARGET"; }
  }

  public static final class BoardFull extends GameException {
    public BoardFull() { super("Your board is full."); }
    @Override public String code() { return "BOARD_FULL"; }
  }

  public static final class CardNotInHand extends GameException {
    public CardNotInHand() { super("Card is not in your hand."); }
    @Override public String code() { return "CARD_NOT_IN_HAND"; }
  }

  public static final class RoomFull extends GameException {
    public RoomFull() { super("Room already has 2 players."); }
    @Override public String code() { return "ROOM_FULL"; }
  }

  public static final class RoomNotFound extends GameException {
    public RoomNotFound() { super("Room not found."); }
    @Override public String code() { return "ROOM_NOT_FOUND"; }
  }

  public static final class ReconnectFailed extends GameException {
    public ReconnectFailed() { super("Phiên chơi đã hết hạn (phòng không còn). Hãy tạo phòng mới."); }
    @Override public String code() { return "RECONNECT_FAILED"; }
  }

  public static final class InvalidPayload extends GameException {
    public InvalidPayload(String message) { super(message); }
    public InvalidPayload() { super("Dữ liệu gửi lên không hợp lệ."); }
    @Override public String code() { return "INVALID_PAYLOAD"; }
  }

  public static final class GameNotRunning extends GameException {
    public GameNotRunning() { super("Game is not in PLAYING status."); }
    @Override public String code() { return "GAME_NOT_RUNNING"; }
  }

  public static final class HandFull extends GameException {
    public HandFull() { super("Tay đã đầy (tối đa 6 lá)."); }
    @Override public String code() { return "HAND_FULL"; }
  }

  public static final class DeckEmpty extends GameException {
    public DeckEmpty() { super("Bộ bài đã hết."); }
    @Override public String code() { return "DECK_EMPTY"; }
  }

  public static final class AlreadyDrew extends GameException {
    public AlreadyDrew() { super("Mỗi turn chỉ được rút 1 lá từ bộ bài."); }
    @Override public String code() { return "ALREADY_DREW"; }
  }

  public static final class TauntRequired extends GameException {
    public TauntRequired() { super("Phải tấn công quái Taunt trước."); }
    @Override public String code() { return "TAUNT_REQUIRED"; }
  }

  public static final class MinionsBlockHero extends GameException {
    public MinionsBlockHero() { super("Đối thủ còn minion trên bàn — phải tấn công minion trước."); }
    @Override public String code() { return "MINIONS_BLOCK_HERO"; }
  }
}
