import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { fetchDashboardSummary, fetchWorkOrders } from '../api/services';
import type { DashboardSummary, WorkOrder } from '../types';
import { StatusBadge, PriorityBadge } from '../components/Badges';
import { Link } from 'react-router-dom';
import { ClipboardList, AlertTriangle, PackageX, Radio } from 'lucide-react';
import { useWorkOrderSocket } from '../hooks/useWorkOrderSocket';
import {
  BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend
} from 'recharts';

export default function Dashboard() {
  const { user } = useAuth();

  if (user?.role === 'TECHNICIAN') return <TechnicianDashboard technicianId={user.id} />;
  if (user?.role === 'CUSTOMER') return <CustomerDashboard customerId={user.customerId} />;
  return <ManagerDashboard />;
}

const STATUS_COLORS: Record<string, string> = {
  NEW: '#6b7280', ASSIGNED: '#2563eb', IN_PROGRESS: '#d97706', ON_HOLD: '#9333ea',
  COMPLETED: '#059669', CLOSED: '#374151', CANCELLED: '#dc2626'
};
const PRIORITY_COLORS: Record<string, string> = {
  CRITICAL: '#dc2626', HIGH: '#ea580c', MEDIUM: '#d97706', LOW: '#059669'
};
const TECH_BAR_COLOR = '#2451c4';

