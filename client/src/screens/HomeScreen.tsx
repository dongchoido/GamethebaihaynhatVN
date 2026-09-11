import { useEffect, useRef, useState } from 'react';
import {
  GameStatus,
  ServerEvents,
  type GameStatePayload,
  type PlayerJoinedResponse,
  type RoomCreatedResponse,
} from '@coincard/shared';
import { socketService } from '../socket/socketService';
import { loadStoredSession, useGameStore } from '../store/gameStore';
import { playSound, resolveAsset, SOUND, UI_IMAGE } from '../assets/assetRegistry';

// Màn hình chủ: tạo phòng / tham gia phòng bằng room code.
export function HomeScreen() {
  const { setPhase, setSession, setGameState, setLobbyPlayers, setSelectedHeroId, setLastError, lastError, reset } = useGameStore();
  const [playerName, setPlayerName] = useState('');
  const [roomCode, setRoomCode] = useState('');
  // Handlers cần tên mới nhất nhưng effect chỉ đăng ký 1 lần → dùng ref.
  const playerNameRef = useRef(playerName);
  playerNameRef.current = playerName;

  useEffect(() => {
    const stored = loadStoredSession();
    const socket = socketService.connect(stored?.sessionToken);

    const onRoomCreated = (res: RoomCreatedResponse) => {
      const name = playerNameRef.current.trim();
      setSession({ roomCode: res.roomCode, playerId: res.playerId, sessionToken: res.sessionToken, playerName: name });
      setLobbyPlayers(res.players);
      socketService.setSessionToken(res.sessionToken);
      setPhase('lobby');
    };
    const onPlayerJoined = (res: PlayerJoinedResponse) => {
      // Broadcast cho người trong phòng không kèm token.
      if (!res.sessionToken || !res.playerId) {
        setLobbyPlayers(res.players);
        // Refresh ở lobby: server gửi lại danh sách → quay lại lobby.
        const storedAgain = loadStoredSession();
        if (storedAgain && storedAgain.roomCode === res.roomCode) {
          const me = res.players.find(p => p.playerId === storedAgain.playerId);
          setSelectedHeroId(me?.ready ? me.heroClass ?? null : null);
          setSession(storedAgain);
          setPhase('lobby');
        }
        return;
      }
      const name = playerNameRef.current.trim();
      setSession({ roomCode: res.roomCode, playerId: res.playerId, sessionToken: res.sessionToken, playerName: name });
      setLobbyPlayers(res.players);
      socketService.setSessionToken(res.sessionToken);
      setPhase('lobby');
    };
    const onRejected = (res: { code: string; message: string }) => {
      // Session chết (phòng mất sau restart) → xóa để về home sạch, tránh kẹt.
      if (res.code === 'RECONNECT_FAILED') {
        reset();
        setLastError('Phòng cũ không còn. Hãy tạo phòng mới.');
        return;
      }
      setLastError(`${res.code}: ${res.message}`);
    };
    // Refresh giữa trận → server gửi lại state → nhảy thẳng vào game/over.
    const onGameState = ({ gameState }: GameStatePayload) => {
      setGameState(gameState);
      if (gameState.status === GameStatus.PLAYING) {
        setPhase('game');
      } else if (gameState.status === GameStatus.FINISHED) {
        setPhase('over');
      }
    };

    socket.on(ServerEvents.ROOM_CREATED, onRoomCreated);
    socket.on(ServerEvents.PLAYER_JOINED, onPlayerJoined);
    socket.on(ServerEvents.ACTION_REJECTED, onRejected);
    socket.on(ServerEvents.GAME_STATE_UPDATED, onGameState);

    // Reconnect tự động nếu refresh giữa trận (service tự RECONNECT sau connect).
    if (stored) {
      socketService.setSessionToken(stored.sessionToken);
      setSession(stored);
    }

    return () => {
      socket.off(ServerEvents.ROOM_CREATED, onRoomCreated);
      socket.off(ServerEvents.PLAYER_JOINED, onPlayerJoined);
      socket.off(ServerEvents.ACTION_REJECTED, onRejected);
      socket.off(ServerEvents.GAME_STATE_UPDATED, onGameState);
    };
  }, [setPhase, setSession, setGameState, setLobbyPlayers, setSelectedHeroId, setLastError, reset]);

  const handleCreate = () => {
    if (!playerName.trim()) {
      setLastError('Nhập tên trước khi tạo phòng.');
      return;
    }
    setLastError(null);
    playSound(SOUND.start);
    socketService.createRoom(playerName.trim());
  };

  const handleJoin = () => {
    if (!playerName.trim() || !roomCode.trim()) {
      setLastError('Nhập tên và mã phòng.');
      return;
    }
    setLastError(null);
    playSound(SOUND.start);
    socketService.joinRoom({ roomCode: roomCode.trim().toUpperCase(), playerName: playerName.trim() });
  };

  return (
    <div className="home-screen" style={{ backgroundImage: `url(${resolveAsset(UI_IMAGE.startBackground)})` }}>
      <div className="home-panel">
        <span className="home-corner tl" aria-hidden="true" />
        <span className="home-corner tr" aria-hidden="true" />
        <span className="home-corner bl" aria-hidden="true" />
        <span className="home-corner br" aria-hidden="true" />
        <span className="home-gem top" aria-hidden="true" />
        <p className="home-tagline">Card battle 2 người chơi</p>
        <div className="home-form">
          <label className="home-field">
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
              <circle cx="12" cy="8" r="4" fill="currentColor" />
              <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7v1H4v-1z" fill="currentColor" />
            </svg>
            <input
              className="text-input"
              placeholder="Tên của bạn"
              value={playerName}
              onChange={(e) => setPlayerName(e.target.value)}
              maxLength={24}
            />
          </label>
          <button type="button" className="btn-primary home-create" onClick={handleCreate}>
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
              <path d="M4 4l7 7-2.5 2.5L4 9V4zm16 0v5l-4.5 4.5L13 11l7-7zM4 4h2v2H4z" fill="currentColor" />
              <path d="M11 13l-4 7 2 1 4-6-2-2zm2 0l4 7-2 1-4-6 2-2z" fill="currentColor" />
            </svg>
            Tạo phòng
          </button>
          <div className="join-row">
            <label className="home-field">
              <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                <circle cx="8" cy="12" r="4" fill="none" stroke="currentColor" strokeWidth="2" />
                <path d="M11 12h9v2h-2v2h-2v-2h-2v2h-2v-2z" fill="currentColor" />
              </svg>
              <input
                className="text-input"
                placeholder="Mã phòng (vd A8F3K2)"
                value={roomCode}
                onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
                maxLength={6}
              />
            </label>
            <button type="button" className="btn-secondary home-join" onClick={handleJoin}>
              <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                <circle cx="9" cy="8" r="3.2" fill="currentColor" />
                <path d="M3.5 19c0-3.4 2.5-5.5 5.5-5.5s5.5 2.1 5.5 5.5v1h-11v-1z" fill="currentColor" />
                <circle cx="16.5" cy="9" r="2.6" fill="currentColor" opacity="0.75" />
                <path d="M15.5 13.7c2.9 0.2 5 2.2 5 5.3v1h-4v-1c0-2.3-0.3-3.9-1-5.3z" fill="currentColor" opacity="0.75" />
              </svg>
              Tham gia
            </button>
          </div>
          {lastError && <p className="error-text">{lastError}</p>}
        </div>
        <span className="home-gem bottom" aria-hidden="true" />
      </div>
    </div>
  );
}
