import { useEffect, useState } from 'react';
import {
  GameStatus,
  ServerEvents,
  type GameStatePayload,
  type PlayerJoinedResponse,
} from '@coincard/shared';
import { socketService } from '../socket/socketService';
import { useGameStore } from '../store/gameStore';
import { getHeroAsset, listHeroAssets, playSound, resolveAsset, SOUND, UI_IMAGE } from '../assets/assetRegistry';

// Sảnh chờ: hiện mã phòng, chọn hero, đợi đủ 2 người.
export function LobbyScreen() {
  const { session, setPhase, setGameState, setSelectedHeroId, selectedHeroId, lobbyPlayers, setLobbyPlayers } = useGameStore();
  const [roomReady, setRoomReady] = useState(lobbyPlayers.length >= 2);

  useEffect(() => {
    const socket = socketService.getSocket();

    const onPlayerJoined = (res: PlayerJoinedResponse) => {
      setLobbyPlayers(res.players);
      if (res.players.length >= 2) {
        setRoomReady(true);
      }
    };
    const onRoomReady = () => setRoomReady(true);
    const onGameState = ({ gameState }: GameStatePayload) => {
      setGameState(gameState);
      if (gameState.status === GameStatus.PLAYING) {
        playSound(SOUND.start);
        setPhase('game');
      }
    };

    socket.on(ServerEvents.PLAYER_JOINED, onPlayerJoined);
    socket.on(ServerEvents.ROOM_READY, onRoomReady);
    socket.on(ServerEvents.GAME_STATE_UPDATED, onGameState);
    return () => {
      socket.off(ServerEvents.PLAYER_JOINED, onPlayerJoined);
      socket.off(ServerEvents.ROOM_READY, onRoomReady);
      socket.off(ServerEvents.GAME_STATE_UPDATED, onGameState);
    };
  }, [setGameState, setPhase, setLobbyPlayers]);

  const handleSelectHero = (heroClass: string) => {
    if (!session) return;
    setSelectedHeroId(heroClass);
    playSound(SOUND.heroSelect);
    socketService.selectDeck(heroClass, session.roomCode);
  };

  return (
    <div className="lobby-screen" style={{ backgroundImage: `url(${resolveAsset(UI_IMAGE.shopBackground)})` }}>
      <h2>
        Phòng: <span className="room-code">{session?.roomCode}</span>
      </h2>
      <p>
        Người chơi: {lobbyPlayers.length > 0 ? lobbyPlayers.map((p) => p.name).join(' vs ') : 'Đang đợi đối thủ...'}
      </p>
      {!roomReady && <img className="lobby-wait" src={resolveAsset(UI_IMAGE.wait)} alt="waiting" />}
      <h3>Chọn hero của bạn</h3>
      <div className="hero-select-row">
        {listHeroAssets().map((hero) => (
          <button
            key={hero.heroClass}
            type="button"
            className={selectedHeroId === hero.heroClass ? 'hero-card hero-card-selected' : 'hero-card'}
            onClick={() => handleSelectHero(hero.heroClass)}
          >
            <img src={resolveAsset(hero.portrait)} alt={getHeroAsset(hero.heroClass).name} draggable={false} />
            <span>{getHeroAsset(hero.heroClass).name}</span>
          </button>
        ))}
      </div>
      {roomReady && !selectedHeroId && <p>Hãy chọn hero để sẵn sàng!</p>}
      {roomReady && selectedHeroId && <p>Đã sẵn sàng — đợi đối thủ chọn hero...</p>}
    </div>
  );
}
