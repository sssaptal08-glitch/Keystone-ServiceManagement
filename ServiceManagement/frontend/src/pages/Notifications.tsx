import React, { useEffect, useState } from 'react';
import { fetchNotifications, markNotificationRead, markAllNotificationsRead } from '../api/services';
import type { AppNotification } from '../types';

export default function Notifications() {
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchNotifications().then((page) => setNotifications(page.content)).catch(() => {});
  }, []);

  const handleRead = async (id: number) => {
    try {
      await markNotificationRead(id);
      setNotifications((prev) => prev.map((item) => (item.id === id ? { ...item, read: true } : item)));
    } catch (err) {
      setError('Unable to mark notification read.');
    }
  };

  const handleReadAll = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));
    } catch (err) {
      setError('Unable to mark all notification read.');
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Notifications</h1>
          <p className="page-subtitle">Actionable alerts and updates from scheduling, SLA monitoring, and customer requests.</p>
        </div>
        <button className="btn btn-secondary" onClick={handleReadAll}>Mark all read</button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="panel">
        {notifications.length === 0 ? (
          <div className="empty-state">No notifications yet.</div>
        ) : (
          <div className="notification-list">
            {notifications.map((notification) => (
              <div key={notification.id} className={`notification-item ${notification.read ? '' : 'unread'}`}>
                <div>
                  <div className="notification-item-title">{notification.message}</div>
                  <div className="notification-item-meta">{notification.workOrderCode ? `${notification.workOrderCode} · ` : ''}{new Date(notification.createdAt).toLocaleString()}</div>
                </div>
                {!notification.read && (
                  <button className="btn btn-ghost" onClick={() => handleRead(notification.id)}>Mark read</button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
