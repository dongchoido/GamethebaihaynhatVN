# Luật chơi CoinCard

Lấy cảm hứng Hearthstone, code và số liệu tự xây dựng cho project này.

## Tổng quan
- 2 người chơi, mỗi hero 30 HP. Hero HP <= 0 → thua.
- Tay đầu: người đi trước 3 lá, người đi sau 4 lá.
- Opening guarantee: tay đầu luôn có ít nhất 1 lá ≤ 2 mana.
- Mana: lượt n có n mana (tối đa 10), refill đầu mỗi turn, draw 1 lá.

## Board & tay
- Tối đa 7 minion mỗi bên, tối đa 10 lá trên tay (thừa bị burn).
- Minion mới summon bị summoning sickness (trừ CHARGE).
- Minion vs minion: cả hai cùng gây damage. Minion vs hero: trừ HP hero.

## Hero power (2 mana)
- MAGE Fireblast: 1 damage vào hero địch.
- HUNTER Steady Shot: 2 damage vào hero địch.
- PALADIN Reinforce: summon Silver Hand Recruit 1/1.
- PRIEST Lesser Heal: hồi 2 HP hero mình.
- WARLOCK Life Tap: chịu 2 damage, draw 1 lá.

## Deck
- 30 lá, draft theo curve (ưu tiên lá rẻ) + Fisher-Yates shuffle phía server.
- Token (Sheep, Recruit) không vào deck (collectible = false).

## Chi tiết effect từng lá xem `data/cards.json`.
