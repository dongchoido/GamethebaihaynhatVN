import { useEffect, useMemo, useState } from 'react';
import {
  CardType,
  HeroClass,
  Rarity,
  ServerEvents,
  type ActionRejectedResponse,
  type CardDefinition,
  type DeckRules,
  type GameCatalogResponse,
  type LoadoutAcceptedResponse,
} from '@coincard/shared';
import { getHeroAsset, resolveAsset } from '../assets/assetRegistry';
import { socketService } from '../socket/socketService';
import { useConnection } from '../socket/useConnection';
import { useGameStore } from '../store/gameStore';

const STORAGE_KEY = 'coincard-decks-v2';
const LEGACY_STORAGE_KEY = 'coincard-decks-v1';

type ClassFilter = 'ALL' | 'HERO' | 'NEUTRAL';
type ManaFilter = 'ALL' | number;
type TypeFilter = 'ALL' | CardType;
type StoredDecks = Partial<Record<HeroClass, string[]>>;

interface StoredDeckDocument {
  version: 2;
  decks: StoredDecks;
}

function copyLimit(card: CardDefinition, rules: DeckRules): number {
  return card.rarity === Rarity.LEGENDARY ? rules.maxLegendaryCopies : rules.maxCopies;
}

function defaultRules(): DeckRules {
  return {
    deckSize: 30,
    maxCopies: 2,
    maxLegendaryCopies: 1,
    heroClasses: [HeroClass.MAGE, HeroClass.HUNTER, HeroClass.PALADIN, HeroClass.PRIEST, HeroClass.WARLOCK],
  };
}

export function sanitizeDeck(
  slugs: readonly string[],
  heroClass: HeroClass,
  cards: readonly CardDefinition[],
  rules: DeckRules,
): string[] {
  const bySlug = new Map(cards.map((card) => [card.slug, card]));
  const copies = new Map<string, number>();
  const sanitized: string[] = [];

  for (const slug of slugs) {
    if (sanitized.length >= rules.deckSize) break;
    const card = bySlug.get(slug);
    if (!card || !card.collectible || (card.heroClass !== HeroClass.NEUTRAL && card.heroClass !== heroClass)) continue;
    const used = copies.get(slug) ?? 0;
    if (used >= copyLimit(card, rules)) continue;
    copies.set(slug, used + 1);
    sanitized.push(slug);
  }
  return sanitized;
}

export function deckCounts(slugs: readonly string[]): Record<string, number> {
  return slugs.reduce<Record<string, number>>((counts, slug) => {
    counts[slug] = (counts[slug] ?? 0) + 1;
    return counts;
  }, {});
}

export function deckSlugs(counts: Readonly<Record<string, number>>): string[] {
  return Object.entries(counts).flatMap(([slug, count]) => Array.from({ length: count }, () => slug));
}

function parseStoredDocument(raw: string | null): StoredDeckDocument | null {
  if (!raw) return null;
  try {
    const parsed: unknown = JSON.parse(raw);
    if (
      typeof parsed === 'object'
      && parsed !== null
      && 'version' in parsed
      && 'decks' in parsed
      && (parsed as { version?: unknown }).version === 2
      && typeof (parsed as { decks?: unknown }).decks === 'object'
      && (parsed as { decks?: unknown }).decks !== null
    ) {
      return parsed as StoredDeckDocument;
    }
  } catch {
    // A corrupt browser cache should never prevent a new deck from being built.
  }
  return null;
}

export function migrateDeckStorage(cards: readonly CardDefinition[], rules: DeckRules): StoredDecks {
  try {
    const current = parseStoredDocument(localStorage.getItem(STORAGE_KEY));
    let source: StoredDecks = current?.decks ?? {};

    if (!current) {
      const legacyRaw = localStorage.getItem(LEGACY_STORAGE_KEY);
      if (legacyRaw) {
        try {
          const legacy: unknown = JSON.parse(legacyRaw);
          if (typeof legacy === 'object' && legacy !== null) source = legacy as StoredDecks;
        } catch {
          source = {};
        }
      }
    }

    const decks: StoredDecks = {};
    for (const heroClass of rules.heroClasses) {
      const slugs = source[heroClass];
      if (Array.isArray(slugs)) decks[heroClass] = sanitizeDeck(slugs, heroClass, cards, rules);
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ version: 2, decks } satisfies StoredDeckDocument));
    return decks;
  } catch {
    return {};
  }
}

function persistDeck(heroClass: HeroClass, slugs: readonly string[]): void {
  try {
    const current = parseStoredDocument(localStorage.getItem(STORAGE_KEY));
    const decks = { ...(current?.decks ?? {}), [heroClass]: [...slugs] };
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ version: 2, decks } satisfies StoredDeckDocument));
  } catch {
    // Private browsing or a full quota must not block the websocket command.
  }
}

