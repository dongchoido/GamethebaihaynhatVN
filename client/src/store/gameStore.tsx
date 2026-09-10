import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import type { GameState } from '@coincard/shared';

export type ScreenPhase = 'home' | 'lobby' | 'game' | 'over';

interface SessionInfo {
  roomCode: string;
  playerId: string;
  sessionToken: string;
  playerName: string;
}

interface GameStoreValue {
  phase: ScreenPhase;
  setPhase: (phase: ScreenPhase) => void;
  session: SessionInfo | null;
  setSession: (session: SessionInfo | null) => void;
  gameState: GameState | null;
  setGameState: (state: GameState | null) => void;
  lastError: string | null;
  setLastError: (message: string | null) => void;
  selectedHeroId: string | null;
  setSelectedHeroId: (heroId: string | null) => void;
  lobbyPlayers: Array<{ playerId: string; name: string }>;
  setLobbyPlayers: (players: Array<{ playerId: string; name: string }>) => void;
  reset: () => void;
}

const GameStoreContext = createContext<GameStoreValue | null>(null);

const SESSION_KEY = 'coincard-session';

export function loadStoredSession(): SessionInfo | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    return raw ? (JSON.parse(raw) as SessionInfo) : null;
  } catch {
    return null;
  }
}

export function GameStoreProvider({ children }: { children: ReactNode }) {
  const [phase, setPhase] = useState<ScreenPhase>('home');
  const [session, setSessionState] = useState<SessionInfo | null>(() => loadStoredSession());
  const [gameState, setGameState] = useState<GameState | null>(null);
  const [lastError, setLastError] = useState<string | null>(null);
  const [selectedHeroId, setSelectedHeroId] = useState<string | null>(null);
  const [lobbyPlayers, setLobbyPlayers] = useState<Array<{ playerId: string; name: string }>>([]);

  const setSession = useCallback((next: SessionInfo | null) => {
    setSessionState(next);
    try {
      if (next) {
        sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
      } else {
        sessionStorage.removeItem(SESSION_KEY);
      }
    } catch {
      // Storage fail không chặn game.
    }
  }, []);

  const reset = useCallback(() => {
    setPhase('home');
    setSession(null);
    setGameState(null);
    setLastError(null);
    setSelectedHeroId(null);
    setLobbyPlayers([]);
  }, [setSession]);

  const value = useMemo<GameStoreValue>(
    () => ({
      phase,
      setPhase,
      session,
      setSession,
      gameState,
      setGameState,
      lastError,
      setLastError,
      selectedHeroId,
      setSelectedHeroId,
      lobbyPlayers,
      setLobbyPlayers,
      reset,
    }),
    [phase, session, setSession, gameState, lastError, selectedHeroId, lobbyPlayers, reset],
  );

  return <GameStoreContext.Provider value={value}>{children}</GameStoreContext.Provider>;
}

export function useGameStore(): GameStoreValue {
  const store = useContext(GameStoreContext);
  if (!store) {
    throw new Error('useGameStore phải dùng trong GameStoreProvider.');
  }
  return store;
}
