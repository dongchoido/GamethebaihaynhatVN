import type { CardDefinition } from '@coincard/shared';
import { resolveAsset } from '../assets/assetRegistry';

interface CardViewProps {
  card: CardDefinition;
  playable: boolean;
  selected: boolean;
  onSelect: () => void;
}

// Lá bài: artwork + mana + tên + attack/health + mô tả. Hover zoom (CSS).
export function CardView({ card, playable, selected, onSelect }: CardViewProps) {
  const isMinion = card.type === 'MINION';
  const className = [
    'card-view',
    playable ? 'card-playable' : 'card-disabled',
    selected ? 'card-selected' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button type="button" className={className} onClick={onSelect} title={card.description}>
      <span className="card-mana">{card.manaCost}</span>
      <img className="card-art" src={resolveAsset(card.imagePath)} alt={card.name} draggable={false} />
      <span className="card-name">{card.name}</span>
      <span className="card-desc">{card.description}</span>
      {isMinion && (
        <>
          <span className="card-attack">{card.attack}</span>
          <span className="card-health">{card.health}</span>
        </>
      )}
    </button>
  );
}
