import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchWorkOrders, fetchCustomers, fetchSites, fetchTechnicians, createWorkOrder } from '../api/services';
import type { Customer, Site, TechnicianWorkload, WorkOrder, WorkOrderStatus } from '../types';
import { StatusBadge, PriorityBadge } from '../components/Badges';
import Pagination from '../components/Pagination';
import KanbanBoard from '../components/KanbanBoard';
import { extractErrorMessage } from '../api/client';
import { Search, LayoutGrid, List as ListIcon } from 'lucide-react';

const STATUSES: WorkOrderStatus[] = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CLOSED', 'CANCELLED'];
const PAGE_SIZE = 10;

export default function WorkOrders() {
  const { user } = useAuth();
  const [view, setView] = useState<'list' | 'board'>('list');
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [boardOrders, setBoardOrders] = useState<WorkOrder[]>([]);
  const [statusFilter, setStatusFilter] = useState<WorkOrderStatus | ''>('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const canCreate = user?.role === 'DISPATCHER' || user?.role === 'MANAGER' || user?.role === 'CUSTOMER';
  const canDragBoard = user?.role === 'DISPATCHER' || user?.role === 'MANAGER';

  const load = useCallback(() => {
    fetchWorkOrders({
      status: statusFilter || undefined,
      search: search || undefined,
      page,
      size: PAGE_SIZE
    }).then((res) => {
      setOrders(res.content);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    }).catch(() => {});
  }, [statusFilter, search, page]);

  const loadBoard = useCallback(() => {
    fetchWorkOrders({ size: 200 }).then((res) => setBoardOrders(res.content)).catch(() => {});
  }, []);

  useEffect(load, [load]);
  useEffect(() => { if (view === 'board') loadBoard(); }, [view, loadBoard]);

  // Reset to page 0 whenever the filter/search changes so we don't land on an empty page.
  useEffect(() => { setPage(0); }, [statusFilter, search]);

  return (
    <div>
      <div className="page-header">
        <h1>Work Orders</h1>
        {canCreate && (
          <button className="btn btn-primary" onClick={() => setShowForm((s) => !s)}>
            {showForm ? 'Cancel' : user?.role === 'CUSTOMER' ? '+ Raise Request' : '+ New Work Order'}
          </button>
        )}
      </div>

      {showForm && <NewWorkOrderForm isCustomer={user?.role === 'CUSTOMER'} onCreated={() => { setShowForm(false); load(); if (view === 'board') loadBoard(); }} />}

      <div className="view-toggle">
        <button className={`btn btn-secondary ${view === 'list' ? 'active' : ''}`} onClick={() => setView('list')}>
          <ListIcon size={14} style={{ verticalAlign: -2, marginRight: 4 }} />List
        </button>
        <button className={`btn btn-secondary ${view === 'board' ? 'active' : ''}`} onClick={() => setView('board')}>
          <LayoutGrid size={14} style={{ verticalAlign: -2, marginRight: 4 }} />Board
        </button>
      </div>

      {view === 'board' ? (
        <KanbanBoard workOrders={boardOrders} canDrag={canDragBoard} onChanged={loadBoard} />
      ) : (
        <>
          <div className="search-bar">
            <div style={{ position: 'relative', flex: 1, maxWidth: 320 }}>
              <Search size={14} style={{ position: 'absolute', left: 10, top: 10, color: 'var(--color-muted)' }} />
              <input
                style={{ paddingLeft: '2rem', width: '100%' }}
                placeholder="Search by title or code…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem' }}>
              Status:
              <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as WorkOrderStatus | '')}>
                <option value="">All</option>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>{s.replace('_', ' ')}</option>
                ))}
              </select>
            </label>
          </div>

          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Title</th>
                <th>Customer / Site</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Technician</th>
                <th>Due</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((wo) => (
                <tr key={wo.id} className={wo.overdue ? 'row-overdue' : ''}>
                  <td><Link to={`/work-orders/${wo.id}`}>{wo.code}</Link></td>
                  <td>{wo.title}</td>
                  <td>{wo.customerName} — {wo.siteName}</td>
                  <td><StatusBadge status={wo.status} /></td>
                  <td><PriorityBadge priority={wo.priority} /></td>
                  <td>{wo.assignedTechnicianName ?? '—'}</td>
                  <td>
                    {wo.dueAt ? new Date(wo.dueAt).toLocaleString() : '—'}
                    {wo.overdue && ' ⚠️'}
                    {!wo.overdue && wo.atRisk && ' ⏳'}
                  </td>
                </tr>
              ))}
              {orders.length === 0 && <tr><td colSpan={7}>No work orders found.</td></tr>}
            </tbody>
          </table>

          <Pagination page={page} totalPages={totalPages} totalElements={totalElements} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}

