import { useEffect, useState } from 'react';
import { ServerEvents } from '@coincard/shared';
import { socketService } from './socketService';

export function useConnection() {
  const [connected, setConnected] = useState(socketService.isConnected());
  useEffect(() => {
    const socket = socketService.getSocket();
    const down = () => setConnected(false);
    const synced = () => setConnected(true);
    socket.on('disconnect', down);
    socket.on('connect_error', down);
    socket.on(ServerEvents.PLAYER_JOINED, synced);
    socket.on(ServerEvents.GAME_STATE_UPDATED, synced);
    return () => {
      socket.off('disconnect', down); socket.off('connect_error', down);
      socket.off(ServerEvents.PLAYER_JOINED, synced); socket.off(ServerEvents.GAME_STATE_UPDATED, synced);
    };
  }, []);
  return connected;
}
