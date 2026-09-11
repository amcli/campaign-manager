import type { ReactNode } from 'react';
import type { CampaignStatus, GameSystemCode } from '../api/types';

export function DiceIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">
      <path d="M12 2 21 7v10l-9 5-9-5V7l9-5Z" />
      <path d="M12 2v20M3 7l9 5 9-5M12 12l9 5M12 12l-9 5" opacity="0.6" />
    </svg>
  );
}

export function label(enumValue: string): string {
  if (enumValue === 'NPC') return 'NPC';
  return enumValue
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function statusTone(status: CampaignStatus): string {
  if (status === 'ACTIVE') return 'ok';
  if (status === 'ON_HOLD') return 'warn';
  return '';
}

export function systemClass(code: GameSystemCode): string {
  return `system-${code}`;
}

export function playerCountLabel(count: number, max: number | null): string {
  if (max) return `${count} / ${max} players`;
  return `${count} player${count === 1 ? '' : 's'}`;
}

export function Badge({ tone = '', title, children }: { tone?: string; title?: string; children: ReactNode }) {
  return (
    <span className={`badge ${tone}`.trim()} title={title}>
      {children}
    </span>
  );
}

export function Panel({
  title,
  actions,
  className = '',
  children,
}: {
  title?: ReactNode;
  actions?: ReactNode;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section className={`panel ${className}`.trim()}>
      {(title || actions) && (
        <div className="panel-header">
          {typeof title === 'string' ? <h2>{title}</h2> : title}
          {actions && <div className="actions">{actions}</div>}
        </div>
      )}
      {children}
    </section>
  );
}

export function EmptyState({ children }: { children: ReactNode }) {
  return (
    <div className="empty">
      <DiceIcon />
      <span>{children}</span>
    </div>
  );
}

export function Field({ label: text, children }: { label: string; children: ReactNode }) {
  return (
    <label className="field">
      <span>{text}</span>
      {children}
    </label>
  );
}