function ManagerDashboard() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [recent, setRecent] = useState<WorkOrder[]>([]);

  const reload = useCallback(() => {
    fetchDashboardSummary().then(setSummary).catch(() => {});
    fetchWorkOrders({ size: 8 }).then((page) => setRecent(page.content)).catch(() => {});
  }, []);

  useEffect(reload, [reload]);

  // Live updates: any create/assign/status-change event elsewhere triggers a refetch here,
  // so counts and the recent list stay current without polling.
  const connected = useWorkOrderSocket(useCallback(() => reload(), [reload]));

  const statusData = summary
    ? Object.entries(summary.byStatus)
        .filter(([, v]) => v > 0)
        .map(([name, value]) => ({ name: name.replace('_', ' '), value, fill: STATUS_COLORS[name] }))
    : [];

  const priorityData = summary
    ? Object.entries(summary.byPriority).map(([name, value]) => ({ name, value, fill: PRIORITY_COLORS[name] }))
    : [];

  const technicianData = summary
    ? Object.entries(summary.byTechnician).map(([name, value]) => ({ name, value }))
    : [];

  return (
    <div>
      <div className="page-header">
        <h1>Operations Dashboard</h1>
        <span className={`live-indicator ${connected ? 'live-on' : 'live-off'}`}>
          <Radio size={13} />
          {connected ? 'Live' : 'Offline'}
        </span>
      </div>

      {summary && (
        <div className="stat-grid">
          <StatCard label="Open Work Orders" value={summary.totalOpenWorkOrders} tone="default" icon={ClipboardList} />
          <StatCard label="Overdue (SLA breach)" value={summary.overdueWorkOrders} tone="danger" icon={AlertTriangle} />
          <StatCard label="Low Stock Parts" value={summary.lowStockParts} tone="warning" icon={PackageX} />
        </div>
      )}

      {summary && (
        <div className="panel-grid-3">
          <div className="panel">
            <h3>By Status</h3>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={statusData} layout="vertical" margin={{ left: 10, right: 20 }}>
                <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="name" width={90} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="value" radius={[0, 4, 4, 0]}>
                  {statusData.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="panel">
            <h3>Open by Priority</h3>
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={priorityData} dataKey="value" nameKey="name" innerRadius={45} outerRadius={75} paddingAngle={2}>
                  {priorityData.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
                </Pie>
                <Tooltip />
                <Legend verticalAlign="bottom" height={24} wrapperStyle={{ fontSize: 11 }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="panel">
            <h3>SLA Compliance (all time)</h3>
            {summary.slaCompliancePercent === null ? (
              <div className="gauge-wrap">
                <div className="gauge-label">No completed work orders yet — nothing to measure.</div>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie
                    data={[
                      { name: 'Met SLA', value: summary.slaCompliancePercent },
                      { name: 'Missed SLA', value: 100 - summary.slaCompliancePercent }
                    ]}
                    dataKey="value"
                    innerRadius={55}
                    outerRadius={80}
                    startAngle={90}
                    endAngle={-270}
                  >
                    <Cell fill="#059669" />
                    <Cell fill="#e5e7eb" />
                  </Pie>
                  <text x="50%" y="48%" textAnchor="middle" dominantBaseline="middle" style={{ fontSize: 26, fontWeight: 800, fill: 'var(--color-text)' }}>
                    {summary.slaCompliancePercent}%
                  </text>
                  <text x="50%" y="62%" textAnchor="middle" dominantBaseline="middle" style={{ fontSize: 11, fill: 'var(--color-muted)' }}>
                    SLA met
                  </text>
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      )}

      {summary && technicianData.length > 0 && (
        <div className="panel" style={{ marginBottom: '1.5rem' }}>
          <h3>Open Work by Technician</h3>
          <ResponsiveContainer width="100%" height={Math.max(120, technicianData.length * 40)}>
            <BarChart data={technicianData} layout="vertical" margin={{ left: 10, right: 20 }}>
              <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11 }} />
              <YAxis type="category" dataKey="name" width={120} tick={{ fontSize: 11 }} />
              <Tooltip />
              <Bar dataKey="value" fill={TECH_BAR_COLOR} radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      <h2>Recent Work Orders</h2>
      <table className="table">
        <thead>
          <tr>
            <th>Code</th>
            <th>Title</th>
            <th>Customer</th>
            <th>Status</th>
            <th>Priority</th>
            <th>Technician</th>
          </tr>
        </thead>
        <tbody>
          {recent.map((wo) => (
            <tr key={wo.id}>
              <td><Link to={`/work-orders/${wo.id}`}>{wo.code}</Link></td>
              <td>{wo.title}</td>
              <td>{wo.customerName}</td>
              <td><StatusBadge status={wo.status} /></td>
              <td><PriorityBadge priority={wo.priority} /></td>
              <td>{wo.assignedTechnicianName ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TechnicianDashboard({ technicianId }: { technicianId: number }) {
  const [jobs, setJobs] = useState<WorkOrder[]>([]);

  const reload = useCallback(() => {
    fetchWorkOrders({ technicianId, size: 50 }).then((page) => setJobs(page.content)).catch(() => {});
  }, [technicianId]);

  useEffect(reload, [reload]);
  const connected = useWorkOrderSocket(useCallback(() => reload(), [reload]));

  return (
    <div>
      <div className="page-header">
        <h1>My Jobs</h1>
        <span className={`live-indicator ${connected ? 'live-on' : 'live-off'}`}>
          <Radio size={13} />
          {connected ? 'Live' : 'Offline'}
        </span>
      </div>
      <div className="card-grid">
        {jobs.map((wo) => (
          <Link to={`/work-orders/${wo.id}`} key={wo.id} className="job-card">
            <div className="job-card-header">
              <span className="job-code">{wo.code}</span>
              <PriorityBadge priority={wo.priority} />
            </div>
            <h3>{wo.title}</h3>
            <p>{wo.siteName} — {wo.customerName}</p>
            <StatusBadge status={wo.status} />
            {wo.overdue && <span className="badge" style={{ backgroundColor: '#dc2626' }}>OVERDUE</span>}
          </Link>
        ))}
        {jobs.length === 0 && <p>No jobs assigned right now.</p>}
      </div>
    </div>
  );
}

function CustomerDashboard({ customerId }: { customerId: number | null }) {
  const [requests, setRequests] = useState<WorkOrder[]>([]);

  useEffect(() => {
    if (customerId) fetchWorkOrders({ customerId, size: 50 }).then((page) => setRequests(page.content)).catch(() => {});
  }, [customerId]);

  return (
    <div>
      <h1>My Service Requests</h1>
      <table className="table">
        <thead>
          <tr>
            <th>Code</th>
            <th>Title</th>
            <th>Site</th>
            <th>Status</th>
            <th>Priority</th>
          </tr>
        </thead>
        <tbody>
          {requests.map((wo) => (
            <tr key={wo.id}>
              <td><Link to={`/work-orders/${wo.id}`}>{wo.code}</Link></td>
              <td>{wo.title}</td>
              <td>{wo.siteName}</td>
              <td><StatusBadge status={wo.status} /></td>
              <td><PriorityBadge priority={wo.priority} /></td>
            </tr>
          ))}
          {requests.length === 0 && (
            <tr><td colSpan={5}>No service requests on file.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function StatCard({ label, value, tone, icon: Icon }: { label: string; value: number; tone: 'default' | 'danger' | 'warning'; icon: React.ElementType }) {
  return (
    <div className={`stat-card stat-${tone}`}>
      <Icon size={28} className="stat-icon" />
      <div>
        <div className="stat-value">{value}</div>
        <div className="stat-label">{label}</div>
      </div>
    </div>
  );
}
