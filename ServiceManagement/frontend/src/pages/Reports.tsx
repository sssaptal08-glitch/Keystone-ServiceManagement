import React, { useEffect, useState } from 'react';
import { fetchDashboardSummary, fetchActivity } from '../api/services';
import type { DashboardSummary, ActivityEntry } from '../types';
import { StatusBadge } from '../components/Badges';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  CartesianGrid,
  Legend
} from 'recharts';

const STATUS_COLORS: Record<string, string> = {
  NEW: '#6b7280',
  ASSIGNED: '#2563eb',
  IN_PROGRESS: '#d97706',
  ON_HOLD: '#9333ea',
  COMPLETED: '#059669',
  CLOSED: '#334155',
  CANCELLED: '#dc2626'
};

export default function Reports() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [activityRows, setActivityRows] = useState<ActivityEntry[]>([]);

  useEffect(() => {
    fetchDashboardSummary().then(setSummary).catch(() => {});
    fetchActivity(12).then(setActivityRows).catch(() => {});
  }, []);

  const statusData = summary
    ? Object.entries(summary.byStatus).map(([name, value]) => ({ name, value, fill: STATUS_COLORS[name] }))
    : [];

  const priorityData = summary
    ? Object.entries(summary.byPriority).map(([name, value]) => ({ name, value, fill: STATUS_COLORS[name] || '#2563eb' }))
    : [];

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Reports</h1>
          <p className="page-subtitle">Detailed analytics, operational health, and recent service performance.</p>
        </div>
      </div>

      <div className="panel-grid-3">
        <div className="panel">
          <h3>Service Pipeline</h3>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={statusData} margin={{ top: 12, right: 12, left: 0, bottom: 0 }}>
              <XAxis dataKey="name" tick={{ fontSize: 12 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                {statusData.map((entry) => (
                  <Cell key={entry.name} fill={entry.fill} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="panel">
          <h3>SLA Compliance</h3>
          <div className="report-summary-card">
            <div className="report-summary-value">{summary?.slaCompliancePercent ?? 0}%</div>
            <div className="report-summary-label">Total SLA met across all completed work orders</div>
          </div>
        </div>

        <div className="panel">
          <h3>Low stock alerts</h3>
          <div className="report-summary-card report-summary-warning">
            <div className="report-summary-value">{summary?.lowStockParts ?? 0}</div>
            <div className="report-summary-label">Parts currently under reorder threshold</div>
          </div>
        </div>
      </div>

      <div className="panel-grid">
        <div className="panel">
          <h3>Status distribution</h3>
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie data={statusData} dataKey="value" nameKey="name" innerRadius={50} outerRadius={90} paddingAngle={4}>
                {statusData.map((entry) => (
                  <Cell key={entry.name} fill={entry.fill} />
                ))}
              </Pie>
              <Tooltip />
              <Legend verticalAlign="bottom" height={24} />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="panel">
          <h3>Recent operations activity</h3>
          <div className="activity-report-list">
            {activityRows.length === 0 ? (
              <p className="empty-state">No recent activity available.</p>
            ) : (
              <ul>
                {activityRows.map((entry) => (
                  <li key={entry.id}>
                    <div className="activity-row-title">{entry.workOrderCode} · {entry.workOrderTitle}</div>
                    <div className="activity-row-meta">
                      {entry.changedByName} moved from {entry.fromStatus ?? 'NEW'} to {entry.toStatus}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
