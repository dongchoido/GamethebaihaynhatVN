# Asset Mapping

Nguồn: `client/public/assets/`. Không dùng ảnh ngoài.

## Card images (`assets/images/Minions/`)

| Filename | Card | Loại | Đúng slug |
|---|---|---|---|
| Argent Commander.png | Argent Commander | MINION | argent-commander |
| Bloodfen Raptor.png | Bloodfen Raptor | MINION | bloodfen-raptor |
| Boulderfist Ogre.png | Boulderfist Ogre | MINION | boulderfist-ogre |
| Chilwind Yeti.png | Chillwind Yeti | MINION | chillwind-yeti |
| Chromaggus.png | Chromaggus | MINION | chromaggus |
| Colossus of the Moon.png | Colossus of the Moon | MINION | colossus-of-the-moon |
| Core Hound.png | Core Hound | MINION | core-hound |
| Frostwolf Grunt.png | Frostwolf Grunt | MINION | frostwolf-grunt |
| Goldshire Footman.png | Goldshire Footman | MINION | goldshire-footman |
| Icehowl.png | Icehowl | MINION | icehowl |
| King Krush.png | King Krush | MINION | king-krush |
| Prophet Velen.png | Prophet Velen | MINION | prophet-velen |
| Stonetusk Boar.png | Stonetusk Boar | MINION | stonetusk-boar |
| Sunwalker.png | Sunwalker | MINION | sunwalker |
| Tirion Fordring.png | Tirion Fordring | MINION | tirion-fordring |
| Wilfred Fizzlebang.png | Wilfred Fizzlebang | MINION | wilfred-fizzlebang |
| Wolfrider.png | Wolfrider | MINION | wolfrider |
| Silver Hand Recruit.png | Silver Hand Recruit | MINION | silver-hand-recruit |
| Sheep.png | Sheep | MINION | sheep |
| Curse of Weakness.png | Curse of Weakness | SPELL | curse-of-weakness |
| Divine Spirit.png | Divine Spirit | SPELL | divine-spirit |
| Flamestrike.png | Flamestrike | SPELL | flamestrike |
| Holy Nova.png | Holy Nova | SPELL | holy-nova |
| Kill Command.png | Kill Command | SPELL | kill-command |
| Level Up!.png | Level Up! | SPELL | level-up |
| Multi-Shot.png | Multi-Shot | SPELL | multi-shot |
| Polymorph.png | Polymorph | SPELL | polymorph |
| Pyroblast.png | Pyroblast | SPELL | pyroblast |
| Seal Of Champions.png | Seal of Champions | SPELL | seal-of-champions |
| Shadow Word Death.png | Shadow Word: Death | SPELL | shadow-word-death |
| Siphon Soul.png | Siphon Soul | SPELL | siphon-soul |
| Twisting nether.png | Twisting Nether | SPELL | twisting-nether |
| The LichKing.png | The Lich King | MINION | the-lich-king |
| Kalycgos.png | Kalecgos | MINION | kalecgos |

## Hero images (`assets/images/Heros/`)

| File | Hero | Class |
|---|---|---|
| Jaina Proudmoore.png | Jaina | MAGE |
| Rexxar.png | Rexxar | HUNTER |
| Uther Lightbringer.png | Uther | PALADIN |
| Anduin-Wrynn.png | Anduin | PRIEST |
| Gul'dan.png | Gul'dan | WARLOCK |

Win/Defeat splash: `Mage-Win.png`, `Hunter-Win.png`, ...

## Hero power (`assets/images/HeroPower/`)
`Mage.png`, `Hunter.png`, `Paladin.png`, `Priest.png`, `Warlock.png` (+ `Disabled`)

## UI/design (`assets/images/design/`)
`PlayGround.jpg` → game background, `Background.jpg` → board, `manacrystal.png` / `manacrystal_dark.png` → mana, `HealthBG.png` → HP badge, `end turn.png` / `End_Turn_Disabled.png` → end-turn, `shop.png` / `start.png` → menu, `start.png` / `StartBG.jpg` → landing, `CardViewBack.png` → overlay, `NoCardView.png` → empty, `MessageBar.png` → status.

(Sounds: `assets/sounds/*.wav`)

## Quy tắc
- Card → slug → `assets/images/Minions/<Tên>.png`
- Hero → `assets/images/Heros/<Tên>.png`
- Không đổi tên file; đường dẫn tập trung ở `client/src/assets/assetRegistry.ts`.
