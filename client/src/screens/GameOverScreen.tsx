import { GameStatus, ServerEvents, type GameStatePayload } from '@coincard/shared';
import { useEffect, useState } from 'react';
import { socketService } from '../socket/socketService';
import { useConnection } from '../socket/useConnection';
import { useGameStore } from '../store/gameStore';
import { getHeroAsset, resolveAsset } from '../assets/assetRegistry';

// Màn hình Victory / Defeat — splash theo hero class của mình.
export function GameOverScreen() {
  const connected = useConnection();
  const { session, gameState, setGameState, setPhase, reset } = useGameStore();
  const [rematchWaiting, setRematchWaiting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const socket = socketService.getSocket();
    const onGameState = ({ gameState: next }: GameStatePayload) => {
      setGameState(next);
      if (next.status === GameStatus.PLAYING) { setRematchWaiting(false); setPhase('game'); }
    };
    const onRejected = (res: { code: string; message: string }) => {
      setRematchWaiting(false); setError(res.message);
      if (res.code === 'RECONNECT_FAILED') { socketService.disconnect(); reset(); }
    };
    socket.on(ServerEvents.GAME_STATE_UPDATED, onGameState);
    socket.on(ServerEvents.ACTION_REJECTED, onRejected);
    return () => {
      socket.off(ServerEvents.GAME_STATE_UPDATED, onGameState);
      socket.off(ServerEvents.ACTION_REJECTED, onRejected);
    };
  }, [setGameState, setPhase, reset]);

  const myPlayer = gameState?.players.find((p) => p.playerId === session?.playerId) ?? null;
  const isWinner = !!gameState && gameState.winnerId === session?.playerId;
  const isDraw = gameState?.status === GameStatus.FINISHED && gameState.winnerId === null;
  const heroClass = myPlayer?.hero.heroClass ?? 'MAGE';
  const splash = isWinner
    ? getHeroAsset(heroClass).winSplash
    : getHeroAsset(heroClass).defeatSplash;
  const winner = gameState?.players.find((p) => p.playerId === gameState?.winnerId) ?? null;
  const winnerHero = winner?.hero ?? myPlayer?.hero ?? null;
  const winnerAsset = getHeroAsset(winnerHero?.heroClass ?? 'MAGE');
  const turns = gameState?.turn ?? 0;
  const damage = (winner ?? myPlayer)?.damageDealt ?? 0;
  const cardsPlayed = (winner ?? myPlayer)?.cardsPlayed ?? 0;
  const summons = (winner ?? myPlayer)?.minionsSummoned ?? 0;

  const awards = [
    {
      key: 'tactician',
      unlocked: damage >= 20,
      title: 'Bậc Thầy Chiến Thuật',
      desc: 'Làm chủ thế trận',
      icon: (
        <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
          <path d="M5 3l7 7-2.3 2.3L5 8V3zm14 0v5l-4.7 4.7L12 10.4 19 3zM12 12.6L8.5 20l1.8.9 3.2-5.6-1.5-2.7zm0 0l3.5 7.4-1.8.9-3.2-5.6 1.5-2.7z" fill="currentColor" />
        </svg>
      ),
    },
    {
      key: 'summoner',
      unlocked: summons >= 4,
      title: 'Bậc Thầy Triệu Hồi',
      desc: 'Khai mở sức mạnh',
      icon: (
        <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
          <rect x="4" y="6" width="9" height="13" rx="1.5" fill="none" stroke="currentColor" strokeWidth="1.8" transform="rotate(-8 8 12)" />
          <rect x="11" y="5" width="9" height="13" rx="1.5" fill="none" stroke="currentColor" strokeWidth="1.8" transform="rotate(8 15 11)" />
        </svg>
      ),
    },
    {
      key: 'unstoppable',
      unlocked: !isDraw && turns > 0 && turns <= 12,
      title: 'Không Thể Ngăn Cản',
      desc: 'Chiến thắng áp đảo',
      icon: (
        <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
          <path d="M12 2l2.6 6.2 6.7.5-5.1 4.4 1.6 6.5L12 15.9 6.2 19.6l1.6-6.5-5.1-4.4 6.7-.5z" fill="currentColor" />
        </svg>
      ),
    },
  ];

  const statIcon = {
    damage: (
      <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
        <path d="M4 4l8 8-2.5 2.5L4 9V4zm16 0v5l-5.5 5.5L12 12l8-8zM5 15l-2 6 2 1 3.5-5.5L5 15zm14 0l2 6-2 1-3.5-5.5L19 15z" fill="currentColor" />
      </svg>
    ),
    cards: (
      <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
        <rect x="5" y="4" width="9" height="13" rx="1.5" fill="none" stroke="currentColor" strokeWidth="1.8" transform="rotate(-8 9 10)" />
        <rect x="11" y="6" width="9" height="13" rx="1.5" fill="none" stroke="currentColor" strokeWidth="1.8" transform="rotate(8 15 12)" />
      </svg>
    ),
    turns: (
      <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
        <path d="M6 3h12v2l-4 6 4 6v2H6v-2l4-6-4-6V3z" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
        <path d="M6 3h12M6 21h12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      </svg>
    ),
    mvp: (
      <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
        <path d="M4 8l3 9h9l3-9-3-1-2 3h-5L7 7 4 8zm3 11h10v2H7v-2z" fill="currentColor" />
        <circle cx="12" cy="5" r="2" fill="currentColor" />
      </svg>
    ),
  };

  return (
    <div className="gameover-screen" style={{ backgroundImage: `url(${resolveAsset(splash)})` }}>
      <div className="honor-title">
        <h1 className={isWinner || isDraw ? 'honor-victory' : 'honor-defeat'}>{isDraw ? 'HÒA' : isWinner ? 'VINH DANH' : 'THẤT BẠI'}</h1>
        <div className="honor-sub">{isDraw ? 'Không có người thắng — thống kê của bạn' : 'Người chiến thắng'}</div>
        {!connected && <p role="status">Đang kết nối lại với server...</p>}
        {error && <p className="error-text" role="alert">{error}</p>}
      </div>
      <div className="honor-body">
        <section className="honor-panel" aria-label="Thống kê trận đấu">
          <h2>THỐNG KÊ TRẬN ĐẤU</h2>
          <ul>
            <li><span className="honor-ico">{statIcon.damage}</span><span>Sát thương gây ra</span><strong>{damage.toLocaleString('vi-VN')}</strong></li>
            <li><span className="honor-ico">{statIcon.cards}</span><span>Lá bài đã chơi</span><strong>{cardsPlayed}</strong></li>
            <li><span className="honor-ico">{statIcon.turns}</span><span>Số lượt</span><strong>{turns}</strong></li>
            <li><span className="honor-ico">{statIcon.mvp}</span><span>MVP</span><strong>{isWinner ? 'Có' : 'Không'}</strong></li>
          </ul>
          <p className="honor-foot">MỘT TRẬN ĐẤU VĨ ĐẠI<br />TẠO NÊN HUYỀN THOẠI</p>
        </section>
        <section className="honor-hero" aria-label="Hero chiến thắng">
          <span className="honor-hero-gem" aria-hidden="true" />
          <span className="honor-portrait">
            <img src={resolveAsset(winnerAsset.portrait)} alt={winnerHero?.name ?? ''} draggable={false} />
          </span>
          <div className="honor-nameplate">
            <strong>{winnerHero?.name ?? ''}</strong>
            <span>{winnerHero?.powerName ?? ''}</span>
          </div>
        </section>
        <section className="honor-panel" aria-label="Thành tựu đặc biệt">
          <h2>THÀNH TỰU ĐẶC BIỆT</h2>
          <ul>
            {awards.map((a) => (
              <li key={a.key} className={a.unlocked ? 'unlocked' : 'locked'}>
                <span className="honor-badge">{a.icon}</span>
                <span className="honor-award-text"><strong>{a.title}</strong><span>{a.unlocked ? a.desc : 'Chưa mở khóa'}</span></span>
              </li>
            ))}
          </ul>
          <p className="honor-foot">VINH QUANG THUỘC VỀ<br />NHỮNG KẺ KIÊN ĐỊNH</p>
        </section>
      </div>
      <div className="honor-actions">
        <button
          type="button"
          className="honor-btn blue"
          disabled={rematchWaiting || !connected}
          onClick={() => {
            if (gameState) {
              setRematchWaiting(true);
              socketService.rematch(gameState.gameId);
            }
          }}
        >
          <span>{rematchWaiting ? 'Đang đợi đối thủ...' : 'Chơi lại'}</span>
        </button>
        <button
          type="button"
          className="honor-btn red"
          onClick={() => {
            socketService.disconnect();
            reset();
          }}
        >
          <span>Sảnh chính</span>
        </button>
      </div>
    </div>
  );
}
