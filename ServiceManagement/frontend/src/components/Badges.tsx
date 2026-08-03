import React from 'react';
import type { Priority, WorkOrderStatus } from '../types';

const statusColors: Record<WorkOrderStatus, string> = {
  NEW: '#6b7280',
  ASSIGNED: '#2563eb',
  IN_PROGRESS: '#d97706',
  ON_HOLD: '#9333ea',
  COMPLETED: '#059669',
  CLOSED: '#374151',
  CANCELLED: '#dc2626'
};

const priorityColors: Record<Priority, string> = {
  CRITICAL: '#dc2626',
  HIGH: '#ea580c',
  MEDIUM: '#d97706',
  LOW: '#059669'
};

export function StatusBadge({ status }: { status: WorkOrderStatus }) {
  return (
    <span className="badge" style={{ backgroundColor: statusColors[status] }}>
      {status.replace('_', ' ')}
    </span>
  );
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <span className="badge badge-outline" style={{ borderColor: priorityColors[priority], color: priorityColors[priority] }}>
      {priority}
    </span>
  );
}
