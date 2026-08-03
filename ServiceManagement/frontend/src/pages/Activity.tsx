import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchActivity } from '../api/services';
import type { ActivityEntry } from '../types';
import { StatusBadge } from '../components/Badges';

export default function Activity() {
  const [entries, setEntries] = useState<ActivityEntry[]>([]);

  useEffect(() => {
    fetchActivity(100).then(setEntries).catch(() => {});
  }, []);

  return (
    <div>
      <h1>Activity Log</h1>
      <p style={{ color: 'var(--color-muted)', fontSize: '0.85rem', marginTop: '-0.5rem' }}>
        Every status change across every work order, most recent first — built from the same
        append-only audit trail shown on each work order's detail page.
      </p>

      <table className="table">
        <thead>
          <tr>
            <th>When</th>
            <th>Work Order</th>
            <th>From</th>
            <th>To</th>
            <th>By</th>
            <th>Note</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((e) => (
            <tr key={e.id}>
              <td>{new Date(e.changedAt).toLocaleString()}</td>
              <td>
                <Link to={`/work-orders/${e.workOrderId}`}>{e.workOrderCode}</Link>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-muted)' }}>{e.workOrderTitle}</div>
              </td>
              <td>{e.fromStatus ?? '—'}</td>
              <td><StatusBadge status={e.toStatus} /></td>
              <td>{e.changedByName}</td>
              <td>{e.note ?? ''}</td>
            </tr>
          ))}
          {entries.length === 0 && <tr><td colSpan={6}>No activity recorded yet.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
