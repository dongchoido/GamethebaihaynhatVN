import type { HeroState } from '@coincard/shared';
import { getHeroAsset, resolveAsset, UI_IMAGE } from '../assets/assetRegistry';

interface HeroViewProps {
  hero: HeroState;
  powerUsable: boolean;
  targetable: boolean;
  onHeroClick: () => void;
  onPowerClick: () => void;
}

// Hero: portrait + HP badge + nút hero power.
export function HeroView({ hero, powerUsable, targetable, onHeroClick, onPowerClick }: HeroViewProps) {
  const asset = getHeroAsset(hero.heroClass);
  const heroClassName = ['hero-view', targetable ? 'hero-targetable' : ''].filter(Boolean).join(' ');

  return (
    <div className="hero-block">
      <button type="button" className={heroClassName} onClick={onHeroClick} title={hero.name}>
        <img className="hero-portrait" src={resolveAsset(asset.portrait)} alt={hero.name} draggable={false} />
        <span className="hero-hp" style={{ backgroundImage: `url(${resolveAsset(UI_IMAGE.healthBadge)})` }}>
          {hero.health}
        </span>
      </button>
      <button
        type="button"
        className={powerUsable ? 'hero-power hero-power-usable' : 'hero-power'}
        onClick={onPowerClick}
        disabled={!powerUsable}
        title={`${hero.name} power (${hero.powerCost})`}
      >
        <img
          src={resolveAsset(powerUsable ? asset.power : asset.powerDisabled)}
          alt="hero power"
          draggable={false}
        />
        <span className="hero-power-cost">{hero.powerCost}</span>
      </button>
    </div>
  );
}
