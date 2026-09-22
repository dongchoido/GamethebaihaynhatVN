import { useEffect, useState } from 'react';
import {
  GameStatus,
  HeroClass,
  ServerEvents,
  type ActionRejectedResponse,
  type GameStatePayload,
  type PlayerJoinedResponse,
  type LoadoutAcceptedResponse,
} from '@coincard/shared';
import { socketService } from '../socket/socketService';
import { useConnection } from '../socket/useConnection';
import { useGameStore } from '../store/gameStore';
import { getHeroAsset, listHeroAssets, playSound, resolveAsset, SOUND, UI_IMAGE } from '../assets/assetRegistry';

// Sảnh chờ: hiện mã phòng, chọn hero, đợi đủ 2 người.
export function LobbyScreen() {
  const connected = useConnection();
  const { session, setPhase, setGameState, setSelectedHeroId, selectedHeroId, lobbyPlayers, setLobbyPlayers, setLastError, lastError, reset } = useGameStore();
  const [roomReady, setRoomReady] = useState(lobbyPlayers.length >= 2);

  useEffect(() => {
    const socket = socketService.getSocket();

    const onPlayerJoined = (res: PlayerJoinedResponse) => {
      setLobbyPlayers(res.players);
      const me = res.players.find(p => p.playerId === session?.playerId);
      if (me) setSelectedHeroId(me.ready ? me.heroClass ?? null : null);
      setLastError(null);
      if (res.players.length >= 2) {
        setRoomReady(true);
      }
    };
    const onRoomReady = () => setRoomReady(true);
    const onLoadoutAccepted = (res: LoadoutAcceptedResponse) => {
      setLobbyPlayers(res.players);
      setRoomReady(res.players.length >= 2);
      const me = res.players.find((p) => p.playerId === session?.playerId);
      if (me) setSelectedHeroId(me.heroClass ?? null);
      setLastError(null);
    };
    const onRejected = (res: ActionRejectedResponse) => {
      if (res.code === 'RECONNECT_FAILED') {
        reset();
        return;
      }
      setLastError(`${res.code}: ${res.message}`);
      if (res.code === 'GAME_START_FAILED') setSelectedHeroId(null);
    };
    const onDisconnected = () => {
      setLastError('Đối thủ đã mất kết nối. Đang chờ kết nối lại...');
    };
    const onGameState = ({ gameState }: GameStatePayload) => {
      setGameState(gameState);
      if (gameState.status === GameStatus.PLAYING) {
        playSound(SOUND.start);
        setPhase('game');
      } else if (gameState.status === GameStatus.FINISHED) {
        setPhase('over');
      }
    };

    socket.on(ServerEvents.PLAYER_JOINED, onPlayerJoined);
    socket.on(ServerEvents.ROOM_READY, onRoomReady);
    socket.on(ServerEvents.LOADOUT_ACCEPTED, onLoadoutAccepted);
    socket.on(ServerEvents.GAME_STATE_UPDATED, onGameState);
    socket.on(ServerEvents.ACTION_REJECTED, onRejected);
    socket.on(ServerEvents.PLAYER_DISCONNECTED, onDisconnected);
    return () => {
      socket.off(ServerEvents.PLAYER_JOINED, onPlayerJoined);
      socket.off(ServerEvents.ROOM_READY, onRoomReady);
      socket.off(ServerEvents.LOADOUT_ACCEPTED, onLoadoutAccepted);
      socket.off(ServerEvents.GAME_STATE_UPDATED, onGameState);
      socket.off(ServerEvents.ACTION_REJECTED, onRejected);
      socket.off(ServerEvents.PLAYER_DISCONNECTED, onDisconnected);
    };
  }, [setGameState, setPhase, setLobbyPlayers, setLastError, setSelectedHeroId, reset, session?.playerId]);

  const handleSelectHero = (heroClass: HeroClass) => {
    if (!session || !connected) return;
    setSelectedHeroId(heroClass);
    playSound(SOUND.heroSelect);
    setPhase('deck');
  };

  const handleBack = () => {
    socketService.disconnect();
    reset();
  };

  return (
    <div className="lobby-screen" style={{ backgroundImage: `url(${resolveAsset(UI_IMAGE.shopBackground)})` }}>
      <aside className="lobby-rail" aria-label="Người chơi trong phòng">
        {lobbyPlayers.map((p) => (
          <span key={p.playerId} className="lobby-slot filled" title={p.name}>
            {(p.name || '?').trim().charAt(0).toUpperCase()}
          </span>
        ))}
        {Array.from({ length: Math.max(0, 2 - lobbyPlayers.length) }).map((_, i) => (
          <span key={`empty-${i}`} className="lobby-slot" title="Đang đợi..." />
        ))}
        <button type="button" className="lobby-back" onClick={handleBack} title="Về trang chủ">
          <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
            <path d="M15 5l-7 7 7 7" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </aside>
      <div className="lobby-main">
        {!connected && <p role="status">Đang kết nối lại với server...</p>}
        <div className="lobby-banner">
          <span className="lobby-gem left" aria-hidden="true" />
          <div className="lobby-banner-text">
            <div className="lobby-room">
              Phòng: <span className="room-code">{session?.roomCode}</span>
            </div>
            <div className="lobby-players">
              Người chơi: {lobbyPlayers.length > 0 ? lobbyPlayers.map((p) => p.name).join(' vs ') : 'Đang đợi đối thủ...'}
            </div>
          </div>
          <span className="lobby-gem right" aria-hidden="true" />
        </div>
        {!roomReady && <div className="lobby-pill">WAIT FOR A FRIEND</div>}
        <h3 className="lobby-heading">Chọn hero của bạn</h3>
        {lastError && <p className="error-text" role="alert">{lastError}</p>}
        <div className="hero-select-row">
          {listHeroAssets().map((hero) => (
            <button
              key={hero.heroClass}
              type="button"
              className={selectedHeroId === hero.heroClass ? 'hero-card hero-card-selected' : 'hero-card'}
              onClick={() => handleSelectHero(hero.heroClass)}
            >
              <span className="hero-frame-gem" aria-hidden="true" />
              <span className="hero-portrait-wrap">
                <img src={resolveAsset(hero.portrait)} alt={getHeroAsset(hero.heroClass).name} draggable={false} />
              </span>
              <span className="hero-cost" title="Hero power cost">{hero.powerCost}</span>
              <span className="hero-name">{getHeroAsset(hero.heroClass).name}</span>
            </button>
          ))}
        </div>
        {roomReady && !selectedHeroId && <p className="lobby-status">Hãy chọn hero để sẵn sàng!</p>}
        {roomReady && selectedHeroId && <p className="lobby-status">Đã sẵn sàng — đợi đối thủ chọn hero...</p>}
      </div>
    </div>
  );
}
