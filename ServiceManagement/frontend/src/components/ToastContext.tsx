import React, { createContext, useContext, useMemo, useState } from 'react';

type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastMessage {
  id: number;
  type: ToastType;
  title: string;
  message: string;
}

interface ToastContextValue {
  showToast: (toast: {
    type?: ToastType;
    title: string;
    message: string;
    duration?: number;
  }) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const removeToast = (id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  };

  const showToast = ({ type = 'info', title, message, duration = 5000 }: {
    type?: ToastType;
    title: string;
    message: string;
    duration?: number;
  }) => {
    const id = Date.now() + Math.floor(Math.random() * 1000);
    setToasts((current) => [{ id, type, title, message }, ...current]);
    window.setTimeout(() => removeToast(id), duration);
  };

  const value = useMemo(() => ({ showToast }), []);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" aria-live="polite" aria-atomic="true">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast-card toast-${toast.type}`}>
            <div className="toast-header">
              <strong>{toast.title}</strong>
              <button type="button" className="toast-close" onClick={() => removeToast(toast.id)} aria-label="Dismiss notification">
                ×
              </button>
            </div>
            <div className="toast-body">{toast.message}</div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used within a ToastProvider');
  return context;
}
