import { apiClient } from './client';
import type {
  ActivityEntry,
  AppNotification,
  Attachment,
  AuthResponse,
  Customer,
  DashboardSummary,
  PageResponse,
  Part,
  Site,
  TechnicianWorkload,
  User,
  WorkOrder,
  WorkOrderStatus,
  WorkOrderStatusHistoryEntry
} from '../types';

// --- Auth ---
export const login = (email: string, password: string) =>
  apiClient.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data);

export const register = (payload: {
  name: string;
  email: string;
  password: string;
  role: string;
  customerId?: number | null;
}) => apiClient.post<AuthResponse>('/auth/register', payload).then((r) => r.data);

export const fetchMe = () => apiClient.get<User>('/users/me').then((r) => r.data);

export const fetchTechnicians = () =>
  apiClient.get<TechnicianWorkload[]>('/users/technicians').then((r) => r.data);

// --- Customers ---
export const fetchCustomers = (params?: { search?: string; page?: number; size?: number }) =>
  apiClient.get<PageResponse<Customer>>('/customers', { params }).then((r) => r.data);

export const createCustomer = (payload: Partial<Customer>) =>
  apiClient.post<Customer>('/customers', payload).then((r) => r.data);

// --- Sites ---
export const fetchSites = (customerId?: number) =>
  apiClient
    .get<Site[]>('/sites', { params: customerId ? { customerId } : {} })
    .then((r) => r.data);

export const createSite = (payload: {
  customerId: number;
  name: string;
  address: string;
  city?: string;
  state?: string;
  postalCode?: string;
}) => apiClient.post<Site>('/sites', payload).then((r) => r.data);

// --- Work Orders ---
export const fetchWorkOrders = (filters?: {
  status?: WorkOrderStatus;
  technicianId?: number;
  customerId?: number;
  search?: string;
  page?: number;
  size?: number;
}) => apiClient.get<PageResponse<WorkOrder>>('/work-orders', { params: filters }).then((r) => r.data);

export const fetchWorkOrder = (id: number) =>
  apiClient.get<WorkOrder>(`/work-orders/${id}`).then((r) => r.data);

export const fetchWorkOrderHistory = (id: number) =>
  apiClient.get<WorkOrderStatusHistoryEntry[]>(`/work-orders/${id}/history`).then((r) => r.data);

export const createWorkOrder = (payload: {
  title: string;
  description?: string;
  customerId?: number;
  siteId: number;
  priority: string;
  dueAt?: string;
  assignedTechnicianId?: number | null;
}) => apiClient.post<WorkOrder>('/work-orders', payload).then((r) => r.data);

export const updateWorkOrder = (id: number, payload: {
  title: string;
  description?: string;
  priority: string;
  dueAt?: string | null;
}) => apiClient.put<WorkOrder>(`/work-orders/${id}`, payload).then((r) => r.data);

export const assignWorkOrder = (id: number, technicianId: number) =>
  apiClient.patch<WorkOrder>(`/work-orders/${id}/assign`, { technicianId }).then((r) => r.data);

export const changeWorkOrderStatus = (id: number, status: WorkOrderStatus, note?: string) =>
  apiClient.patch<WorkOrder>(`/work-orders/${id}/status`, { status, note }).then((r) => r.data);

export const addPartUsage = (id: number, partId: number, quantity: number) =>
  apiClient.post<WorkOrder>(`/work-orders/${id}/parts`, { partId, quantity }).then((r) => r.data);

export const clockIn = (id: number) =>
  apiClient.post<WorkOrder>(`/work-orders/${id}/clock-in`).then((r) => r.data);

export const clockOut = (id: number, note?: string) =>
  apiClient.post<WorkOrder>(`/work-orders/${id}/clock-out`, { note }).then((r) => r.data);

// --- Parts / Inventory ---
export const fetchParts = (params?: { search?: string; lowStock?: boolean; page?: number; size?: number }) =>
  apiClient.get<PageResponse<Part>>('/parts', { params }).then((r) => r.data);

export const createPart = (payload: {
  sku: string;
  name: string;
  unitCost: number;
  quantityOnHand?: number;
  reorderThreshold?: number;
}) => apiClient.post<Part>('/parts', payload).then((r) => r.data);

export const restockPart = (id: number, quantity: number) =>
  apiClient.post<Part>(`/parts/${id}/restock`, { quantity }).then((r) => r.data);

// --- Reports ---
export const fetchDashboardSummary = () =>
  apiClient.get<DashboardSummary>('/reports/dashboard').then((r) => r.data);

export const fetchActivity = (limit = 50) =>
  apiClient.get<ActivityEntry[]>('/reports/activity', { params: { limit } }).then((r) => r.data);

// --- Notifications ---
export const fetchNotifications = (page = 0, size = 20) =>
  apiClient.get<PageResponse<AppNotification>>('/notifications', { params: { page, size } }).then((r) => r.data);

export const fetchUnreadNotificationCount = () =>
  apiClient.get<{ unreadCount: number }>('/notifications/unread-count').then((r) => r.data.unreadCount);

export const markNotificationRead = (id: number) => apiClient.patch(`/notifications/${id}/read`);

export const markAllNotificationsRead = () => apiClient.patch('/notifications/read-all');

// --- Attachments ---
export const fetchAttachments = (workOrderId: number) =>
  apiClient.get<Attachment[]>(`/work-orders/${workOrderId}/attachments`).then((r) => r.data);

export const uploadAttachment = (workOrderId: number, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return apiClient
    .post<Attachment>(`/work-orders/${workOrderId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    .then((r) => r.data);
};

export const deleteAttachment = (workOrderId: number, attachmentId: number) =>
  apiClient.delete(`/work-orders/${workOrderId}/attachments/${attachmentId}`);

export const attachmentDownloadUrl = (relativeUrl: string) => {
  const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api\/?$/, '');
  return `${base}${relativeUrl}`;
};
