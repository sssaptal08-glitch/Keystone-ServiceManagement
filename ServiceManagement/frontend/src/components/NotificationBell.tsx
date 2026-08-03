import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '../context/AuthContext';
import {
  fetchNotifications,
  fetchUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead
} from '../api/services';
import type { AppNotification } from '../types';

const WS_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api\/?$/, '');

export default function NotificationBell() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const wrapRef = useRef<HTMLDivElement>(null);

  const reload = useCallback(() => {
    fetchNotifications().then((page) => setNotifications(page.content)).catch(() => {});
    fetchUnreadNotificationCount().then(setUnreadCount).catch(() => {});
  }, []);

  useEffect(reload, [reload]);

  // Live push: a new SLA breach or assignment notification appears instantly without polling.
  useEffect(() => {
    if (!user) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws`),
      reconnectDelay: 4000,
      onConnect: () => {
        client.subscribe('/topic/notifications', (frame) => {
          try {
            const event = JSON.parse(frame.body) as { recipientId: number; notification: AppNotification };
            if (event.recipientId === user.id) {
              setNotifications((prev) => [event.notification, ...prev].slice(0, 20));
              setUnreadCount((c) => c + 1);
            }
          } catch {
            // ignore malformed frames
          }
        });
      }
    });
    client.activate();
    return () => { client.deactivate(); };
  }, [user]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleItemClick = async (n: AppNotification) => {
    if (!n.read) {
      await markNotificationRead(n.id).catch(() => {});
      setNotifications((prev) => prev.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      setUnreadCount((c) => Math.max(0, c - 1));
    }
    setOpen(false);
    if (n.workOrderId) navigate(`/work-orders/${n.workOrderId}`);
  };

  const handleMarkAll = async () => {
    await markAllNotificationsRead().catch(() => {});
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);
  };

  return (
    <div className="notification-bell-wrap" ref={wrapRef}>
      <button className="btn btn-ghost notification-bell-btn" onClick={() => setOpen((o) => !o)} aria-label="Notifications">
        <Bell size={16} />
        {unreadCount > 0 && <span className="notification-unread-dot">{unreadCount > 9 ? '9+' : unreadCount}</span>}
      </button>

      {open && (
        <div className="notification-dropdown">
          <div className="notification-dropdown-header">
            Notifications
            {unreadCount > 0 && <button onClick={handleMarkAll}>Mark all read</button>}
          </div>
          {notifications.length === 0 && <div className="notification-empty">You're all caught up.</div>}
          {notifications.map((n) => (
            <button key={n.id} className={`notification-item ${!n.read ? 'unread' : ''}`} onClick={() => handleItemClick(n)}>
              {n.message}
              <div className="notification-item-meta">
                {n.workOrderCode ? `${n.workOrderCode} · ` : ''}{new Date(n.createdAt).toLocaleString()}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
