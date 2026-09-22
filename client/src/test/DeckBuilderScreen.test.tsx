import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  CardType,
  HeroClass,
  Rarity,
  ServerEvents,
  type CardDefinition,
  type DeckRules,
  type GameCatalogResponse,
} from '@coincard/shared';
import {
  DeckBuilderScreen,
  migrateDeckStorage,
  sanitizeDeck,
} from '../screens/DeckBuilderScreen';
import { GameStoreProvider, useGameStore } from '../store/gameStore';

const socketMock = vi.hoisted(() => {
  const listeners = new Map<string, (payload: unknown) => void>();
  return {
    listeners,
    submitLoadout: vi.fn(),
  };
});

vi.mock('../socket/useConnection', () => ({ useConnection: () => true }));
vi.mock('../socket/socketService', () => ({
  socketService: {
    getSocket: () => ({
      on: (event: string, handler: (payload: unknown) => void) => socketMock.listeners.set(event, handler),
      off: (event: string, handler: (payload: unknown) => void) => {
        if (socketMock.listeners.get(event) === handler) socketMock.listeners.delete(event);
      },
    }),
    submitLoadout: socketMock.submitLoadout,
  },
}));

const rules: DeckRules = {
  deckSize: 2,
  maxCopies: 2,
  maxLegendaryCopies: 1,
  heroClasses: [HeroClass.MAGE, HeroClass.HUNTER],
};

function card(slug: string, heroClass: HeroClass, manaCost: number, type = CardType.MINION,
  rarity = Rarity.COMMON, collectible = true): CardDefinition {
  return {
    id: slug,
    name: slug.replace(/-/g, ' '),
    slug,
    description: '',
    type,
    rarity,
    manaCost,
    attack: type === CardType.MINION ? 1 : 0,
    health: type === CardType.MINION ? 1 : 0,
    heroClass,
    imagePath: 'assets/images/design/NoCardView.png',
    effects: [],
    keywords: [],
    collectible,
  };
}

function catalog(): GameCatalogResponse {
  return {
    heroes: [],
    collectibleCards: [
      card('mage-bolt', HeroClass.MAGE, 2, CardType.SPELL),
      card('neutral-guard', HeroClass.NEUTRAL, 1),
      card('hunter-only', HeroClass.HUNTER, 3),
      card('token-card', HeroClass.NEUTRAL, 0, CardType.SPELL, Rarity.COMMON, false),
    ],
    deckRules: rules,
    suggestedDecks: { [HeroClass.MAGE]: ['mage-bolt', 'neutral-guard'] },
  };
}

function ScreenWithState() {
  const { setSession, setSelectedHeroId, setPhase, phase } = useGameStore();
  return (
    <>
      <button
        type="button"
        onClick={() => {
          setSession({ roomCode: 'ABC123', playerId: 'p1', sessionToken: 'token-1' });
          setSelectedHeroId(HeroClass.MAGE);
          setPhase('deck');
        }}
      >
        Set up
      </button>
      <output data-testid="phase">{phase}</output>
      <DeckBuilderScreen />
    </>
  );
}

async function renderBuilder(nextCatalog = catalog()) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    json: async () => nextCatalog,
  }));
  render(<GameStoreProvider><ScreenWithState /></GameStoreProvider>);
  await userEvent.setup().click(screen.getByRole('button', { name: 'Set up' }));
  await screen.findByText('mage bolt');
}

describe('DeckBuilderScreen', () => {
  beforeEach(() => {
    localStorage.clear();
    socketMock.listeners.clear();
    socketMock.submitLoadout.mockReset();
    vi.unstubAllGlobals();
  });

  afterEach(() => {
    cleanup();
  });

  it('migrates v1 storage and removes stale, illegal, token, and excess cards', () => {
    localStorage.setItem('coincard-decks-v1', JSON.stringify({
      [HeroClass.MAGE]: ['mage-bolt', 'mage-bolt', 'mage-bolt', 'hunter-only', 'token-card', 'missing'],
    }));

    const migrated = migrateDeckStorage(catalog().collectibleCards, rules);

    expect(migrated[HeroClass.MAGE]).toEqual(['mage-bolt', 'mage-bolt']);
    expect(JSON.parse(localStorage.getItem('coincard-decks-v2') ?? '{}')).toEqual({
      version: 2,
      decks: { [HeroClass.MAGE]: ['mage-bolt', 'mage-bolt'] },
    });
  });

  it('enforces the legendary copy limit while sanitizing persisted decks', () => {
    const legendary = card('legendary', HeroClass.MAGE, 8, CardType.MINION, Rarity.LEGENDARY);

    expect(sanitizeDeck(['legendary', 'legendary'], HeroClass.MAGE, [legendary], rules))
      .toEqual(['legendary']);
  });

  it('filters playable cards by class, mana, and type', async () => {
    await renderBuilder();

    expect(screen.queryByText('hunter only')).not.toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Hero' }));
    expect(screen.getByText('mage bolt')).toBeInTheDocument();
    expect(screen.queryByText('neutral guard')).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Lọc theo mana'), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText('Lọc theo loại bài'), { target: { value: CardType.SPELL } });
    expect(screen.getByText('mage bolt')).toBeInTheDocument();
  });

  it('prevents selecting more copies than the deck rule allows', async () => {
    const noSuggestion = catalog();
    noSuggestion.suggestedDecks = {};
    await renderBuilder(noSuggestion);
    const addMageBolt = screen.getByRole('button', { name: 'Thêm mage bolt' });
    const user = userEvent.setup();

    await user.click(addMageBolt);
    await user.click(addMageBolt);

    expect(screen.getByLabelText('mage bolt: 2/2')).toBeInTheDocument();
    expect(addMageBolt).toBeDisabled();
  });

  it('waits for server acceptance, then keeps deck editable after rejection', async () => {
    await renderBuilder();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Tự tạo deck' }));
    const ready = screen.getByRole('button', { name: 'Sẵn sàng' });
    expect(ready).toBeEnabled();

    await user.click(ready);
    expect(socketMock.submitLoadout).toHaveBeenCalledWith(HeroClass.MAGE, 'ABC123', [
      'mage-bolt', 'neutral-guard',
    ]);
    expect(screen.getByRole('button', { name: 'Đang xác nhận...' })).toBeDisabled();
    expect(screen.getByTestId('phase')).toHaveTextContent('deck');

    act(() => socketMock.listeners.get(ServerEvents.ACTION_REJECTED)?.({
      code: 'INVALID_DECK', message: 'Deck không hợp lệ.',
    }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Sẵn sàng' })).toBeEnabled());
    expect(screen.getByRole('alert')).toHaveTextContent('INVALID_DECK');

    await user.click(screen.getByRole('button', { name: 'Sẵn sàng' }));
    act(() => socketMock.listeners.get(ServerEvents.LOADOUT_ACCEPTED)?.({
      roomCode: 'ABC123',
      players: [{
        playerId: 'p1', name: 'One', heroClass: HeroClass.MAGE,
        deckReady: true, ready: true, connected: true,
      }],
    }));
    await waitFor(() => expect(screen.getByTestId('phase')).toHaveTextContent('lobby'));
  });
});
