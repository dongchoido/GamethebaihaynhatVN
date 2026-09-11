-- A room may host multiple games. Keep all existing game rows.
DROP INDEX IF EXISTS "Game_roomCode_key";
CREATE INDEX "Game_roomCode_idx" ON "Game"("roomCode");
