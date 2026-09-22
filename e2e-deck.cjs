async function deckFor(baseUrl, heroClass) {
  const response = await fetch(`${baseUrl}/api/game-catalog`);
  if (!response.ok) throw new Error(`catalog ${response.status}`);
  const catalog = await response.json();
  const deck = catalog.suggestedDecks[heroClass];
  if (!Array.isArray(deck) || deck.length !== catalog.deckRules.deckSize) {
    throw new Error(`invalid suggested deck for ${heroClass}`);
  }
  return deck;
}

module.exports = { deckFor };
