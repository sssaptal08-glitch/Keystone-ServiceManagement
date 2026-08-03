import React, { useEffect, useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { StatusBadge, PriorityBadge } from '../components/Badges';
import {
  fetchWorkOrder,
  fetchWorkOrderHistory,
  fetchTechnicians,
  fetchParts,
  assignWorkOrder,
  changeWorkOrderStatus,
  updateWorkOrder,
  addPartUsage,
  clockIn,
  clockOut,
  fetchAttachments,
  uploadAttachment,
  deleteAttachment,
  attachmentDownloadUrl
} from '../api/services';
import type { Attachment, Part, TechnicianWorkload, WorkOrder, WorkOrderStatus, WorkOrderStatusHistoryEntry } from '../types';
import { extractErrorMessage } from '../api/client';
import { Paperclip, Download, Trash2, UploadCloud, Pencil } from 'lucide-react';

const NEXT_STATUS_OPTIONS: Record<WorkOrderStatus, WorkOrderStatus[]> = {
  NEW: ['ASSIGNED', 'CANCELLED'],
  ASSIGNED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['ON_HOLD', 'COMPLETED', 'CANCELLED'],
  ON_HOLD: ['IN_PROGRESS', 'CANCELLED'],
  COMPLETED: ['CLOSED', 'IN_PROGRESS'],
  CLOSED: [],
  CANCELLED: []
};

const IMMUTABLE_STATUSES: WorkOrderStatus[] = ['CLOSED', 'CANCELLED'];

export default function WorkOrderDetail() {
  const { id } = useParams();
  const woId = Number(id);
  const { user } = useAuth();

  const [wo, setWo] = useState<WorkOrder | null>(null);
  const [history, setHistory] = useState<WorkOrderStatusHistoryEntry[]>([]);
  const [technicians, setTechnicians] = useState<TechnicianWorkload[]>([]);
  const [parts, setParts] = useState<Part[]>([]);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [note, setNote] = useState('');
  const [clockOutNote, setClockOutNote] = useState('');
  const [editing, setEditing] = useState(false);

  const canManage = user?.role === 'DISPATCHER' || user?.role === 'MANAGER';
  const isTechnician = user?.role === 'TECHNICIAN';
  const canEdit = canManage && wo != null && !IMMUTABLE_STATUSES.includes(wo.status);

  const load = () => {
    fetchWorkOrder(woId).then(setWo).catch(() => {});
    fetchWorkOrderHistory(woId).then(setHistory).catch(() => {});
    fetchAttachments(woId).then(setAttachments).catch(() => {});
  };

  useEffect(load, [woId]);

  useEffect(() => {
    if (canManage) fetchTechnicians().then(setTechnicians).catch(() => {});
    if (canManage || isTechnician) fetchParts().then((res) => setParts(res.content)).catch(() => {});
  }, [canManage, isTechnician]);

  const runAction = async (fn: () => Promise<WorkOrder>) => {
    setError(null);
    try {
      const updated = await fn();
      setWo(updated);
      load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  if (!wo) return <p>Loading...</p>;

  const nextStatuses = NEXT_STATUS_OPTIONS[wo.status] ?? [];

  return (
    <div>
      <div className="page-header">
        <h1>{wo.code} — {wo.title}</h1>
        <div>
          <StatusBadge status={wo.status} /> <PriorityBadge priority={wo.priority} />
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {wo.overdue && <div className="alert alert-warning">This work order is past its SLA due date.</div>}
      {!wo.overdue && wo.atRisk && <div className="alert alert-warning">This work order is approaching its SLA due date.</div>}

      <div className="panel-grid">
        <div className="panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>Details</h3>
            {canEdit && (
              <button className="btn btn-ghost" onClick={() => setEditing((e) => !e)} title="Edit">
                <Pencil size={13} /> {editing ? 'Cancel' : 'Edit'}
              </button>
            )}
          </div>

          {editing ? (
            <EditWorkOrderForm
              wo={wo}
              onSaved={(updated) => { setWo(updated); setEditing(false); load(); }}
              onError={setError}
            />
          ) : (
            <>
              <p>{wo.description || 'No description provided.'}</p>
              <dl className="detail-list">
                <dt>Customer</dt><dd>{wo.customerName}</dd>
                <dt>Site</dt><dd>{wo.siteName}</dd>
                <dt>Technician</dt><dd>{wo.assignedTechnicianName ?? 'Unassigned'}</dd>
                <dt>Due</dt><dd>{wo.dueAt ? new Date(wo.dueAt).toLocaleString() : '—'}</dd>
                <dt>Started</dt><dd>{wo.startedAt ? new Date(wo.startedAt).toLocaleString() : '—'}</dd>
                <dt>Completed</dt><dd>{wo.completedAt ? new Date(wo.completedAt).toLocaleString() : '—'}</dd>
                <dt>Parts cost</dt><dd>${wo.totalPartsCost.toFixed(2)}</dd>
                <dt>Labour logged</dt><dd>{wo.totalMinutesLogged} min</dd>
              </dl>
            </>
          )}
        </div>

        <div className="panel">
          <h3>Actions</h3>

          {canManage && (
            <div className="action-block">
              <label>
                Assign technician
                <select
                  defaultValue=""
                  onChange={(e) => e.target.value && runAction(() => assignWorkOrder(wo.id, Number(e.target.value)))}
                >
                  <option value="">Choose technician…</option>
                  {technicians.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} ({t.openJobCount} open {t.openJobCount === 1 ? 'job' : 'jobs'})
                    </option>
                  ))}
                </select>
              </label>
            </div>
          )}

          {(canManage || isTechnician) && nextStatuses.length > 0 && (
            <div className="action-block">
              <label>
                Add a note (optional)
                <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="What changed?" />
              </label>
              <div className="button-row">
                {nextStatuses.map((s) => (
                  <button
                    key={s}
                    className="btn btn-secondary"
                    onClick={() => runAction(() => changeWorkOrderStatus(wo.id, s, note || undefined))}
                  >
                    Mark {s.replace('_', ' ')}
                  </button>
                ))}
              </div>
            </div>
          )}

          {isTechnician && (
            <div className="action-block">
              <label>
                Clock-out note (optional)
                <input value={clockOutNote} onChange={(e) => setClockOutNote(e.target.value)} placeholder="What did you do?" />
              </label>
              <div className="button-row">
                <button className="btn btn-secondary" onClick={() => runAction(() => clockIn(wo.id))}>Clock In</button>
                <button className="btn btn-secondary" onClick={() => runAction(() => clockOut(wo.id, clockOutNote || undefined))}>Clock Out</button>
              </div>
            </div>
          )}

          {(canManage || isTechnician) && (
            <PartUsageForm parts={parts} onUse={(partId, qty) => runAction(() => addPartUsage(wo.id, partId, qty))} />
          )}
        </div>
      </div>

      <h3>Status History</h3>
      <table className="table">
        <thead>
          <tr><th>When</th><th>From</th><th>To</th><th>By</th><th>Note</th></tr>
        </thead>
        <tbody>
          {history.map((h) => (
            <tr key={h.id}>
              <td>{new Date(h.changedAt).toLocaleString()}</td>
              <td>{h.fromStatus ?? '—'}</td>
              <td>{h.toStatus}</td>
              <td>{h.changedByName}</td>
              <td>{h.note ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {(canManage || isTechnician) && (
        <AttachmentsPanel
          workOrderId={wo.id}
          attachments={attachments}
          canDelete={canManage}
          onChanged={load}
          onError={setError}
        />
      )}
    </div>
  );
}

function EditWorkOrderForm({ wo, onSaved, onError }: { wo: WorkOrder; onSaved: (wo: WorkOrder) => void; onError: (m: string) => void }) {
  const [title, setTitle] = useState(wo.title);
  const [description, setDescription] = useState(wo.description ?? '');
  const [priority, setPriority] = useState(wo.priority);
  const [dueAt, setDueAt] = useState(wo.dueAt ? wo.dueAt.slice(0, 16) : '');
  const [saving, setSaving] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const updated = await updateWorkOrder(wo.id, {
        title,
        description,
        priority,
        dueAt: dueAt ? new Date(dueAt).toISOString() : null
      });
      onSaved(updated);
    } catch (err) {
      onError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={submit} className="form-panel">
      <label>Title<input value={title} onChange={(e) => setTitle(e.target.value)} required /></label>
      <label style={{ marginTop: '0.6rem' }}>Description<textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} /></label>
      <div className="form-grid" style={{ marginTop: '0.6rem' }}>
        <label>
          Priority
          <select value={priority} onChange={(e) => setPriority(e.target.value as WorkOrder['priority'])}>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </label>
        <label>Due date/time<input type="datetime-local" value={dueAt} onChange={(e) => setDueAt(e.target.value)} /></label>
      </div>
      <button className="btn btn-primary" type="submit" disabled={saving} style={{ marginTop: '0.6rem' }}>
        {saving ? 'Saving…' : 'Save Changes'}
      </button>
    </form>
  );
}

function AttachmentsPanel({
  workOrderId, attachments, canDelete, onChanged, onError
}: {
  workOrderId: number;
  attachments: Attachment[];
  canDelete: boolean;
  onChanged: () => void;
  onError: (msg: string) => void;
}) {
  const [uploading, setUploading] = useState(false);
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const doUpload = async (file: File) => {
    setUploading(true);
    try {
      await uploadAttachment(workOrderId, file);
      onChanged();
    } catch (err) {
      onError(extractErrorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file) doUpload(file);
  };

  const handleDelete = async (attachmentId: number) => {
    try {
      await deleteAttachment(workOrderId, attachmentId);
      onChanged();
    } catch (err) {
      onError(extractErrorMessage(err));
    }
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="panel">
      <h3><Paperclip size={13} style={{ verticalAlign: -2, marginRight: 4 }} />Attachments</h3>

      {attachments.length > 0 && (
        <div className="attachment-list">
          {attachments.map((a) => (
            <div className="attachment-item" key={a.id}>
              <div>
                <a href={attachmentDownloadUrl(a.downloadUrl)} target="_blank" rel="noreferrer">
                  <Download size={13} style={{ verticalAlign: -2, marginRight: 4 }} />
                  {a.originalFilename}
                </a>
                <div className="attachment-meta">
                  {formatSize(a.fileSize)} · uploaded by {a.uploadedByName} · {new Date(a.uploadedAt).toLocaleString()}
                </div>
              </div>
              {canDelete && (
                <button className="btn btn-ghost" style={{ color: '#dc2626', borderColor: '#dc2626' }} onClick={() => handleDelete(a.id)} title="Delete attachment">
                  <Trash2 size={14} />
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      <div
        className={`upload-dropzone ${dragging ? 'dragging' : ''}`}
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
      >
        <UploadCloud size={20} style={{ marginBottom: 4 }} />
        <div>{uploading ? 'Uploading…' : 'Click or drag a photo/document here to attach it to this work order'}</div>
        <div style={{ fontSize: '0.72rem', marginTop: 2 }}>Max 15MB</div>
        <input
          ref={inputRef}
          type="file"
          hidden
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) doUpload(file);
            e.target.value = '';
          }}
        />
      </div>
    </div>
  );
}

function PartUsageForm({ parts, onUse }: { parts: Part[]; onUse: (partId: number, qty: number) => void }) {
  const [partId, setPartId] = useState<number | ''>('');
  const [qty, setQty] = useState(1);

  return (
    <div className="action-block">
      <label>
        Log part usage
        <select value={partId} onChange={(e) => setPartId(e.target.value ? Number(e.target.value) : '')}>
          <option value="">Choose part…</option>
          {parts.map((p) => (
            <option key={p.id} value={p.id}>{p.sku} — {p.name} ({p.quantityOnHand} on hand)</option>
          ))}
        </select>
      </label>
      <div className="button-row">
        <input
          type="number"
          min={1}
          value={qty}
          onChange={(e) => setQty(Number(e.target.value))}
          style={{ width: '80px' }}
        />
        <button
          className="btn btn-secondary"
          disabled={!partId}
          onClick={() => partId && onUse(Number(partId), qty)}
        >
          Log Usage
        </button>
      </div>
    </div>
  );
}
