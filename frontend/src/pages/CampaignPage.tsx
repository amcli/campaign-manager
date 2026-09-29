import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { campaigns, characters } from '../api/endpoints';
import {
  CAMPAIGN_STATUSES,
  NOTE_CATEGORIES,
  type CampaignDetail,
  type CampaignStatus,
  type CharacterOption,
  type Note,
  type NoteCategory,
  type NoteInput,
  type UserSummary,
} from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/Toast';
import { Badge, EmptyState, Field, Panel, label, playerCountLabel, statusTone, systemClass } from '../components/ui';
import { useAsync } from '../hooks';

export function CampaignPage() {
  const id = Number(useParams().id);
  const { data, error, reload } = useAsync(() => Promise.all([campaigns.get(id), characters.mine()]), [id]);
  const { user } = useAuth();
  const { notify, notifyError } = useToast();
  const navigate = useNavigate();
  const [editing, setEditing] = useState(false);

  if (error) return <p className="error">{error}</p>;
  if (!data) return null;
  const [campaign, myCharacters] = data;
  const isDm = campaign.viewerIsDungeonMaster;
  const isPlayerHere = !isDm && campaign.players.some((player) => player.id === user?.id);
  const joinable = myCharacters.filter((c) => !c.campaign && c.gameSystem === campaign.gameSystem);

  async function run(action: () => Promise<unknown>, successMessage?: string) {
    try {
      await action();
      if (successMessage) notify(successMessage);
      reload();
    } catch (e) {
      notifyError(e);
    }
  }

  async function deleteCampaign() {
    if (!confirm(`Delete "${campaign.name}"? Characters in it will be released, not deleted.`)) return;
    try {
      await campaigns.remove(id);
      notify('Campaign deleted');
      navigate('/');
    } catch (e) {
      notifyError(e);
    }
  }

  function removePlayer(playerId: number, username: string) {
    if (!confirm(`Remove ${username} from "${campaign.name}"? Their characters in it will be released, not deleted.`)) return;
    run(() => campaigns.removePlayer(id, playerId), `${username} removed`);
  }

  async function leaveCampaign() {
    if (!user) return;
    if (!confirm(`Leave "${campaign.name}"? Any of your characters in it will be released, not deleted.`)) return;
    try {
      await campaigns.removePlayer(id, user.id);
      notify('Left the campaign');
      navigate('/');
    } catch (e) {
      notifyError(e);
    }
  }

  return (
    <>
      <div className="breadcrumb"><Link to="/">Dashboard</Link> / {campaign.name}</div>

      <Panel className={systemClass(campaign.gameSystem)}>
        <div className="panel-header">
          <div>
            <h1>{campaign.name}</h1>
            <div className="card-meta">
              <Badge tone="system">{campaign.gameSystemName}</Badge>
              <Badge tone={statusTone(campaign.status)}>{label(campaign.status)}</Badge>
              <span className="muted small">{campaign.gameMasterTitle}: {campaign.dungeonMaster.username}</span>
              <span className="muted small">{playerCountLabel(campaign.players.length, campaign.maxPlayers)}</span>
            </div>
          </div>
          {isDm && (
            <div className="actions">
              <button className="button ghost small" onClick={() => setEditing((v) => !v)}>{editing ? 'Cancel' : 'Edit'}</button>
              <button className="button danger small" onClick={deleteCampaign}>Delete</button>
            </div>
          )}
          {isPlayerHere && (
            <div className="actions">
              <button className="button danger small" onClick={leaveCampaign}>Leave campaign</button>
            </div>
          )}
        </div>
        {editing ? (
          <EditCampaignForm
            campaign={campaign}
            onSaved={() => { setEditing(false); run(async () => undefined, 'Campaign saved'); }}
          />
        ) : (
          <p className="note-body">{campaign.description || <span className="muted">No description yet.</span>}</p>
        )}
      </Panel>

      <div className="grid-2 mt">
        <Panel title="Players">
          <ul className="list">
            {campaign.players.length ? campaign.players.map((player) => (
              <li key={player.id}>
                <span>{player.username}</span>
                {isDm && (
                  <button className="button danger small" onClick={() => removePlayer(player.id, player.username)}>Remove</button>
                )}
              </li>
            )) : <li className="muted">No players yet.</li>}
          </ul>
          {isDm && <AddPlayerForm onAdd={(username) => run(() => campaigns.addPlayer(id, username), `${username} added`)} />}

          <h2 className="mt">Characters in this campaign</h2>
          <ul className="list">
            {campaign.characters.length ? campaign.characters.map((c) => (
              <li key={c.id}>
                <span className="actions">
                  <Link to={`/characters/${c.id}`}>{c.name}</Link>
                  {c.buildViolations.length > 0 && (
                    <Badge tone="warn" title={c.buildViolations.join('\n')}>
                      Breaks {c.buildViolations.length} rule{c.buildViolations.length === 1 ? '' : 's'}
                    </Badge>
                  )}
                </span>
                <span className="muted small">played by {c.owner.username}</span>
              </li>
            )) : <li className="muted">No characters have joined yet.</li>}
          </ul>
          {joinable.length > 0 && (
            <JoinForm
              options={joinable.map((c) => ({ id: c.id, name: c.name }))}
              onJoin={(characterId) => run(() => characters.joinCampaign(characterId, id), 'Character joined the campaign')}
            />
          )}

          {isDm && (
            <>
              <h2 className="mt">Invite a player's character</h2>
              <p className="muted small">The character only joins once its owner accepts. Invites never expire on their own.</p>
              <InviteCharacterForm
                players={campaign.players}
                campaignId={id}
                onInvited={() => run(async () => undefined, 'Invite sent')}
              />
              {campaign.pendingInvites.length > 0 && (
                <>
                  <div className="section-title">Pending invites</div>
                  <ul className="list">
                    {campaign.pendingInvites.map((invite) => (
                      <li key={invite.id}>
                        <span>{invite.characterName} <span className="muted small">→ {invite.player.username}</span></span>
                        <button className="button ghost small" onClick={() => run(() => campaigns.cancelInvite(id, invite.id))}>Cancel</button>
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </>
          )}
        </Panel>

        <div className="stack">
          <Panel
            title="Character build constraints"
            actions={isDm && <Link className="button small" to={`/campaigns/${id}/constraints`}>Manage</Link>}
          >
            {campaign.buildConstraints.length ? (
              <ul className="list">
                {campaign.buildConstraints.map((rule) => <li key={rule.id}><span>{rule.description}</span></li>)}
              </ul>
            ) : (
              <EmptyState>No build constraints. Any legal character can join.</EmptyState>
            )}
          </Panel>

          <Panel title={isDm ? 'Planning notes' : 'Notes shared with players'}>
            <div className="stack">
              {campaign.notes.length ? campaign.notes.map((note) => (
                <NoteCard
                  key={note.id}
                  note={note}
                  isDm={isDm}
                  onSave={(input) => run(() => campaigns.updateNote(id, note.id, input))}
                  onDelete={() => { if (confirm('Delete this note?')) run(() => campaigns.removeNote(id, note.id)); }}
                />
              )) : <EmptyState>No notes yet.</EmptyState>}
            </div>
            {isDm && (
              <>
                <div className="section-title">Add a note</div>
                <NoteForm onSubmit={(input) => run(() => campaigns.addNote(id, input), 'Note added')} />
              </>
            )}
          </Panel>
        </div>
      </div>
    </>
  );
}

function EditCampaignForm({ campaign, onSaved }: { campaign: CampaignDetail; onSaved: () => void }) {
  const { notifyError } = useToast();
  const [name, setName] = useState(campaign.name);
  const [status, setStatus] = useState<CampaignStatus>(campaign.status);
  const [maxPlayers, setMaxPlayers] = useState(campaign.maxPlayers?.toString() ?? '');
  const [description, setDescription] = useState(campaign.description ?? '');

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      await campaigns.update(campaign.id, { name, description, status, maxPlayers: maxPlayers ? Number(maxPlayers) : null });
      onSaved();
    } catch (e) {
      notifyError(e);
    }
  }

  return (
    <form className="stack" onSubmit={submit}>
      <Field label="Name"><input type="text" value={name} onChange={(e) => setName(e.target.value)} required maxLength={120} /></Field>
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
      <Field label="Description"><textarea value={description} onChange={(e) => setDescription(e.target.value)} maxLength={5000} /></Field>
      <div className="actions end"><button type="submit" className="button">Save</button></div>
    </form>
  );
}

function AddPlayerForm({ onAdd }: { onAdd: (username: string) => void }) {
  const [username, setUsername] = useState('');
  return (
    <form className="inline-form mt" onSubmit={(e) => { e.preventDefault(); onAdd(username); setUsername(''); }}>
      <input type="text" placeholder="Player username" value={username} onChange={(e) => setUsername(e.target.value)} required />
      <button type="submit" className="button small">Add player</button>
    </form>
  );
}

function JoinForm({ options, onJoin }: { options: { id: number; name: string }[]; onJoin: (characterId: number) => void }) {
  const [characterId, setCharacterId] = useState(options[0].id);

  useEffect(() => {
    if (!options.some((o) => o.id === characterId)) {
      setCharacterId(options[0]?.id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [options]);

  return (
    <form className="inline-form mt" onSubmit={(e) => { e.preventDefault(); onJoin(characterId); }}>
      <select value={characterId} onChange={(e) => setCharacterId(Number(e.target.value))}>
        {options.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
      </select>
      <button type="submit" className="button small">Bring character in</button>
    </form>
  );
}

function InviteCharacterForm({
  players,
  campaignId,
  onInvited,
}: {
  players: UserSummary[];
  campaignId: number;
  onInvited: () => void;
}) {
  const { notifyError } = useToast();
  const [playerId, setPlayerId] = useState<number | null>(players[0]?.id ?? null);
  const [options, setOptions] = useState<CharacterOption[]>([]);
  const [characterId, setCharacterId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setPlayerId((current) => (current !== null && players.some((p) => p.id === current) ? current : players[0]?.id ?? null));
  }, [players]);

  useEffect(() => {
    if (playerId === null) {
      setOptions([]);
      setCharacterId(null);
      return;
    }
    let cancelled = false;
    setLoading(true);
    campaigns.eligibleCharacters(campaignId, playerId)
      .then((loaded) => {
        if (cancelled) return;
        setOptions(loaded);
        setCharacterId(loaded[0]?.id ?? null);
      })
      .catch((e) => !cancelled && notifyError(e))
      .finally(() => !cancelled && setLoading(false));
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [playerId, campaignId]);

  if (players.length === 0) {
    return <p className="muted small">Add a player first, then you can invite one of their characters.</p>;
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (characterId === null) return;
    campaigns.inviteCharacter(campaignId, characterId)
      .then(onInvited)
      .catch(notifyError);
  }

  return (
    <form className="stack" onSubmit={submit}>
      <div className="grid-2">
        <Field label="Player">
          <select value={playerId ?? ''} onChange={(e) => setPlayerId(Number(e.target.value))}>
            {players.map((p) => <option key={p.id} value={p.id}>{p.username}</option>)}
          </select>
        </Field>
        <Field label="Character">
          <select
            value={characterId ?? ''}
            onChange={(e) => setCharacterId(Number(e.target.value))}
            disabled={loading || options.length === 0}
          >
            {options.length
              ? options.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)
              : <option value="">{loading ? 'Loading…' : 'No eligible characters'}</option>}
          </select>
        </Field>
      </div>
      <div className="actions end">
        <button type="submit" className="button small" disabled={characterId === null}>Send invite</button>
      </div>
    </form>
  );
}

function NoteForm({ initial, onSubmit, onCancel }: { initial?: Note; onSubmit: (input: NoteInput) => void; onCancel?: () => void }) {
  const [title, setTitle] = useState(initial?.title ?? '');
  const [category, setCategory] = useState<NoteCategory>(initial?.category ?? 'SESSION_PLAN');
  const [content, setContent] = useState(initial?.content ?? '');
  const [shared, setShared] = useState(initial?.sharedWithPlayers ?? false);

  function submit(event: FormEvent) {
    event.preventDefault();
    onSubmit({ title, content, category, sharedWithPlayers: shared });
    if (!initial) {
      setTitle('');
      setContent('');
      setShared(false);
    }
  }

  return (
    <form className="stack" onSubmit={submit}>
      <Field label="Title"><input type="text" value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={120} /></Field>
      <Field label="Category">
        <select value={category} onChange={(e) => setCategory(e.target.value as NoteCategory)}>
          {NOTE_CATEGORIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}
        </select>
      </Field>
      <Field label="Content"><textarea value={content} onChange={(e) => setContent(e.target.value)} maxLength={10000} /></Field>
      <label className="check">
        <input type="checkbox" checked={shared} onChange={(e) => setShared(e.target.checked)} />
        <span>Visible to players</span>
      </label>
      <div className="actions end">
        {onCancel && <button type="button" className="button ghost small" onClick={onCancel}>Cancel</button>}
        <button type="submit" className="button small">{initial ? 'Save' : 'Add note'}</button>
      </div>
    </form>
  );
}

function NoteCard({ note, isDm, onSave, onDelete }: { note: Note; isDm: boolean; onSave: (input: NoteInput) => void; onDelete: () => void }) {
  const [editing, setEditing] = useState(false);
  if (editing) {
    return (
      <article className="note">
        <NoteForm initial={note} onSubmit={(input) => { onSave(input); setEditing(false); }} onCancel={() => setEditing(false)} />
      </article>
    );
  }
  return (
    <article className="note">
      <div className="note-header">
        <div className="actions">
          <strong>{note.title}</strong>
          <Badge>{label(note.category)}</Badge>
          {isDm && note.sharedWithPlayers && <Badge tone="ok">Shared</Badge>}
        </div>
        {isDm && (
          <div className="actions">
            <button className="link-button small" onClick={() => setEditing(true)}>Edit</button>
            <button className="link-button small" onClick={onDelete}>Delete</button>
          </div>
        )}
      </div>
      <div className="note-body">{note.content}</div>
    </article>
  );
}
