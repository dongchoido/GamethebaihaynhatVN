package vn.coincard.server.game;

/** Domain errors mapped to ACTION_REJECTED { code, message }. */
public abstract class GameException extends RuntimeException {
  public abstract ErrorCode errorCode();

  public final String code() {
    return errorCode().name();
  }

  protected GameException(String message) {
    super(message);
  }

  public static final class NotPlayerTurn extends GameException {
    public NotPlayerTurn() { super("It is not your turn."); }
    @Override public ErrorCode errorCode() { return ErrorCode.NOT_PLAYER_TURN; }
  }

  public static final class NotEnoughMana extends GameException {
    public NotEnoughMana() { super("Not enough mana."); }
    @Override public ErrorCode errorCode() { return ErrorCode.NOT_ENOUGH_MANA; }
  }

  public static final class InvalidTarget extends GameException {
    public InvalidTarget(String message) { super(message); }
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_TARGET; }
  }

  public static final class BoardFull extends GameException {
    public BoardFull() { super("Your board is full."); }
    @Override public ErrorCode errorCode() { return ErrorCode.BOARD_FULL; }
  }

  public static final class CardNotInHand extends GameException {
    public CardNotInHand() { super("Card is not in your hand."); }
    @Override public ErrorCode errorCode() { return ErrorCode.CARD_NOT_IN_HAND; }
  }

  public static final class InvalidDeck extends GameException {
    public InvalidDeck(String message) { super(message); }
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_DECK; }
  }

  public static final class RoomFull extends GameException {
    public RoomFull() { super("Room already has 2 players."); }
    @Override public ErrorCode errorCode() { return ErrorCode.ROOM_FULL; }
  }

  public static final class RoomNotFound extends GameException {
    public RoomNotFound() { super("Room not found."); }
    @Override public ErrorCode errorCode() { return ErrorCode.ROOM_NOT_FOUND; }
  }

  public static final class ReconnectFailed extends GameException {
    public ReconnectFailed() { super("Phiên chơi đã hết hạn (phòng không còn). Hãy tạo phòng mới."); }
    @Override public ErrorCode errorCode() { return ErrorCode.RECONNECT_FAILED; }
  }

  public static final class InvalidPayload extends GameException {
    public InvalidPayload(String message) { super(message); }
    public InvalidPayload() { super("Dữ liệu gửi lên không hợp lệ."); }
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_PAYLOAD; }
  }

  public static final class InvalidCommand extends GameException {
    public InvalidCommand(String message) { super(message); }
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_COMMAND; }
  }

  public static final class InternalError extends GameException {
    public InternalError() { super("Server không thể xử lý yêu cầu này."); }
    @Override public ErrorCode errorCode() { return ErrorCode.INTERNAL_ERROR; }
  }

  public static final class GameNotRunning extends GameException {
    public GameNotRunning() { super("Game is not in PLAYING status."); }
    @Override public ErrorCode errorCode() { return ErrorCode.GAME_NOT_RUNNING; }
  }

  public static final class HeroPowerAlreadyUsed extends GameException {
    public HeroPowerAlreadyUsed() { super("Mỗi lượt chỉ được dùng hero power một lần."); }
    @Override public ErrorCode errorCode() { return ErrorCode.HERO_POWER_ALREADY_USED; }
  }

  public static final class TauntRequired extends GameException {
    public TauntRequired() { super("Phải tấn công quái Taunt trước."); }
    @Override public ErrorCode errorCode() { return ErrorCode.TAUNT_REQUIRED; }
  }

}
