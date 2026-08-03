import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import type { WorkOrder, WorkOrderStatus } from '../types';
import { PriorityBadge } from './Badges';
import { changeWorkOrderStatus } from '../api/services';
import { extractErrorMessage } from '../api/client';

// F4.4: "A board (Kanban by status) shows all open work." Closed/cancelled work orders are
// tracked via the list view and Activity log instead — this board is specifically the
// day-to-day working set a dispatcher watches.
const BOARD_COLUMNS: { status: WorkOrderStatus; label: string; color: string }[] = [
  { status: 'NEW', label: 'New', color: '#6b7280' },
  { status: 'ASSIGNED', label: 'Assigned', color: '#2563eb' },
  { status: 'IN_PROGRESS', label: 'In Progress', color: '#d97706' },
  { status: 'ON_HOLD', label: 'On Hold', color: '#9333ea' },
  { status: 'COMPLETED', label: 'Completed', color: '#059669' }
];

export default function KanbanBoard({
  workOrders, canDrag, onChanged
}: {
  workOrders: WorkOrder[];
  canDrag: boolean;
  onChanged: () => void;
}) {
  const [draggingId, setDraggingId] = useState<number | null>(null);
  const [dragOverColumn, setDragOverColumn] = useState<WorkOrderStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  const byColumn = (status: WorkOrderStatus) => workOrders.filter((wo) => wo.status === status);

  const handleDrop = async (targetStatus: WorkOrderStatus) => {
    setDragOverColumn(null);
    if (draggingId == null) return;
    const wo = workOrders.find((w) => w.id === draggingId);
    setDraggingId(null);
    if (!wo || wo.status === targetStatus) return;

    try {
      await changeWorkOrderStatus(wo.id, targetStatus);
      setError(null);
      onChanged();
    } catch (err) {
      // The server is the single source of truth for legal transitions (e.g. dragging NEW
      // straight to IN_PROGRESS skips ASSIGNED and is correctly rejected with 409) — we just
      // surface whatever it says rather than trying to duplicate the state machine client-side.
      setError(extractErrorMessage(err));
    }
  };

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="kanban-board">
        {BOARD_COLUMNS.map((col) => (
          <div
            key={col.status}
            className={`kanban-column ${dragOverColumn === col.status ? 'drag-over' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setDragOverColumn(col.status); }}
            onDragLeave={() => setDragOverColumn(null)}
            onDrop={(e) => { e.preventDefault(); handleDrop(col.status); }}
          >
            <div className="kanban-column-header" style={{ borderTopColor: col.color }}>
              {col.label}
              <span className="kanban-count">{byColumn(col.status).length}</span>
            </div>
            <div className="kanban-column-body">
              {byColumn(col.status).map((wo) => (
                <div
                  key={wo.id}
                  className={`kanban-card ${wo.overdue ? 'kanban-card-overdue' : wo.atRisk ? 'kanban-card-at-risk' : ''}`}
                  draggable={canDrag}
                  onDragStart={() => setDraggingId(wo.id)}
                  onDragEnd={() => setDraggingId(null)}
                >
                  <div className="kanban-card-top">
                    <span className="job-code">{wo.code}</span>
                    <PriorityBadge priority={wo.priority} />
                  </div>
                  <Link to={`/work-orders/${wo.id}`} className="kanban-card-title">{wo.title}</Link>
                  <div className="kanban-card-meta">{wo.customerName} — {wo.siteName}</div>
                  <div className="kanban-card-meta">{wo.assignedTechnicianName ?? 'Unassigned'}</div>
                  {wo.overdue && <span className="badge" style={{ backgroundColor: '#dc2626' }}>OVERDUE</span>}
                  {!wo.overdue && wo.atRisk && <span className="badge" style={{ backgroundColor: '#d97706' }}>AT RISK</span>}
                </div>
              ))}
              {byColumn(col.status).length === 0 && <div className="kanban-empty">No jobs</div>}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
