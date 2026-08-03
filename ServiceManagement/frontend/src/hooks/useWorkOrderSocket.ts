import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { WorkOrder } from '../types';

export interface WorkOrderEvent {
  type: 'CREATED' | 'ASSIGNED' | 'STATUS_CHANGED';
  workOrder: WorkOrder;
}

const WS_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api\/?$/, '');

/**
 * Subscribes to /topic/work-orders over STOMP-over-SockJS and calls onEvent for every
 * create/assign/status-change broadcast from the backend. Falls back silently (no crash) if
 * the socket can't connect — the app still works via plain REST polling in that case.
 */
export function useWorkOrderSocket(onEvent: (event: WorkOrderEvent) => void) {
  const [connected, setConnected] = useState(false);
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws`),
      reconnectDelay: 4000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/work-orders', (message) => {
          try {
            const event = JSON.parse(message.body) as WorkOrderEvent;
            onEventRef.current(event);
          } catch {
            // ignore malformed frames
          }
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
      onWebSocketError: () => setConnected(false)
    });

    client.activate();
    return () => {
      client.deactivate();
    };
  }, []);

  return connected;
}
