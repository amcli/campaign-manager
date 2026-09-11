import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react';
import { errorMessage } from '../hooks';

interface ToastApi {
  notify: (message: string) => void;
  notifyError: (error: unknown) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<{ message: string; isError: boolean } | null>(null);
  const timer = useRef<number | undefined>(undefined);

  const show = useCallback((message: string, isError: boolean) => {
    setToast({ message, isError });
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setToast(null), 4000);
  }, []);

  const api = useMemo<ToastApi>(
    () => ({
      notify: (message) => show(message, false),
      notifyError: (error) => show(errorMessage(error), true),
    }),
    [show],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      {toast && <div className={`toast${toast.isError ? ' error' : ''}`}>{toast.message}</div>}
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used inside ToastProvider');
  }
  return context;
}
