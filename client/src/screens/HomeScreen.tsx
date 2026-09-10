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
  const { setPhase, setSession, setGameState, setLobbyPlayers, setLastError, lastError, reset } = useGameStore();
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
  }, [setPhase, setSession, setGameState, setLobbyPlayers, setLastError, reset]);

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
      <h1 className="game-title">CoinCard</h1>
      <p className="game-subtitle">Card battle 2 người chơi</p>
      <div className="home-form">
        <input
          className="text-input"
          placeholder="Tên của bạn"
          value={playerName}
          onChange={(e) => setPlayerName(e.target.value)}
          maxLength={24}
        />
        <button type="button" className="btn-primary" onClick={handleCreate}>
          Tạo phòng
        </button>
        <div className="join-row">
          <input
            className="text-input"
            placeholder="Mã phòng (vd A8F3K2)"
            value={roomCode}
            onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
            maxLength={6}
          />
          <button type="button" className="btn-secondary" onClick={handleJoin}>
            Tham gia
          </button>
        </div>
        {lastError && <p className="error-text">{lastError}</p>}
      </div>
    </div>
  );
}
