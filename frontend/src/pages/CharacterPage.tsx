import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { campaigns, characters, gameSystems } from '../api/endpoints';
import type { Sheet } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/Toast';
import { Badge, Field, Panel, systemClass } from '../components/ui';
import { useAsync } from '../hooks';
import { SheetFields } from '../sheet/SheetFields';

export function CharacterPage() {
  const id = Number(useParams().id);
  const { user } = useAuth();
  const { notify, notifyError } = useToast();
  const navigate = useNavigate();
  const { data, error, reload } = useAsync(
    () => Promise.all([characters.get(id), campaigns.runByMe(), campaigns.playedByMe(), gameSystems.list()]),
    [id],
  );

  const [name, setName] = useState('');
  const [sheet, setSheet] = useState<Sheet>({});
  const [campaignId, setCampaignId] = useState<number | null>(null);

  useEffect(() => {
    if (!data) return;
    const [character] = data;
    setName(character.name);
    setSheet(character.sheet);
  }, [data]);

  if (error) return <p className="error">{error}</p>;
  if (!data) return null;
  const [character, run, playing, systems] = data;
  const system = systems.find((s) => s.code === character.gameSystem)!;
  const isOwner = character.owner.id === user?.id;
  const eligible = [...run, ...playing].filter((c) => c.gameSystem === character.gameSystem);
  const selectedCampaign = campaignId ?? eligible[0]?.id ?? null;

  async function run_(action: () => Promise<unknown>, message?: string) {
    try {
      await action();
      if (message) notify(message);
      reload();
    } catch (e) {
      notifyError(e);
    }
  }

  function saveSheet(event: FormEvent) {
    event.preventDefault();
    run_(() => characters.update(id, name, sheet), 'Sheet saved');
  }

  async function deleteCharacter() {
    if (!confirm(`Delete "${character.name}"? This cannot be undone.`)) return;
    try {
      await characters.remove(id);
      notify('Character deleted');
      navigate('/');
    } catch (e) {
      notifyError(e);
    }
  }

  return (
    <>
      <div className="breadcrumb"><Link to="/">Dashboard</Link> / {character.name}</div>

      <Panel className={systemClass(character.gameSystem)}>
        <div className="panel-header">
          <div>
            <h1>{character.name}</h1>
            <div className="card-meta">
              <Badge tone="system">{character.gameSystemName}</Badge>
              {character.campaign ? (
                <Badge tone="ok">In campaign: <Link to={`/campaigns/${character.campaign.id}`}>{character.campaign.name}</Link></Badge>
              ) : (
                <Badge>Not in a campaign</Badge>
              )}
              <span className="muted small">Played by {character.owner.username}</span>
            </div>
          </div>
          {isOwner && <button className="button danger small" onClick={deleteCharacter}>Delete</button>}
        </div>

        {isOwner && (
          <div className="actions" style={{ marginTop: '0.5rem' }}>
            {character.campaign ? (
              <button className="button ghost small" onClick={() => run_(() => characters.leaveCampaign(id), 'Left campaign')}>
                Leave campaign
              </button>
            ) : eligible.length ? (
              <form
                className="actions"
                onSubmit={(e) => { e.preventDefault(); if (selectedCampaign) run_(() => characters.joinCampaign(id, selectedCampaign), 'Joined campaign'); }}
              >
                <select value={selectedCampaign ?? ''} onChange={(e) => setCampaignId(Number(e.target.value))}>
                  {eligible.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
                <button type="submit" className="button small">Join campaign</button>
              </form>
            ) : (
              <span className="muted small">No {character.gameSystemName} campaigns available to join yet.</span>
            )}
          </div>
        )}
      </Panel>

      {character.campaign && (
        <Panel title="Campaign build rules" className="mt">
          {character.buildViolations.length ? (
            <div className="callout warn">
              <strong>This character currently breaks {character.buildViolations.length} rule{character.buildViolations.length === 1 ? '' : 's'}:</strong>
              <ul>{character.buildViolations.map((v) => <li key={v}>{v}</li>)}</ul>
            </div>
          ) : (
            <div className="callout ok">This character follows all of the campaign rules.</div>
          )}
          <p className="muted small">
            Rules are set by the game master on the <Link to={`/campaigns/${character.campaign.id}`}>campaign page</Link>.
            Saving a sheet that breaks them is rejected.
          </p>
        </Panel>
      )}

      <Panel title="Character sheet" className="mt">
        <form className="stack" onSubmit={saveSheet}>
          <Field label="Name">
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} required maxLength={120} disabled={!isOwner} />
          </Field>
          <SheetFields system={system} sheet={sheet} onChange={setSheet} disabled={!isOwner} />
          {isOwner && (
            <div className="actions end mt">
              <button type="submit" className="button">Save sheet</button>
            </div>
          )}
        </form>
      </Panel>
    </>
  );
}
