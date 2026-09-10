// Lỗi domain để tầng socket map sang ACTION_REJECTED { code, message }
export abstract class GameRuleError extends Error {
  abstract readonly code: string;
}

export class NotPlayerTurnError extends GameRuleError {
  readonly code = 'NOT_PLAYER_TURN';
  constructor() {
    super('It is not your turn.');
  }
}

export class NotEnoughManaError extends GameRuleError {
  readonly code = 'NOT_ENOUGH_MANA';
  constructor() {
    super('Not enough mana.');
  }
}

export class InvalidTargetError extends GameRuleError {
  readonly code = 'INVALID_TARGET';
  constructor(message = 'Invalid target.') {
    super(message);
  }
}

export class BoardFullError extends GameRuleError {
  readonly code = 'BOARD_FULL';
  constructor() {
    super('Your board is full.');
  }
}

export class CardNotInHandError extends GameRuleError {
  readonly code = 'CARD_NOT_IN_HAND';
  constructor() {
    super('Card is not in your hand.');
  }
}

export class RoomFullError extends GameRuleError {
  readonly code = 'ROOM_FULL';
  constructor() {
    super('Room already has 2 players.');
  }
}

export class RoomNotFoundError extends GameRuleError {
  readonly code = 'ROOM_NOT_FOUND';
  constructor() {
    super('Room not found.');
  }
}

export class ReconnectFailedError extends GameRuleError {
  readonly code = 'RECONNECT_FAILED';
  constructor() {
    super('Phiên chơi đã hết hạn (phòng không còn). Hãy tạo phòng mới.');
  }
}

export class GameNotRunningError extends GameRuleError {
  readonly code = 'GAME_NOT_RUNNING';
  constructor() {
    super('Game is not in PLAYING status.');
  }
}
