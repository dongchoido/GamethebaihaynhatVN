import { useState } from 'react';
import type { GameState } from '@coincard/shared';
import { socketService } from '../socket/socketService';
import { useGameStore } from '../store/gameStore';

interface DebugPanelProps {
  gameState: GameState | null;
}

// Debug panel — chỉ hiện khi dev (import.meta.env.DEV).
export function DebugPanel({ gameState }: DebugPanelProps) {
  const { session } = useGameStore();
  const [copied, setCopied] = useState(false);

  if (!import.meta.env.DEV) {
    return null;
  }

  const myPlayer = gameState?.players.find((p) => p.playerId === session?.playerId) ?? null;

  const copyState = () => {
    if (!gameState) return;
    void navigator.clipboard
      ?.writeText(JSON.stringify(gameState, null, 2))
      .then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 1500);
      })
      .catch(() => undefined);
  };

  return (
    <div className="debug-panel">
      <div>gameId: {gameState?.gameId ?? '-'}</div>
      <div>playerId: {session?.playerId ?? '-'}</div>
      <div>activePlayer: {gameState?.activePlayerId ?? '-'}</div>
      <div>turn: {gameState?.turn ?? '-'}</div>
      <div>
        mana: {myPlayer?.mana ?? '-'}/{myPlayer?.maxMana ?? '-'}
      </div>
      <div>
        hand/deck: {myPlayer?.handCount ?? '-'}/{myPlayer?.deckCount ?? '-'}
      </div>
      <div>socket: {socketService.isConnected() ? 'connected' : 'disconnected'}</div>
      <div>roomCode: {session?.roomCode ?? '-'}</div>
      <button type="button" onClick={copyState}>
        {copied ? 'Copied!' : 'Copy GameState'}
      </button>
    </div>
  );
}