function friendlyClass(value: HeroClass): string {
  return value.charAt(0) + value.slice(1).toLowerCase();
}

// The deck only leaves this screen after the authoritative LOADOUT_ACCEPTED event.
export function DeckBuilderScreen() {
  const connected = useConnection();
  const {
    session,
    selectedHeroId,
    setPhase,
    setLastError,
    lastError,
    setLobbyPlayers,
  } = useGameStore();
  const heroClass = selectedHeroId ?? HeroClass.MAGE;
  const [catalog, setCatalog] = useState<GameCatalogResponse | null>(null);
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [query, setQuery] = useState('');
  const [classFilter, setClassFilter] = useState<ClassFilter>('ALL');
  const [manaFilter, setManaFilter] = useState<ManaFilter>('ALL');
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL');
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);

  const rules = catalog?.deckRules ?? defaultRules();
  const selectedSlugs = useMemo(() => deckSlugs(counts), [counts]);
  const totalCards = selectedSlugs.length;

  useEffect(() => {
    let active = true;
    setLoading(true);
    fetch('/api/game-catalog')
      .then(async (response) => {
        if (!response.ok) throw new Error('Không thể tải danh mục bài.');
        return response.json() as Promise<GameCatalogResponse>;
      })
      .then((nextCatalog) => {
        if (!active) return;
        setCatalog(nextCatalog);
        const saved = migrateDeckStorage(nextCatalog.collectibleCards, nextCatalog.deckRules)[heroClass];
        const suggested = sanitizeDeck(
          nextCatalog.suggestedDecks[heroClass] ?? [],
          heroClass,
          nextCatalog.collectibleCards,
          nextCatalog.deckRules,
        );
        setCounts(deckCounts(saved && saved.length > 0 ? saved : suggested));
        setLastError(null);
      })
      .catch((error: unknown) => {
        if (active) setLastError(error instanceof Error ? error.message : 'Không thể tải danh mục bài.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [heroClass, setLastError]);

  useEffect(() => {
    let socket;
    try {
      socket = socketService.getSocket();
    } catch {
      return undefined;
    }

    const onAccepted = (response: LoadoutAcceptedResponse) => {
      setLobbyPlayers(response.players);
      const mine = response.players.find((player) => player.playerId === session?.playerId);
      if (pending && mine?.deckReady) {
        persistDeck(heroClass, selectedSlugs);
        setPending(false);
        setLastError(null);
        setPhase('lobby');
      }
    };
    const onRejected = (response: ActionRejectedResponse) => {
      if (!pending) return;
      setPending(false);
      setLastError(`${response.code}: ${response.message}`);
    };

    socket.on(ServerEvents.LOADOUT_ACCEPTED, onAccepted);
    socket.on(ServerEvents.ACTION_REJECTED, onRejected);
    return () => {
      socket.off(ServerEvents.LOADOUT_ACCEPTED, onAccepted);
      socket.off(ServerEvents.ACTION_REJECTED, onRejected);
    };
  }, [heroClass, pending, selectedSlugs, session?.playerId, setLastError, setLobbyPlayers, setPhase]);

  const visibleCards = useMemo(() => {
    if (!catalog) return [];
    const normalizedQuery = query.trim().toLowerCase();
    return catalog.collectibleCards.filter((card) => {
      const classMatches = (classFilter === 'ALL'
        && (card.heroClass === heroClass || card.heroClass === HeroClass.NEUTRAL))
        || (classFilter === 'HERO' && card.heroClass === heroClass)
        || (classFilter === 'NEUTRAL' && card.heroClass === HeroClass.NEUTRAL);
      const manaMatches = manaFilter === 'ALL' || card.manaCost === manaFilter;
      const typeMatches = typeFilter === 'ALL' || card.type === typeFilter;
      const nameMatches = !normalizedQuery || card.name.toLowerCase().includes(normalizedQuery);
      return card.collectible && classMatches && manaMatches && typeMatches && nameMatches;
    });
  }, [catalog, classFilter, heroClass, manaFilter, query, typeFilter]);

  const changeCard = (card: CardDefinition, amount: 1 | -1) => {
    if (pending) return;
    setCounts((current) => {
      const currentCount = current[card.slug] ?? 0;
      if (amount > 0 && (totalCards >= rules.deckSize || currentCount >= copyLimit(card, rules))) return current;
      if (amount < 0 && currentCount === 0) return current;
      const nextCount = currentCount + amount;
      const next = { ...current };
      if (nextCount === 0) delete next[card.slug];
      else next[card.slug] = nextCount;
      return next;
    });
  };

  const autoBuild = () => {
    if (!catalog || pending) return;
    const suggested = sanitizeDeck(
      catalog.suggestedDecks[heroClass] ?? [],
      heroClass,
      catalog.collectibleCards,
      rules,
    );
    setCounts(deckCounts(suggested));
    setLastError(null);
  };

  const submit = () => {
    if (!session || !connected || totalCards !== rules.deckSize || pending) return;
    setPending(true);
    setLastError(null);
    socketService.submitLoadout(heroClass, session.roomCode, selectedSlugs);
  };

  return (
    <main className="deck-builder-screen">
      <header className="deck-builder-header">
        <div>
          <p>{getHeroAsset(heroClass).name} - {friendlyClass(heroClass)}</p>
          <h1>Xây dựng deck</h1>
          <p>{totalCards}/{rules.deckSize} lá</p>
        </div>
        <div className="deck-builder-actions">
          <button type="button" className="secondary-button" onClick={() => setPhase('lobby')} disabled={pending}>Quay lại</button>
          <button type="button" className="secondary-button" onClick={autoBuild} disabled={loading || pending}>Tự tạo deck</button>
          <button type="button" className="primary-button" onClick={submit} disabled={loading || pending || !connected || totalCards !== rules.deckSize}>
            {pending ? 'Đang xác nhận...' : 'Sẵn sàng'}
          </button>
        </div>
      </header>

      {lastError && <p className="error-text" role="alert">{lastError}</p>}
      {!loading && totalCards !== rules.deckSize && (
        <p role="status">Deck cần đủ {rules.deckSize} lá trước khi sẵn sàng.</p>
      )}
      {!connected && <p role="status">Đang kết nối lại với server...</p>}

      <section className="deck-builder-toolbar" aria-label="Bộ lọc bài">
        <input
          className="text-input"
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Tìm theo tên bài"
          aria-label="Tìm theo tên bài"
        />
        <div className="deck-segmented" role="group" aria-label="Lọc theo lớp bài">
          {(['ALL', 'HERO', 'NEUTRAL'] as const).map((filter) => (
            <button
              key={filter}
              type="button"
              className={classFilter === filter ? 'selected' : ''}
              onClick={() => setClassFilter(filter)}
              aria-pressed={classFilter === filter}
            >
              {filter === 'ALL' ? 'Tất cả' : filter === 'HERO' ? 'Hero' : 'Neutral'}
            </button>
          ))}
        </div>
        <select
          className="deck-builder-select"
          value={manaFilter}
          onChange={(event) => setManaFilter(event.target.value === 'ALL' ? 'ALL' : Number(event.target.value))}
          aria-label="Lọc theo mana"
        >
          <option value="ALL">Mọi mana</option>
          {Array.from({ length: 11 }, (_, mana) => <option key={mana} value={mana}>{mana} mana</option>)}
        </select>
        <select
          className="deck-builder-select"
          value={typeFilter}
          onChange={(event) => setTypeFilter(event.target.value as TypeFilter)}
          aria-label="Lọc theo loại bài"
        >
          <option value="ALL">Mọi loại</option>
          <option value={CardType.MINION}>Minion</option>
          <option value={CardType.SPELL}>Spell</option>
        </select>
      </section>

      {loading ? (
        <p role="status">Đang tải bộ bài...</p>
      ) : (
        <section className="deck-card-grid" aria-label="Danh mục bài có thể chọn">
          {visibleCards.map((card) => {
            const count = counts[card.slug] ?? 0;
            const limit = copyLimit(card, rules);
            return (
              <article className={count > 0 ? 'deck-card selected' : 'deck-card'} key={card.slug}>
                <img src={resolveAsset(card.imagePath)} alt="" />
                <div className="deck-card-info">
                  <strong title={card.name}>{card.name}</strong>
                  <span>{card.manaCost} mana - {card.type} - {card.rarity}</span>
                  <div className="deck-card-controls">
                    <button type="button" onClick={() => changeCard(card, -1)} disabled={count === 0 || pending} aria-label={`Bỏ ${card.name}`}>-</button>
                    <span aria-label={`${card.name}: ${count}/${limit}`}>{count}/{limit}</span>
                    <button type="button" onClick={() => changeCard(card, 1)} disabled={count >= limit || totalCards >= rules.deckSize || pending} aria-label={`Thêm ${card.name}`}>+</button>
                  </div>
                </div>
              </article>
            );
          })}
        </section>
      )}
      {!loading && visibleCards.length === 0 && <p>Không có bài phù hợp với bộ lọc.</p>}
    </main>
  );
}
