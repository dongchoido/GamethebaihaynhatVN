import { GameStoreProvider, useGameStore } from './store/gameStore';
import { HomeScreen } from './screens/HomeScreen';
import { LobbyScreen } from './screens/LobbyScreen';
import { GameScreen } from './screens/GameScreen';
import { GameOverScreen } from './screens/GameOverScreen';
import './styles/game.css';

function PhaseRouter() {
  const { phase } = useGameStore();
  switch (phase) {
    case 'lobby':
      return <LobbyScreen />;
    case 'game':
      return <GameScreen />;
    case 'over':
      return <GameOverScreen />;
    case 'home':
    default:
      return <HomeScreen />;
  }
}

export function App() {
  return (
    <GameStoreProvider>
      <PhaseRouter />
    </GameStoreProvider>
  );
}
