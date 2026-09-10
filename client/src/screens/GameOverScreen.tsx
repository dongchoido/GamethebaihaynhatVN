import { ServerEvents, type GameStatePayload } from '@coincard/shared';
import { useEffect } from 'react';
import { socketService } from '../socket/socketService';
import { useGameStore } from '../store/gameStore';
import { getHeroAsset, resolveAsset } from '../assets/assetRegistry';

// Màn hình Victory / Defeat — splash theo hero class của mình.
export function GameOverScreen() {
  const { session, gameState, setGameState, reset } = useGameStore();

  useEffect(() => {
    const socket = socketService.getSocket();
    const onGameState = ({ gameState: next }: GameStatePayload) => setGameState(next);
    socket.on(ServerEvents.GAME_STATE_UPDATED, onGameState);
    return () => {
      socket.off(ServerEvents.GAME_STATE_UPDATED, onGameState);
    };
  }, [setGameState]);

  const myPlayer = gameState?.players.find((p) => p.playerId === session?.playerId) ?? null;
  const isWinner = !!gameState && gameState.winnerId === session?.playerId;
  const heroClass = myPlayer?.hero.heroClass ?? 'MAGE';
  const splash = isWinner
    ? getHeroAsset(heroClass).winSplash
    : getHeroAsset(heroClass).defeatSplash;

  return (
    <div className="gameover-screen" style={{ backgroundImage: `url(${resolveAsset(splash)})` }}>
      <h1 className={isWinner ? 'victory-text' : 'defeat-text'}>{isWinner ? 'VICTORY' : 'DEFEAT'}</h1>
      <button type="button" className="btn-primary" onClick={reset}>
        Chơi lại
      </button>
    </div>
  );
}
