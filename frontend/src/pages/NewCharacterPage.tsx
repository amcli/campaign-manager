import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { characters, gameSystems } from '../api/endpoints';
import type { GameSystemCode, Sheet } from '../api/types';
import { useToast } from '../components/Toast';
import { Field, Panel } from '../components/ui';
import { useAsync } from '../hooks';
import { SectionNav } from '../sheet/SectionNav';
import { SheetFields } from '../sheet/SheetFields';

export function NewCharacterPage() {
  const { data: systems } = useAsync(gameSystems.list, []);
  const { notify, notifyError } = useToast();
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [gameSystem, setGameSystem] = useState<GameSystemCode>('DND_5E');
  const [sheet, setSheet] = useState<Sheet>({});
  const [busy, setBusy] = useState(false);

  if (!systems) return null;
  const system = systems.find((s) => s.code === gameSystem)!;

  function changeSystem(code: GameSystemCode) {
    setGameSystem(code);
    setSheet({});
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      const character = await characters.create(name, gameSystem, sheet);
      notify('Character created');
      navigate(`/characters/${character.id}`);
    } catch (error) {
      notifyError(error);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="breadcrumb"><Link to="/">Dashboard</Link> / New character</div>
      <div className="sheet-layout">
        <Panel>
          <h1>New character</h1>
          <form className="stack" onSubmit={submit}>
            <div className="grid-2">
              <Field label="Name">
                <input type="text" value={name} onChange={(e) => setName(e.target.value)} required maxLength={120} />
              </Field>
              <Field label="Game system">
                <select value={gameSystem} onChange={(e) => changeSystem(e.target.value as GameSystemCode)}>
                  {systems.map((s) => <option key={s.code} value={s.code}>{s.name}</option>)}
                </select>
              </Field>
            </div>
            <SheetFields system={system} sheet={sheet} onChange={setSheet} />
            <div className="actions end mt">
              <Link className="button ghost" to="/">Cancel</Link>
              <button type="submit" className="button" disabled={busy}>Create character</button>
            </div>
          </form>
        </Panel>
        <SectionNav sections={system.sections} />
      </div>
    </>
  );
}
