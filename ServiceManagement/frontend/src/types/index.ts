export type Role = 'DISPATCHER' | 'TECHNICIAN' | 'MANAGER' | 'CUSTOMER';

export type WorkOrderStatus =
  | 'NEW'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CLOSED'
  | 'CANCELLED';

export type Priority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  customerId: number | null;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Customer {
  id: number;
  name: string;
  contactEmail: string | null;
  contactPhone: string | null;
}

export interface Site {
  id: number;
  customerId: number;
  customerName: string;
  name: string;
  address: string;
  city: string | null;
  state: string | null;
  postalCode: string | null;
}

export interface WorkOrder {
  id: number;
  code: string;
  title: string;
  description: string | null;
  customerId: number;
  customerName: string;
  siteId: number;
  siteName: string;
  assignedTechnicianId: number | null;
  assignedTechnicianName: string | null;
  status: WorkOrderStatus;
  priority: Priority;
  dueAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  closedAt: string | null;
  resolutionNotes: string | null;
  createdAt: string;
  updatedAt: string;
  overdue: boolean;
  atRisk: boolean;
  totalPartsCost: number;
  totalMinutesLogged: number;
}

export interface WorkOrderStatusHistoryEntry {
  id: number;
  fromStatus: WorkOrderStatus | null;
  toStatus: WorkOrderStatus;
  changedByName: string;
  note: string | null;
  changedAt: string;
}

export interface Part {
  id: number;
  sku: string;
  name: string;
  unitCost: number;
  quantityOnHand: number;
  reorderThreshold: number;
  lowStock: boolean;
}

export interface DashboardSummary {
  totalOpenWorkOrders: number;
  overdueWorkOrders: number;
  byStatus: Record<string, number>;
  byPriority: Record<string, number>;
  byTechnician: Record<string, number>;
  lowStockParts: number;
  slaCompliancePercent: number | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export type NotificationType = 'ASSIGNMENT' | 'STATUS_CHANGE' | 'SLA_BREACH';

export interface AppNotification {
  id: number;
  type: NotificationType;
  message: string;
  workOrderId: number | null;
  workOrderCode: string | null;
  read: boolean;
  createdAt: string;
}

export interface ActivityEntry {
  id: number;
  workOrderId: number;
  workOrderCode: string;
  workOrderTitle: string;
  fromStatus: WorkOrderStatus | null;
  toStatus: WorkOrderStatus;
  changedByName: string;
  note: string | null;
  changedAt: string;
}

export interface TechnicianWorkload {
  id: number;
  name: string;
  email: string;
  openJobCount: number;
}

export interface Attachment {
  id: number;
  originalFilename: string;
  contentType: string | null;
  fileSize: number;
  uploadedByName: string;
  uploadedAt: string;
  downloadUrl: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details: string[];
}
