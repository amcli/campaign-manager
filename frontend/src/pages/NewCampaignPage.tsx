import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { campaigns, gameSystems } from '../api/endpoints';
import { CAMPAIGN_STATUSES, type CampaignStatus, type GameSystemCode } from '../api/types';
import { useToast } from '../components/Toast';
import { Field, Panel, label } from '../components/ui';
import { useAsync } from '../hooks';

export function NewCampaignPage() {
  const { data: systems } = useAsync(gameSystems.list, []);
  const { notify, notifyError } = useToast();
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [gameSystem, setGameSystem] = useState<GameSystemCode>('DND_5E');
  const [status, setStatus] = useState<CampaignStatus>('PLANNING');
  const [maxPlayers, setMaxPlayers] = useState('');
  const [description, setDescription] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      const campaign = await campaigns.create({
        name,
        description,
        gameSystem,
        status,
        maxPlayers: maxPlayers ? Number(maxPlayers) : null,
      });
      notify('Campaign created');
      navigate(`/campaigns/${campaign.id}`);
    } catch (error) {
      notifyError(error);
    } finally {
      setBusy(false);
    }
  }

  if (!systems) return null;

  return (
    <>
      <div className="breadcrumb"><Link to="/">Dashboard</Link> / New campaign</div>
      <Panel>
        <h1>New campaign</h1>
        <form className="stack" onSubmit={submit}>
          <Field label="Name">
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} required maxLength={120} />
          </Field>
          <Field label="Game system">
            <select value={gameSystem} onChange={(e) => setGameSystem(e.target.value as GameSystemCode)}>
              {systems.map((s) => <option key={s.code} value={s.code}>{s.name}</option>)}
            </select>
          </Field>
          <div className="grid-2">
            <Field label="Status">
              <select value={status} onChange={(e) => setStatus(e.target.value as CampaignStatus)}>
                {CAMPAIGN_STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
              </select>
            </Field>
            <Field label="Max players (empty for no limit)">
              <input type="number" min={1} value={maxPlayers} onChange={(e) => setMaxPlayers(e.target.value)} />
            </Field>
          </div>
          <Field label="Description">
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} maxLength={5000} />
          </Field>
          <div className="actions end">
            <Link className="button ghost" to="/">Cancel</Link>
            <button type="submit" className="button" disabled={busy}>Create campaign</button>
          </div>
        </form>
      </Panel>
    </>
  );
}