function NewWorkOrderForm({ isCustomer, onCreated }: { isCustomer: boolean; onCreated: () => void }) {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [technicians, setTechnicians] = useState<TechnicianWorkload[]>([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [customerId, setCustomerId] = useState<number | ''>('');
  const [siteId, setSiteId] = useState<number | ''>('');
  const [priority, setPriority] = useState('MEDIUM');
  const [technicianId, setTechnicianId] = useState<number | ''>('');
  const [dueAt, setDueAt] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    // A customer raising their own request only ever sees their own sites — fetchSites() with
    // no customerId returns everything for dispatcher/manager, but for a customer the backend
    // itself only exposes their own sites via the object-level scoping already in place.
    if (!isCustomer) fetchCustomers({ size: 200 }).then((res) => setCustomers(res.content)).catch(() => {});
    if (!isCustomer) fetchTechnicians().then(setTechnicians).catch(() => {});
    if (isCustomer) fetchSites().then(setSites).catch(() => {});
  }, [isCustomer]);

  useEffect(() => {
    if (!isCustomer && customerId) fetchSites(Number(customerId)).then(setSites).catch(() => {});
    else if (!isCustomer) setSites([]);
  }, [customerId, isCustomer]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!siteId || (!isCustomer && !customerId)) {
      setError(isCustomer ? 'Please select a site.' : 'Please select a customer and site.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await createWorkOrder({
        title,
        description,
        customerId: isCustomer ? undefined : Number(customerId),
        siteId: Number(siteId),
        priority,
        dueAt: dueAt ? new Date(dueAt).toISOString() : undefined,
        assignedTechnicianId: isCustomer ? null : (technicianId ? Number(technicianId) : null)
      });
      onCreated();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="panel form-panel" onSubmit={handleSubmit}>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="form-grid">
        <label>
          Title
          <input value={title} onChange={(e) => setTitle(e.target.value)} required />
        </label>
        <label>
          Priority
          <select value={priority} onChange={(e) => setPriority(e.target.value)}>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </label>
        {!isCustomer && (
          <label>
            Customer
            <select value={customerId} onChange={(e) => setCustomerId(e.target.value ? Number(e.target.value) : '')} required>
              <option value="">Select customer</option>
              {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </label>
        )}
        <label>
          Site
          <select value={siteId} onChange={(e) => setSiteId(e.target.value ? Number(e.target.value) : '')} required disabled={!isCustomer && !customerId}>
            <option value="">Select site</option>
            {sites.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </label>
        {!isCustomer && (
          <label>
            Assign technician (optional)
            <select value={technicianId} onChange={(e) => setTechnicianId(e.target.value ? Number(e.target.value) : '')}>
              <option value="">Unassigned</option>
              {technicians.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.openJobCount} open {t.openJobCount === 1 ? 'job' : 'jobs'})
                </option>
              ))}
            </select>
          </label>
        )}
        <label>
          Due date/time (optional — defaults based on priority)
          <input type="datetime-local" value={dueAt} onChange={(e) => setDueAt(e.target.value)} />
        </label>
      </div>
      <label>
        Description
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
      </label>
      <button className="btn btn-primary" type="submit" disabled={submitting}>
        {submitting ? 'Submitting...' : isCustomer ? 'Raise Request' : 'Create Work Order'}
      </button>
    </form>
  );
}
