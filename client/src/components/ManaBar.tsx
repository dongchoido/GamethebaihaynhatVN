import { resolveAsset, UI_IMAGE } from '../assets/assetRegistry';
import { MAX_MANA_DISPLAY } from '../gameConfig';

interface ManaBarProps {
  mana: number;
  maxMana: number;
}

// Thanh mana — crystal sáng/tối theo mana hiện tại.
export function ManaBar({ mana, maxMana }: ManaBarProps) {
  const slots = Array.from({ length: Math.max(maxMana, 1) }, (_, i) => i);
  return (
    <div className="mana-bar" title={`${mana}/${maxMana}`}>
      <span className="mana-text">
        {mana}/{maxMana}
      </span>
      {slots.slice(0, MAX_MANA_DISPLAY).map((i) => (
        <img
          key={i}
          className="mana-crystal"
          src={resolveAsset(i < mana ? UI_IMAGE.manaCrystal : UI_IMAGE.manaCrystalDark)}
          alt={i < mana ? 'mana' : 'empty mana'}
          draggable={false}
        />
      ))}
    </div>
  );
}
