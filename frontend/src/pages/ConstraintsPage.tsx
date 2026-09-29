import { useMemo, useState, type FormEvent } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { campaigns, gameSystems } from '../api/endpoints';
import type { BuildConstraintInput, ConstraintType, FieldType, GameSystem } from '../api/types';
import { useToast } from '../components/Toast';
import { EmptyState, Field, Panel } from '../components/ui';
import { useAsync } from '../hooks';

interface RuleGroup {
  title: string;
  hint?: string;
  fieldTypes: FieldType[];
  anyLabel: string;
  types: { value: ConstraintType; text: string }[];
}

const GROUPS: RuleGroup[] = [
  {
    title: 'Stat constraints',
    fieldTypes: ['NUMBER'],
    anyLabel: 'Any stat',
    types: [
      { value: 'MAX_VALUE', text: 'Max value' },
      { value: 'MIN_VALUE', text: 'Min value' },
      { value: 'MAX_TOTAL', text: 'Max total across all stats in scope' },
    ],
  },
  {
    title: 'Equipment & list constraints',
    hint: 'List fields are read one entry per line, or separated by commas.',
    fieldTypes: ['LONG_TEXT'],
    anyLabel: 'Any list field',
    types: [
      { value: 'MAX_ENTRIES', text: 'Max number of entries' },
      { value: 'MAX_REPEATS', text: 'Same entry at most N times' },
      { value: 'FORBIDDEN_TEXT', text: 'Forbidden entry' },
    ],
  },
  {
    title: 'Character info constraints',
    fieldTypes: ['TEXT'],
    anyLabel: 'Any text field',
    types: [{ value: 'FORBIDDEN_TEXT', text: 'Forbidden text' }],
  },
];

const TYPES_NEEDING_TEXT: ConstraintType[] = ['FORBIDDEN_TEXT'];

export function ConstraintsPage() {
  const id = Number(useParams().id);
  const { data, error, reload } = useAsync(() => Promise.all([campaigns.get(id), gameSystems.list()]), [id]);
  const { notify, notifyError } = useToast();

  if (error) return <p className="error">{error}</p>;
  if (!data) return null;
  const [campaign, systems] = data;
  if (!campaign.viewerIsDungeonMaster) return <Navigate to={`/campaigns/${id}`} replace />;
  const system = systems.find((s) => s.code === campaign.gameSystem)!;
  const offenders = campaign.characters.filter((c) => c.buildViolations.length);

  async function add(input: BuildConstraintInput) {
    try {
      await campaigns.addConstraint(id, input);
      notify('Rule added');
      reload();
    } catch (e) {
      notifyError(e);
    }
  }

  async function remove(constraintId: number) {
    try {
      await campaigns.removeConstraint(id, constraintId);
      reload();
    } catch (e) {
      notifyError(e);
    }
  }

  return (
    <>
      <div className="breadcrumb">
        <Link to="/">Dashboard</Link> / <Link to={`/campaigns/${id}`}>{campaign.name}</Link> / Character build constraints
      </div>

      <Panel>
        <h1>Character build constraints</h1>
        <p className="muted small">
          Characters must follow these rules to join {campaign.name} and to save their sheet while in it.
        </p>
        {campaign.buildConstraints.length ? (
          <ul className="list">
            {campaign.buildConstraints.map((rule) => (
              <li key={rule.id}>
                <span>{rule.description}</span>
                <button className="button danger small" onClick={() => remove(rule.id)}>Remove</button>
              </li>
            ))}
          </ul>
        ) : (
          <EmptyState>No rules yet. Add one below.</EmptyState>
        )}
        {offenders.length > 0 && (
          <>
            <div className="section-title">Characters currently breaking a rule</div>
            <ul className="list">
              {offenders.map((c) => (
                <li key={c.id}>
                  <span><Link to={`/characters/${c.id}`}>{c.name}</Link> <span className="muted small">({c.owner.username})</span></span>
                  <span className="small">{c.buildViolations.join('; ')}</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </Panel>

      <div className="stack mt">
        {GROUPS.map((group) => <RuleForm key={group.title} group={group} system={system} onAdd={add} />)}
      </div>
    </>
  );
}

function RuleForm({ group, system, onAdd }: { group: RuleGroup; system: GameSystem; onAdd: (input: BuildConstraintInput) => Promise<void> }) {
  const [type, setType] = useState<ConstraintType>(group.types[0].value);
  const [scope, setScope] = useState('any');
  const [limit, setLimit] = useState('');
  const [text, setText] = useState('');
  const needsText = TYPES_NEEDING_TEXT.includes(type);

  const sections = useMemo(
    () => system.sections
      .map((section) => ({ ...section, fields: section.fields.filter((f) => group.fieldTypes.includes(f.type)) }))
      .filter((section) => section.fields.length),
    [system, group],
  );

  async function submit(event: FormEvent) {
    event.preventDefault();
    await onAdd({
      type,
      section: scope.startsWith('section:') ? scope.slice('section:'.length) : null,
      fieldKey: scope.startsWith('field:') ? scope.slice('field:'.length) : null,
      limit: needsText || limit === '' ? null : Number(limit),
      text: needsText ? text : null,
    });
    setLimit('');
    setText('');
  }

  return (
    <Panel title={group.title}>
      {group.hint && <p className="muted small">{group.hint}</p>}
      <form className="stack" onSubmit={submit}>
        <div className="grid-2">
          <Field label="Rule">
            <select value={type} onChange={(e) => setType(e.target.value as ConstraintType)}>
              {group.types.map((t) => <option key={t.value} value={t.value}>{t.text}</option>)}
            </select>
          </Field>
          <Field label="Applies to">
            <select value={scope} onChange={(e) => setScope(e.target.value)}>
              <option value="any">{group.anyLabel}</option>
              {sections.length > 1 && (
                <optgroup label="Whole section">
                  {sections.map((s) => <option key={s.name} value={`section:${s.name}`}>All {s.name} fields</option>)}
                </optgroup>
              )}
              {sections.map((s) => (
                <optgroup key={s.name} label={s.name}>
                  {s.fields.map((f) => <option key={f.key} value={`field:${f.key}`}>{f.label}</option>)}
                </optgroup>
              ))}
            </select>
          </Field>
        </div>
        {needsText ? (
          <Field label="Text"><input type="text" value={text} onChange={(e) => setText(e.target.value)} required maxLength={120} /></Field>
        ) : (
          <Field label="Limit"><input type="number" min={0} value={limit} onChange={(e) => setLimit(e.target.value)} required /></Field>
        )}
        <div className="actions end"><button type="submit" className="button small">Add rule</button></div>
      </form>
    </Panel>
  );
}
