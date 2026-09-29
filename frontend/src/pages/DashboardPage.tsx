import { Link } from 'react-router-dom';
import { campaigns, characters, invites } from '../api/endpoints';
import type { CampaignSummary, CharacterSummary, IncomingInvite } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/Toast';
import { Badge, EmptyState, Panel, label, playerCountLabel, statusTone, systemClass } from '../components/ui';
import { useAsync } from '../hooks';

export function DashboardPage() {
  const { user } = useAuth();
  const { notify, notifyError } = useToast();
  const { data, error, reload } = useAsync(
    () => Promise.all([campaigns.runByMe(), campaigns.playedByMe(), characters.mine(), invites.mine()]),
    [],
  );

  if (error) return <p className="error">{error}</p>;
  if (!data) return null;
  const [run, playing, mine, myInvites] = data;
  const inPlay = mine.filter((c) => c.campaign).length;

  async function accept(invite: IncomingInvite) {
    try {
      await invites.accept(invite.id);
      notify(`${invite.characterName} joined ${invite.campaignName}`);
      reload();
    } catch (e) {
      notifyError(e);
    }
  }

  async function decline(invite: IncomingInvite) {
    try {
      await invites.decline(invite.id);
      reload();
    } catch (e) {
      notifyError(e);
    }
  }

  return (
    <>
      <div className="hero">
        <div>
          <h1>Well met, {user?.username}</h1>
          <p>Your campaigns and characters, all in one place.</p>
        </div>
        <div className="grid-3" style={{ minWidth: 360 }}>
          <StatTile value={run.length} label="Campaigns you run" />
          <StatTile value={mine.length} label="Characters" />
          <StatTile value={inPlay} label="Characters in play" />
        </div>
      </div>

      {myInvites.length > 0 && (
        <Panel title={`Campaign invites (${myInvites.length})`} className="mt">
          <ul className="list">
            {myInvites.map((invite) => (
              <li key={invite.id}>
                <span>
                  <strong>{invite.characterName}</strong> invited to{' '}
                  <Badge tone="system">{invite.gameSystemName}</Badge> campaign{' '}
                  <Link to={`/campaigns/${invite.campaignId}`}>{invite.campaignName}</Link>
                  <span className="muted small"> by {invite.dungeonMaster.username}</span>
                </span>
                <span className="actions">
                  <button className="button small" onClick={() => accept(invite)}>Accept</button>
                  <button className="button ghost small" onClick={() => decline(invite)}>Decline</button>
                </span>
              </li>
            ))}
          </ul>
        </Panel>
      )}

      <div className="grid-2 mt">
        <Panel
          title="Campaigns you run"
          actions={<Link className="button small" to="/campaigns/new">New campaign</Link>}
        >
          <div className="stack">
            {run.length ? run.map((c) => <CampaignCard key={c.id} campaign={c} />) : <EmptyState>You are not running any campaigns yet.</EmptyState>}
          </div>
          {playing.length > 0 && (
            <>
              <div className="section-title">Campaigns you play in</div>
              <div className="stack">
                {playing.map((c) => <CampaignCard key={c.id} campaign={c} />)}
              </div>
            </>
          )}
        </Panel>

        <Panel
          title="Your characters"
          actions={<Link className="button small" to="/characters/new">New character</Link>}
        >
          <div className="stack">
            {mine.length ? mine.map((c) => <CharacterCard key={c.id} character={c} />) : <EmptyState>No characters yet. Roll one up!</EmptyState>}
          </div>
        </Panel>
      </div>
    </>
  );
}

function StatTile({ value, label: text }: { value: number; label: string }) {
  return (
    <div className="stat-tile">
      <div className="value">{value}</div>
      <div className="label">{text}</div>
    </div>
  );
}

export function CampaignCard({ campaign }: { campaign: CampaignSummary }) {
  return (
    <Link className={`card ${systemClass(campaign.gameSystem)}`} to={`/campaigns/${campaign.id}`}>
      <div className="card-title">{campaign.name}</div>
      <div className="card-meta">
        <Badge tone="system">{campaign.gameSystemName}</Badge>
        <Badge tone={statusTone(campaign.status)}>{label(campaign.status)}</Badge>
        <span className="muted small">{playerCountLabel(campaign.playerCount, campaign.maxPlayers)}</span>
        <span className="muted small">{campaign.gameMasterTitle}: {campaign.dungeonMaster.username}</span>
      </div>
    </Link>
  );
}

export function CharacterCard({ character }: { character: CharacterSummary }) {
  return (
    <Link className={`card ${systemClass(character.gameSystem)}`} to={`/characters/${character.id}`}>
      <div className="card-title">{character.name}</div>
      <div className="card-meta">
        <Badge tone="system">{character.gameSystemName}</Badge>
        {character.campaign ? (
          <Badge tone="ok">In campaign: {character.campaign.name}</Badge>
        ) : (
          <Badge>Not in a campaign</Badge>
        )}
      </div>
    </Link>
  );
}
