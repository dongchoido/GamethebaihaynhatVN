import type { MinionState } from '@coincard/shared';
import { resolveAsset } from '../assets/assetRegistry';

interface MinionViewProps {
  minion: MinionState;
  selectable: boolean;
  selected: boolean;
  targetable: boolean;
  onSelect: () => void;
}

// Minion trên board — click chọn attacker hoặc chọn target.
export function MinionView({ minion, selectable, selected, targetable, onSelect }: MinionViewProps) {
  const className = [
    'minion-view',
    selectable ? 'minion-selectable' : '',
    selected ? 'minion-selected' : '',
    targetable ? 'minion-targetable' : '',
    minion.hasTaunt ? 'minion-taunt' : '',
    minion.canAttack ? 'minion-ready' : 'minion-sleeping',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button type="button" className={className} onClick={onSelect} title={minion.hasTaunt ? `${minion.name} (Taunt)` : minion.name}>
      <img className="minion-art" src={resolveAsset(minion.imagePath)} alt={minion.name} draggable={false} />
      {minion.hasTaunt && <span className="minion-taunt-badge">🛡</span>}
      <span className="minion-name">{minion.name}</span>
      <span className="minion-attack">{minion.attack}</span>
      <span className="minion-health">{minion.health}</span>
    </button>
  );
}
