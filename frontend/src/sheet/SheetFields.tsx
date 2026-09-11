import type { GameSystem, Sheet, SheetField } from '../api/types';

interface Props {
  system: GameSystem;
  sheet: Sheet;
  onChange: (sheet: Sheet) => void;
  disabled?: boolean;
}

export function SheetFields({ system, sheet, onChange, disabled = false }: Props) {
  function set(key: string, value: string) {
    const next = { ...sheet };
    if (value === '') {
      delete next[key];
    } else {
      next[key] = value;
    }
    onChange(next);
  }

  return (
    <>
      {system.sections.map((section) => (
        <div key={section.name}>
          <div className="section-title">{section.name}</div>
          <div className="field-grid">
            {section.fields.map((field) => (
              <SheetInput
                key={field.key}
                field={field}
                value={sheet[field.key] ?? ''}
                onChange={(value) => set(field.key, value)}
                disabled={disabled}
              />
            ))}
          </div>
        </div>
      ))}
    </>
  );
}

function SheetInput({
  field,
  value,
  onChange,
  disabled,
}: {
  field: SheetField;
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
}) {
  switch (field.type) {
    case 'NUMBER':
      return (
        <label className="field">
          <span>{field.label}</span>
          <input type="number" value={value} disabled={disabled} onChange={(e) => onChange(e.target.value)} />
        </label>
      );
    case 'BOOLEAN':
      return (
        <label className="field check">
          <input
            type="checkbox"
            checked={value === 'true'}
            disabled={disabled}
            onChange={(e) => onChange(e.target.checked ? 'true' : '')}
          />
          <span style={{ textTransform: 'none', margin: 0 }}>{field.label}</span>
        </label>
      );
    case 'LONG_TEXT':
      return (
        <label className="field wide">
          <span>{field.label}</span>
          <textarea value={value} disabled={disabled} onChange={(e) => onChange(e.target.value)} />
        </label>
      );
    default:
      return (
        <label className="field">
          <span>{field.label}</span>
          <input type="text" value={value} disabled={disabled} onChange={(e) => onChange(e.target.value)} />
        </label>
      );
  }
}
