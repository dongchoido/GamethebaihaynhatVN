import { useEffect, useMemo, useRef, useState } from 'react';
import {
  GameStatus,
  ServerEvents,
  effectNeedsTarget,
  type ActionRejectedResponse,
  type CardDefinition,
  type GameOverResponse,
  type GameStatePayload,
  type TurnChangedResponse,
} from '@coincard/shared';
import { socketService } from '../socket/socketService';
import { useConnection } from '../socket/useConnection';
import { useGameStore } from '../store/gameStore';
import { CardView } from '../components/CardView';
import { MinionView } from '../components/MinionView';
import { HeroView } from '../components/HeroView';
import { ManaBar } from '../components/ManaBar';
import { DebugPanel } from '../components/DebugPanel';
import { playSound, resolveAsset, SOUND, UI_IMAGE } from '../assets/assetRegistry';

// Dùng chung helper với server — chỉ effect cần chọn tay mới highlight.
// Effect tự resolve (HEAL hero, AOE...) không góp id vào để khỏi highlight sai.
function spellNeedsTarget(card: CardDefinition): boolean {
  return (
    card.type === 'SPELL' && card.effects.some((e) => effectNeedsTarget(e.target))
  );
}

function validTargetIds(card: CardDefinition, myId: string, oppId: string, myBoard: string[], oppBoard: string[]): Set<string> {
  const ids = new Set<string>();
  for (const effect of card.effects) {
    if (!effectNeedsTarget(effect.target)) {
      continue;
    }
    switch (effect.target) {
      case 'ENEMY_CHARACTER':
        oppBoard.forEach((id) => ids.add(id));
        ids.add(oppId);
        break;
      case 'ANY_CHARACTER':
        myBoard.forEach((id) => ids.add(id));
        oppBoard.forEach((id) => ids.add(id));
        ids.add(myId);
        ids.add(oppId);
        break;
      case 'ENEMY_MINION':
        oppBoard.forEach((id) => ids.add(id));
        break;
      case 'FRIENDLY_MINION':
        myBoard.forEach((id) => ids.add(id));
        break;
      case 'ANY_MINION':
        myBoard.forEach((id) => ids.add(id));
        oppBoard.forEach((id) => ids.add(id));
        break;
      case 'ENEMY_HERO':
        ids.add(oppId);
        break;
      case 'SELF':
        ids.add(myId);
        break;
      default:
        break;
    }
  }
  return ids;
}

// Màn hình trận đấu — click để chơi (core gameplay không phụ thuộc drag/drop).
export function GameScreen() {
  const connected = useConnection();
  const [opponentAway, setOpponentAway] = useState(false);
  const { session, gameState, setGameState, setPhase, setLastError, lastError, reset } = useGameStore();
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);
  const [selectedAttackerId, setSelectedAttackerId] = useState<string | null>(null);
  const [endTurnPending, setEndTurnPending] = useState(false);
  const [drawPending, setDrawPending] = useState(false);
  const prevTurnRef = useRef<number>(0);
  const MAX_HAND = 6;

  // Preload ảnh nút để bấm là hiện ngay, không nháy.
  useEffect(() => {
    for (const key of [UI_IMAGE.endTurn, UI_IMAGE.surrender]) {
      const img = new Image();
      img.src = resolveAsset(key);
    }
  }, []);

  useEffect(() => {
    const socket = socketService.getSocket();
    const onGameState = ({ gameState: next }: GameStatePayload) => {
      if (next.turn !== prevTurnRef.current) {
        prevTurnRef.current = next.turn;
        if (next.turn > 1) {
          playSound(SOUND.endTurn);
        }
      }
      setGameState(next);
      if (next.status === GameStatus.FINISHED) {
        setPhase('over');
      }
      setSelectedCardId(null);
      setSelectedAttackerId(null);
      setEndTurnPending(false);
      setDrawPending(false);
    };
    const onRejected = (res: ActionRejectedResponse) => {
      // Game trên server đã mất → về home sạch thay vì kẹt.
      if (res.code === 'RECONNECT_FAILED') {
        reset();
        return;
      }
      setLastError(`${res.code}: ${res.message}`);
      setEndTurnPending(false);
      setDrawPending(false);
    };
    const onTurnChanged = (_res: TurnChangedResponse) => {
      // statusMessage trong state đã đủ; giữ handler để log/debug.
    };
    const onGameOver = (_res: GameOverResponse) => {
      if (_res.winnerId === session?.playerId) playSound(SOUND.victory);
      setPhase('over');
    };
    const onLeft = ({ playerId }: { playerId: string }) => { if (playerId !== session?.playerId) setOpponentAway(true); };
    const onJoined = () => { setOpponentAway(false); setLastError(null); };
    socket.on(ServerEvents.PLAYER_DISCONNECTED, onLeft);
    socket.on(ServerEvents.PLAYER_JOINED, onJoined);
    socket.on(ServerEvents.GAME_STATE_UPDATED, onGameState);
    socket.on(ServerEvents.ACTION_REJECTED, onRejected);
    socket.on(ServerEvents.TURN_CHANGED, onTurnChanged);
    socket.on(ServerEvents.GAME_OVER, onGameOver);
    return () => {
      socket.off(ServerEvents.PLAYER_DISCONNECTED, onLeft);
      socket.off(ServerEvents.PLAYER_JOINED, onJoined);
      socket.off(ServerEvents.GAME_STATE_UPDATED, onGameState);
      socket.off(ServerEvents.ACTION_REJECTED, onRejected);
      socket.off(ServerEvents.TURN_CHANGED, onTurnChanged);
      socket.off(ServerEvents.GAME_OVER, onGameOver);
    };
  }, [setGameState, setPhase, setLastError, reset, session?.playerId]);

  const myPlayer = gameState?.players.find((p) => p.playerId === session?.playerId) ?? null;
  const oppPlayer = gameState?.players.find((p) => p.playerId !== session?.playerId) ?? null;
  const isMyTurn =
    connected &&
    !!gameState &&
    !!session &&
    gameState.status === GameStatus.PLAYING &&
    gameState.activePlayerId === session.playerId;

  const selectedCard = useMemo(
    () => myPlayer?.hand.find((c) => c.id === selectedCardId) ?? null,
    [myPlayer, selectedCardId],
  );

  const targetIds = useMemo(() => {
    if (!selectedCard || !myPlayer || !oppPlayer) return new Set<string>();
    return validTargetIds(
      selectedCard,
      myPlayer.playerId,
      oppPlayer.playerId,
      myPlayer.board.map((m) => m.instanceId),
      oppPlayer.board.map((m) => m.instanceId),
    );
  }, [selectedCard, myPlayer, oppPlayer]);

  if (!gameState || !myPlayer || !oppPlayer || !session) {
    return <div className="game-screen"><p>Đang tải trận đấu...</p></div>;
  }

  const sendPlayCard = (cardId: string, targetId?: string) => {
    socketService.playCard({ gameId: gameState.gameId, cardInstanceId: cardId, targetId });
    playSound(SOUND.playCard);
    setSelectedCardId(null);
  };

  const handleHandCardClick = (card: CardDefinition) => {
    setLastError(null);
    if (!isMyTurn) return;
    if (selectedCardId === card.id) {
      setSelectedCardId(null); // bấm lại để hủy chọn
      return;
    }
    if (card.manaCost > myPlayer.mana) return; // không đủ mana → dim, không chọn
    setSelectedAttackerId(null);
    if (card.type === 'MINION' || !spellNeedsTarget(card)) {
      sendPlayCard(card.id);
      return;
    }
    setSelectedCardId(card.id); // spell cần target → đợi chọn target
  };

  const oppHasTaunt = oppPlayer?.board.some((m) => m.hasTaunt) ?? false;

  const handleMinionClick = (instanceId: string, isMine: boolean) => {
    setLastError(null);
    if (!isMyTurn) return;
    // Đang chọn spell: đúng target thì cast, click sai chỗ thì hủy chọn
    // (không để kẹt vừa chọn spell vừa chọn attacker).
    if (selectedCard) {
      if (targetIds.has(instanceId)) {
        sendPlayCard(selectedCard.id, instanceId);
      } else {
        setSelectedCardId(null);
        if (!isMine) return;
      }
    }
    if (isMine) {
      // Ưu tiên 2: chọn attacker của mình
      const minion = myPlayer.board.find((m) => m.instanceId === instanceId);
      if (minion?.canAttack) {
        setSelectedCardId(null);
        setSelectedAttackerId(selectedAttackerId === instanceId ? null : instanceId);
      }
      return;
    }
    // Ưu tiên 3: đang chọn attacker → tấn công minion địch
    if (selectedAttackerId) {
      socketService.attack({
        gameId: gameState.gameId,
        attackerId: selectedAttackerId,
        targetId: instanceId,
      });
      playSound(SOUND.attack);
      setSelectedAttackerId(null);
    }
  };

  const handleHeroClick = (playerId: string, isMine: boolean) => {
    setLastError(null);
    if (!isMyTurn) return;
    if (selectedCard && targetIds.has(playerId)) {
      sendPlayCard(selectedCard.id, playerId);
      return;
    }
    if (!isMine && selectedAttackerId) {
      if (oppHasTaunt) {
        setLastError('TAUNT_REQUIRED: Phải tấn công quái Taunt trước.');
        return;
      }
      socketService.attack({
        gameId: gameState.gameId,
        attackerId: selectedAttackerId,
        targetId: playerId,
      });
      playSound(SOUND.attack);
      setSelectedAttackerId(null);
    }
  };

  const handleEndTurn = () => {
    if (!isMyTurn || endTurnPending) return;
    setEndTurnPending(true);
    socketService.endTurn(gameState.gameId);
  };

  const handleHeroPower = () => {
    if (!isMyTurn) return;
    socketService.useHeroPower(gameState.gameId);
    playSound(SOUND.play);
  };

  const handleDraw = () => {
    if (!isMyTurn || drawPending || gameState.manualDrawUsed) return;
    setLastError(null);
    setDrawPending(true);
    socketService.drawCard(gameState.gameId);
    playSound(SOUND.play);
  };

  const handleConcede = () => {
    if (!connected) return;
    socketService.concede(gameState.gameId);
  };

  const oppHandBacks = Array.from({ length: oppPlayer.handCount }, (_, i) => i);

  return (
    <div className="game-screen" style={{ backgroundImage: `radial-gradient(ellipse at center, rgba(33, 20, 12, 0.28), rgba(12, 8, 6, 0.88)), url(${resolveAsset(UI_IMAGE.playground)})` }}>
      {!connected && <p role="status">Mất kết nối với server. Đang kết nối lại...</p>}
      {connected && opponentAway && <p role="status">Đối thủ mất kết nối. Đang chờ quay lại...</p>}
      {/* Đối thủ */}
      <div className="opponent-area">
        <div className="opponent-hand">
          {oppHandBacks.map((i) => (
            <img key={i} className="card-back-mini" src={resolveAsset(UI_IMAGE.cardBack)} alt="enemy card" draggable={false} />
          ))}
        </div>
        <HeroView
          hero={oppPlayer.hero}
          powerUsable={false}
          targetable={isMyTurn && ((selectedAttackerId !== null && !oppHasTaunt) || (selectedCard !== null && targetIds.has(oppPlayer.playerId)))}
          onHeroClick={() => handleHeroClick(oppPlayer.playerId, false)}
          onPowerClick={() => undefined}
        />
      </div>
      <div className="board-row opponent-board">
        {oppPlayer.board.map((m) => (
          <MinionView
            key={m.instanceId}
            minion={m}
            selectable={false}
            selected={false}
            targetable={isMyTurn && ((selectedAttackerId !== null && (!oppHasTaunt || m.hasTaunt)) || targetIds.has(m.instanceId))}
            onSelect={() => handleMinionClick(m.instanceId, false)}
          />
        ))}
        {oppPlayer.board.length === 0 && <span className="board-empty">—</span>}
      </div>

      {/* Thanh trạng thái */}
      <div className="message-bar">
        <span className="message-pill">{gameState.statusMessage || (isMyTurn ? 'Lượt của bạn' : 'Lượt đối thủ')}</span>
        {lastError && <span className="error-text">{lastError}</span>}
      </div>

      {/* Mình */}
      <div className="board-row my-board">
        {myPlayer.board.map((m) => (
          <MinionView
            key={m.instanceId}
            minion={m}
            selectable={isMyTurn && m.canAttack}
            selected={selectedAttackerId === m.instanceId}
            targetable={isMyTurn && selectedCard !== null && targetIds.has(m.instanceId)}
            onSelect={() => handleMinionClick(m.instanceId, true)}
          />
        ))}
        {myPlayer.board.length === 0 && <span className="board-empty">—</span>}
      </div>
      <div className="my-area">
        <div className="my-hand-wrap">
          <div className="hand-count">{myPlayer.hand.length}/{MAX_HAND}</div>
          <div className="my-hand">
            {myPlayer.hand.map((card) => (
              <CardView
                key={card.id}
                card={card}
                playable={isMyTurn && card.manaCost <= myPlayer.mana}
                selected={selectedCardId === card.id}
                onSelect={() => handleHandCardClick(card)}
              />
            ))}
          </div>
        </div>
        <div className="my-hero-mana">
          <HeroView
            hero={myPlayer.hero}
            powerUsable={isMyTurn && myPlayer.mana >= myPlayer.hero.powerCost}
            targetable={isMyTurn && selectedCard !== null && targetIds.has(myPlayer.playerId)}
            onHeroClick={() => handleHeroClick(myPlayer.playerId, true)}
            onPowerClick={handleHeroPower}
          />
          <ManaBar mana={myPlayer.mana} maxMana={myPlayer.maxMana} />
        </div>
        <div className="my-actions">
          <button
            type="button"
            className="deck-pile"
            onClick={handleDraw}
            disabled={!isMyTurn || drawPending || gameState.manualDrawUsed || myPlayer.hand.length >= MAX_HAND || myPlayer.deckCount <= 0}
            title={myPlayer.hand.length >= MAX_HAND ? 'Tay đầy (6/6)' : 'Rút 1 lá (mỗi turn 1 lần)'}
          >
            <img src={resolveAsset(UI_IMAGE.cardBack)} alt="Bộ bài" draggable={false} />
            <span className="deck-count">{myPlayer.deckCount}</span>
          </button>
          <button type="button" className="end-turn-btn" onClick={handleEndTurn} disabled={!isMyTurn || endTurnPending} title="End turn">
            <img
              src={resolveAsset(UI_IMAGE.endTurn)}
              alt="End turn"
              draggable={false}
            />
          </button>
          <button type="button" className="btn-concede" onClick={handleConcede} title="Đầu hàng">
            <img src={resolveAsset(UI_IMAGE.surrender)} alt="Đầu hàng" draggable={false} />
          </button>
        </div>
      </div>
      <DebugPanel gameState={gameState} />
    </div>
  );
}
