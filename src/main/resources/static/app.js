(() => {
  'use strict';

  const view = document.getElementById('view');
  const topnav = document.getElementById('topnav');
  const currentUserLabel = document.getElementById('current-user');
  const toastBox = document.getElementById('toast');

  const state = { user: null, systems: null };

  // ---------- API ----------

  class ApiError extends Error {
    constructor(message, status, fieldErrors) {
      super(message);
      this.status = status;
      this.fieldErrors = fieldErrors || {};
    }
  }

  function csrfToken() {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
  }

  async function api(method, path, body) {
    const response = await fetch(path, {
      method,
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': csrfToken(),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });

    if (response.status === 204) return null;

    const data = await response.json().catch(() => null);
    if (!response.ok) {
      if (response.status === 401 && !path.startsWith('/api/auth/')) {
        state.user = null;
        navigate('#/login');
      }
      throw new ApiError((data && data.message) || response.statusText, response.status, data && data.fieldErrors);
    }
    return data;
  }

  async function loadSystems() {
    if (!state.systems) {
      state.systems = await api('GET', '/api/game-systems');
    }
    return state.systems;
  }

  function systemByCode(code) {
    return state.systems.find((system) => system.code === code);
  }

  // ---------- helpers ----------

  function esc(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  const FIXED_LABELS = { NPC: 'NPC' };

  function label(enumValue) {
    if (FIXED_LABELS[enumValue]) return FIXED_LABELS[enumValue];
    return String(enumValue)
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  function statusBadge(status) {
    const tone = status === 'ACTIVE' ? 'ok' : status === 'ON_HOLD' ? 'warn' : '';
    return `<span class="badge ${tone}">${esc(label(status))}</span>`;
  }

  function options(items, selected) {
    return items
      .map((item) => `<option value="${esc(item.value)}" ${item.value === selected ? 'selected' : ''}>${esc(item.text)}</option>`)
      .join('');
  }

  function enumOptions(values, selected) {
    return options(values.map((value) => ({ value, text: label(value) })), selected);
  }

  function formValues(form) {
    const values = {};
    new FormData(form).forEach((value, key) => { values[key] = value; });
    return values;
  }

  let toastTimer;
  function toast(message, isError) {
    toastBox.textContent = message;
    toastBox.classList.toggle('error', Boolean(isError));
    toastBox.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { toastBox.hidden = true; }, 3500);
  }

  function showError(error) {
    const details = Object.entries(error.fieldErrors || {}).map(([field, msg]) => `${field}: ${msg}`);
    toast(details.length ? `${error.message}. ${details.join('; ')}` : error.message, true);
  }

  function navigate(hash) {
    if (location.hash === hash) {
      route();
    } else {
      location.hash = hash;
    }
  }

  function on(selector, event, handler) {
    view.querySelectorAll(selector).forEach((element) => element.addEventListener(event, handler));
  }

  async function submitting(form, action) {
    const button = form.querySelector('button[type="submit"]');
    if (button) button.disabled = true;
    try {
      await action(formValues(form), form);
    } catch (error) {
      showError(error);
    } finally {
      if (button) button.disabled = false;
    }
  }

  // ---------- character sheet fields ----------

  function renderSheetFields(system, sheet) {
    return system.sections.map((section) => `
      <div class="section-title">${esc(section.name)}</div>
      <div class="field-grid">
        ${section.fields.map((field) => renderSheetField(field, sheet[field.key])).join('')}
      </div>
    `).join('');
  }

  function renderSheetField(field, value) {
    const name = `sheet.${field.key}`;
    switch (field.type) {
      case 'NUMBER':
        return `<label><span>${esc(field.label)}</span><input type="number" name="${name}" value="${esc(value)}"></label>`;
      case 'BOOLEAN':
        return `<label class="check"><input type="checkbox" name="${name}" ${value === 'true' ? 'checked' : ''}><span>${esc(field.label)}</span></label>`;
      case 'LONG_TEXT':
        return `<label class="wide"><span>${esc(field.label)}</span><textarea name="${name}">${esc(value)}</textarea></label>`;
      default:
        return `<label><span>${esc(field.label)}</span><input type="text" name="${name}" value="${esc(value)}"></label>`;
    }
  }

  function collectSheet(form) {
    const sheet = {};
    form.querySelectorAll('[name^="sheet."]').forEach((input) => {
      const key = input.name.slice('sheet.'.length);
      if (input.type === 'checkbox') {
        if (input.checked) sheet[key] = 'true';
      } else if (input.value.trim() !== '') {
        sheet[key] = input.value.trim();
      }
    });
    return sheet;
  }

  // ---------- views: auth ----------

  function renderAuth(mode) {
    const isRegister = mode === 'register';
    view.innerHTML = `
      <div class="panel auth-box">
        <h1>${isRegister ? 'Create an account' : 'Log in'}</h1>
        <form id="auth-form" class="stack">
          <label><span>Username</span><input type="text" name="username" required autocomplete="username"></label>
          ${isRegister ? '<label><span>Email</span><input type="email" name="email" required autocomplete="email"></label>' : ''}
          <label><span>Password</span><input type="password" name="password" required minlength="${isRegister ? 8 : 1}" autocomplete="${isRegister ? 'new-password' : 'current-password'}"></label>
          <div class="actions">
            <button type="submit" class="button">${isRegister ? 'Register' : 'Log in'}</button>
            <span class="muted small">
              ${isRegister ? 'Already have an account? <a href="#/login">Log in</a>' : 'New here? <a href="#/register">Create an account</a>'}
            </span>
          </div>
        </form>
        <div id="dev-login-slot"></div>
      </div>
    `;

    on('#auth-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values) => {
        state.user = await api('POST', isRegister ? '/api/auth/register' : '/api/auth/login', values);
        updateTopnav();
        navigate('#/');
      });
    });

    if (!isRegister) {
      offerDevLogin();
    }
  }

  async function offerDevLogin() {
    let status;
    try {
      status = await api('GET', '/api/auth/dev-login-status');
    } catch (error) {
      return;
    }
    if (!status.enabled) return;

    const slot = document.getElementById('dev-login-slot');
    if (!slot) return;
    slot.innerHTML = `
      <hr style="border:none;border-top:1px solid var(--line);margin:1rem 0">
      <button id="dev-login-button" class="button ghost" style="width:100%">Continue as dev (local only)</button>
    `;

    document.getElementById('dev-login-button').addEventListener('click', async () => {
      try {
        state.user = await api('POST', '/api/auth/dev-login');
        updateTopnav();
        navigate('#/');
      } catch (error) {
        showError(error);
      }
    });
  }

  // ---------- views: dashboard ----------

  async function renderDashboard() {
    const [runCampaigns, playingCampaigns, characters] = await Promise.all([
      api('GET', '/api/campaigns/dm'),
      api('GET', '/api/campaigns/playing'),
      api('GET', '/api/characters'),
    ]);

    view.innerHTML = `
      <h1>Welcome, ${esc(state.user.username)}</h1>
      <div class="grid-2">
        <section class="panel">
          <div class="panel-header">
            <h2>Campaigns you run</h2>
            <a class="button small" href="#/campaigns/new">New campaign</a>
          </div>
          <div class="stack">
            ${runCampaigns.length ? runCampaigns.map(renderCampaignCard).join('') : '<div class="empty">You are not running any campaigns yet.</div>'}
          </div>
          ${playingCampaigns.length ? `
            <h3 class="section-title">Campaigns you play in</h3>
            <div class="stack">${playingCampaigns.map(renderCampaignCard).join('')}</div>
          ` : ''}
        </section>

        <section class="panel">
          <div class="panel-header">
            <h2>Your characters</h2>
            <a class="button small" href="#/characters/new">New character</a>
          </div>
          <div class="stack">
            ${characters.length ? characters.map(renderCharacterCard).join('') : '<div class="empty">No characters yet. Roll one up!</div>'}
          </div>
        </section>
      </div>
    `;
  }

  function renderCampaignCard(campaign) {
    return `
      <a class="card" href="#/campaigns/${campaign.id}">
        <div class="card-title">${esc(campaign.name)}</div>
        <div class="card-meta">
          <span class="badge accent">${esc(campaign.gameSystemName)}</span>
          ${statusBadge(campaign.status)}
          <span class="muted small">${playerCountLabel(campaign.playerCount, campaign.maxPlayers)}</span>
          <span class="muted small">${esc(campaign.gameMasterTitle)}: ${esc(campaign.dungeonMaster.username)}</span>
        </div>
      </a>
    `;
  }

  function playerCountLabel(count, max) {
    if (max) return `${count} / ${max} players`;
    return `${count} player${count === 1 ? '' : 's'}`;
  }

  function campaignBody(values) {
    return {
      name: values.name,
      description: values.description,
      gameSystem: values.gameSystem,
      status: values.status,
      maxPlayers: values.maxPlayers ? Number(values.maxPlayers) : null,
    };
  }

  function renderCharacterCard(character) {
    const campaignBadge = character.campaign
      ? `<span class="badge ok">In campaign: ${esc(character.campaign.name)}</span>`
      : '<span class="badge">Not in a campaign</span>';
    return `
      <a class="card" href="#/characters/${character.id}">
        <div class="card-title">${esc(character.name)}</div>
        <div class="card-meta">
          <span class="badge accent">${esc(character.gameSystemName)}</span>
          ${campaignBadge}
        </div>
      </a>
    `;
  }

  // ---------- views: campaigns ----------

  const CAMPAIGN_STATUSES = ['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED'];
  const NOTE_CATEGORIES = ['SESSION_PLAN', 'PLOT', 'NPC', 'LOCATION', 'LORE', 'OTHER'];

  async function renderNewCampaign() {
    const systems = await loadSystems();
    view.innerHTML = `
      <div class="breadcrumb"><a href="#/">Dashboard</a> / New campaign</div>
      <div class="panel">
        <h1>New campaign</h1>
        <form id="campaign-form" class="stack">
          <label><span>Name</span><input type="text" name="name" required maxlength="120"></label>
          <label><span>Game system</span>
            <select name="gameSystem">${options(systems.map((s) => ({ value: s.code, text: s.name })))}</select>
          </label>
          <label><span>Status</span><select name="status">${enumOptions(CAMPAIGN_STATUSES, 'PLANNING')}</select></label>
          <label><span>Max players (leave empty for no limit)</span><input type="number" name="maxPlayers" min="1"></label>
          <label><span>Description</span><textarea name="description" maxlength="5000"></textarea></label>
          <div class="actions end">
            <a class="button ghost" href="#/">Cancel</a>
            <button type="submit" class="button">Create campaign</button>
          </div>
        </form>
      </div>
    `;

    on('#campaign-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values) => {
        const campaign = await api('POST', '/api/campaigns', campaignBody(values));
        toast('Campaign created');
        navigate(`#/campaigns/${campaign.id}`);
      });
    });
  }

  async function renderCampaign(id) {
    const [campaign, myCharacters] = await Promise.all([
      api('GET', `/api/campaigns/${id}`),
      api('GET', '/api/characters'),
    ]);
    const isDm = campaign.viewerIsDungeonMaster;
    const joinable = myCharacters.filter((c) => !c.campaign && c.gameSystem === campaign.gameSystem);

    view.innerHTML = `
      <div class="breadcrumb"><a href="#/">Dashboard</a> / ${esc(campaign.name)}</div>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h1>${esc(campaign.name)}</h1>
            <div class="card-meta">
              <span class="badge accent">${esc(campaign.gameSystemName)}</span>
              ${statusBadge(campaign.status)}
              <span class="muted small">${esc(campaign.gameMasterTitle)}: ${esc(campaign.dungeonMaster.username)}</span>
              <span class="muted small">${playerCountLabel(campaign.players.length, campaign.maxPlayers)}</span>
            </div>
          </div>
          ${isDm ? `<div class="actions">
            <button id="edit-toggle" class="button ghost small">Edit</button>
            <button id="delete-campaign" class="button danger small">Delete</button>
          </div>` : ''}
        </div>
        <p id="description" class="note-body">${esc(campaign.description || 'No description yet.')}</p>
        ${isDm ? `
          <form id="edit-form" class="stack" hidden>
            <label><span>Name</span><input type="text" name="name" value="${esc(campaign.name)}" required maxlength="120"></label>
            <label><span>Status</span><select name="status">${enumOptions(CAMPAIGN_STATUSES, campaign.status)}</select></label>
            <label><span>Max players (leave empty for no limit)</span><input type="number" name="maxPlayers" min="1" value="${campaign.maxPlayers ?? ''}"></label>
            <label><span>Description</span><textarea name="description" maxlength="5000">${esc(campaign.description)}</textarea></label>
            <div class="actions end">
              <button type="button" id="edit-cancel" class="button ghost">Cancel</button>
              <button type="submit" class="button">Save</button>
            </div>
          </form>
        ` : ''}
      </section>

      <div class="grid-2" style="margin-top:1.5rem">
        <section class="panel">
          <h2>Players</h2>
          <ul class="list">
            ${campaign.players.length ? campaign.players.map((player) => `
              <li>
                <span>${esc(player.username)}</span>
                ${isDm ? `<button class="button danger small remove-player" data-id="${player.id}">Remove</button>` : ''}
              </li>`).join('') : '<li class="muted">No players yet.</li>'}
          </ul>
          ${isDm ? `
            <form id="add-player-form" class="actions" style="margin-top:0.75rem">
              <input type="text" name="username" placeholder="Player username" required style="flex:1">
              <button type="submit" class="button small">Add player</button>
            </form>
          ` : ''}

          <h2 style="margin-top:1.5rem">Characters in this campaign</h2>
          <ul class="list">
            ${campaign.characters.length ? campaign.characters.map((c) => `
              <li>
                <span>
                  <a href="#/characters/${c.id}">${esc(c.name)}</a>
                  ${c.buildViolations.length ? `<span class="badge warn" title="${esc(c.buildViolations.join('\n'))}">Breaks ${c.buildViolations.length} rule${c.buildViolations.length === 1 ? '' : 's'}</span>` : ''}
                </span>
                <span class="muted small">played by ${esc(c.owner.username)}</span>
              </li>`).join('') : '<li class="muted">No characters have joined yet.</li>'}
          </ul>
          ${joinable.length ? `
            <form id="join-form" class="actions" style="margin-top:0.75rem">
              <select name="characterId" style="flex:1">${options(joinable.map((c) => ({ value: String(c.id), text: c.name })))}</select>
              <button type="submit" class="button small">Bring character in</button>
            </form>
          ` : ''}
        </section>

        <section class="panel">
          <div class="panel-header">
            <h2>Character build constraints</h2>
            ${isDm ? `<a class="button small" href="#/campaigns/${id}/constraints">Manage</a>` : ''}
          </div>
          ${campaign.buildConstraints.length ? `
            <ul class="list">
              ${campaign.buildConstraints.map((rule) => `<li><span>${esc(rule.description)}</span></li>`).join('')}
            </ul>
          ` : '<div class="empty">No build constraints. Any legal character can join.</div>'}
        </section>

        <section class="panel">
          <h2>${isDm ? 'Planning notes' : 'Notes shared with players'}</h2>
          <div id="notes" class="stack">
            ${campaign.notes.length ? campaign.notes.map((note) => renderNote(note, isDm)).join('') : '<div class="empty">No notes yet.</div>'}
          </div>
          ${isDm ? `
            <h3 class="section-title">Add a note</h3>
            <form id="note-form" class="stack">
              <label><span>Title</span><input type="text" name="title" required maxlength="120"></label>
              <label><span>Category</span><select name="category">${enumOptions(NOTE_CATEGORIES, 'SESSION_PLAN')}</select></label>
              <label><span>Content</span><textarea name="content" maxlength="10000"></textarea></label>
              <label class="check"><input type="checkbox" name="sharedWithPlayers"><span>Visible to players</span></label>
              <div class="actions end"><button type="submit" class="button small">Add note</button></div>
            </form>
          ` : ''}
        </section>
      </div>
    `;

    const reload = () => renderCampaign(id);

    on('#edit-toggle', 'click', () => {
      view.querySelector('#edit-form').hidden = false;
      view.querySelector('#description').hidden = true;
    });
    on('#edit-cancel', 'click', () => {
      view.querySelector('#edit-form').hidden = true;
      view.querySelector('#description').hidden = false;
    });
    on('#edit-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values) => {
        await api('PUT', `/api/campaigns/${id}`, campaignBody(values));
        toast('Campaign saved');
        reload();
      });
    });
    on('#delete-campaign', 'click', async () => {
      if (!confirm(`Delete "${campaign.name}"? Characters in it will be released, not deleted.`)) return;
      try {
        await api('DELETE', `/api/campaigns/${id}`);
        toast('Campaign deleted');
        navigate('#/');
      } catch (error) {
        showError(error);
      }
    });
    on('#add-player-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values) => {
        await api('POST', `/api/campaigns/${id}/players`, values);
        toast(`${values.username} added`);
        reload();
      });
    });
    on('.remove-player', 'click', async (event) => {
      try {
        await api('DELETE', `/api/campaigns/${id}/players/${event.target.dataset.id}`);
        reload();
      } catch (error) {
        showError(error);
      }
    });
    on('#join-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values) => {
        await api('PUT', `/api/characters/${values.characterId}/campaign`, { campaignId: Number(id) });
        toast('Character joined the campaign');
        reload();
      });
    });
    on('#note-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values, form) => {
        await api('POST', `/api/campaigns/${id}/notes`, noteBody(values, form));
        reload();
      });
    });
    on('.note-edit', 'click', (event) => {
      const noteElement = event.target.closest('.note');
      noteElement.querySelector('.note-display').hidden = true;
      noteElement.querySelector('.note-edit-form').hidden = false;
    });
    on('.note-edit-cancel', 'click', (event) => {
      const noteElement = event.target.closest('.note');
      noteElement.querySelector('.note-display').hidden = false;
      noteElement.querySelector('.note-edit-form').hidden = true;
    });
    on('.note-edit-form', 'submit', (event) => {
      event.preventDefault();
      const noteId = event.target.dataset.id;
      submitting(event.target, async (values, form) => {
        await api('PUT', `/api/campaigns/${id}/notes/${noteId}`, noteBody(values, form));
        reload();
      });
    });
    on('.note-delete', 'click', async (event) => {
      if (!confirm('Delete this note?')) return;
      try {
        await api('DELETE', `/api/campaigns/${id}/notes/${event.target.dataset.id}`);
        reload();
      } catch (error) {
        showError(error);
      }
    });
  }

  function noteBody(values, form) {
    return {
      title: values.title,
      content: values.content,
      category: values.category,
      sharedWithPlayers: form.querySelector('[name="sharedWithPlayers"]').checked,
    };
  }

  function renderNote(note, isDm) {
    return `
      <article class="note">
        <div class="note-display">
          <div class="note-header">
            <div>
              <strong>${esc(note.title)}</strong>
              <span class="badge">${esc(label(note.category))}</span>
              ${isDm && note.sharedWithPlayers ? '<span class="badge ok">Shared</span>' : ''}
            </div>
            ${isDm ? `<div class="actions">
              <button class="link-button small note-edit">Edit</button>
              <button class="link-button small note-delete" data-id="${note.id}">Delete</button>
            </div>` : ''}
          </div>
          <div class="note-body">${esc(note.content)}</div>
        </div>
        ${isDm ? `
          <form class="note-edit-form stack" data-id="${note.id}" hidden>
            <label><span>Title</span><input type="text" name="title" value="${esc(note.title)}" required maxlength="120"></label>
            <label><span>Category</span><select name="category">${enumOptions(NOTE_CATEGORIES, note.category)}</select></label>
            <label><span>Content</span><textarea name="content" maxlength="10000">${esc(note.content)}</textarea></label>
            <label class="check"><input type="checkbox" name="sharedWithPlayers" ${note.sharedWithPlayers ? 'checked' : ''}><span>Visible to players</span></label>
            <div class="actions end">
              <button type="button" class="button ghost small note-edit-cancel">Cancel</button>
              <button type="submit" class="button small">Save</button>
            </div>
          </form>
        ` : ''}
      </article>
    `;
  }

  // ---------- views: characters ----------

  async function renderNewCharacter() {
    const systems = await loadSystems();
    view.innerHTML = `
      <div class="breadcrumb"><a href="#/">Dashboard</a> / New character</div>
      <div class="panel">
        <h1>New character</h1>
        <form id="character-form" class="stack">
          <label><span>Name</span><input type="text" name="name" required maxlength="120"></label>
          <label><span>Game system</span>
            <select name="gameSystem" id="system-select">${options(systems.map((s) => ({ value: s.code, text: s.name })))}</select>
          </label>
          <div id="sheet-fields"></div>
          <div class="actions end">
            <a class="button ghost" href="#/">Cancel</a>
            <button type="submit" class="button">Create character</button>
          </div>
        </form>
      </div>
    `;

    const select = view.querySelector('#system-select');
    const sheetContainer = view.querySelector('#sheet-fields');
    const drawFields = () => { sheetContainer.innerHTML = renderSheetFields(systemByCode(select.value), {}); };
    select.addEventListener('change', drawFields);
    drawFields();

    on('#character-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values, form) => {
        const character = await api('POST', '/api/characters', {
          name: values.name,
          gameSystem: values.gameSystem,
          sheet: collectSheet(form),
        });
        toast('Character created');
        navigate(`#/characters/${character.id}`);
      });
    });
  }

  async function renderCharacter(id) {
    const [character, runCampaigns, playingCampaigns] = await Promise.all([
      api('GET', `/api/characters/${id}`),
      api('GET', '/api/campaigns/dm'),
      api('GET', '/api/campaigns/playing'),
      loadSystems(),
    ]);
    const system = systemByCode(character.gameSystem);
    const isOwner = character.owner.id === state.user.id;
    const eligibleCampaigns = [...runCampaigns, ...playingCampaigns].filter((c) => c.gameSystem === character.gameSystem);

    view.innerHTML = `
      <div class="breadcrumb"><a href="#/">Dashboard</a> / ${esc(character.name)}</div>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h1>${esc(character.name)}</h1>
            <div class="card-meta">
              <span class="badge accent">${esc(character.gameSystemName)}</span>
              ${character.campaign
                ? `<span class="badge ok">In campaign: <a href="#/campaigns/${character.campaign.id}">${esc(character.campaign.name)}</a></span>`
                : '<span class="badge">Not in a campaign</span>'}
              <span class="muted small">Played by ${esc(character.owner.username)}</span>
            </div>
          </div>
          ${isOwner ? '<button id="delete-character" class="button danger small">Delete</button>' : ''}
        </div>

        ${isOwner ? `
          <div class="actions" style="margin-top:0.5rem">
            ${character.campaign
              ? '<button id="leave-campaign" class="button ghost small">Leave campaign</button>'
              : eligibleCampaigns.length ? `
                <form id="join-form" class="actions">
                  <select name="campaignId">${options(eligibleCampaigns.map((c) => ({ value: String(c.id), text: c.name })))}</select>
                  <button type="submit" class="button small">Join campaign</button>
                </form>`
              : `<span class="muted small">No ${esc(character.gameSystemName)} campaigns available to join yet.</span>`}
          </div>
        ` : ''}
      </section>

      ${character.campaign ? `
        <section class="panel" style="margin-top:1.5rem">
          <h2>Campaign build rules</h2>
          ${character.buildViolations.length ? `
            <div class="empty" style="border-color:var(--warn);color:var(--warn);text-align:left">
              <strong>This character currently breaks ${character.buildViolations.length} rule${character.buildViolations.length === 1 ? '' : 's'}:</strong>
              <ul style="margin:0.4rem 0 0 1.2rem;padding:0">${character.buildViolations.map((v) => `<li>${esc(v)}</li>`).join('')}</ul>
            </div>
          ` : '<p class="muted small">This character follows all of the campaign rules.</p>'}
          <p class="muted small">Rules are set by the game master on the <a href="#/campaigns/${character.campaign.id}">campaign page</a>. Saving a sheet that breaks them is rejected.</p>
        </section>
      ` : ''}

      <section class="panel" style="margin-top:1.5rem">
        <h2>Character sheet</h2>
        <form id="sheet-form" class="stack">
          <label><span>Name</span><input type="text" name="name" value="${esc(character.name)}" required maxlength="120" ${isOwner ? '' : 'readonly'}></label>
          ${renderSheetFields(system, character.sheet)}
          ${isOwner ? '<div class="actions end" style="margin-top:1rem"><button type="submit" class="button">Save sheet</button></div>' : ''}
        </form>
      </section>
    `;

    if (!isOwner) {
      view.querySelectorAll('#sheet-form input, #sheet-form textarea').forEach((input) => { input.disabled = true; });
    }

    const reload = () => renderCharacter(id);

    on('#sheet-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values, form) => {
        await api('PUT', `/api/characters/${id}`, { name: values.name, sheet: collectSheet(form) });
        toast('Sheet saved');
        reload();
      });
    });
    on('#join-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values) => {
        await api('PUT', `/api/characters/${id}/campaign`, { campaignId: Number(values.campaignId) });
        toast('Joined campaign');
        reload();
      });
    });
    on('#leave-campaign', 'click', async () => {
      try {
        await api('DELETE', `/api/characters/${id}/campaign`);
        toast('Left campaign');
        reload();
      } catch (error) {
        showError(error);
      }
    });
    on('#delete-character', 'click', async () => {
      if (!confirm(`Delete "${character.name}"? This cannot be undone.`)) return;
      try {
        await api('DELETE', `/api/characters/${id}`);
        toast('Character deleted');
        navigate('#/');
      } catch (error) {
        showError(error);
      }
    });
  }

  // ---------- views: build constraints ----------

  const CONSTRAINT_GROUPS = [
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
      fieldTypes: ['LONG_TEXT'],
      anyLabel: 'Any list field',
      hint: 'List fields are read one entry per line, or separated by commas.',
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
      types: [
        { value: 'FORBIDDEN_TEXT', text: 'Forbidden text' },
      ],
    },
  ];

  const TYPES_NEEDING_TEXT = ['FORBIDDEN_TEXT'];

  function scopeOptions(system, group) {
    const sections = system.sections
      .map((section) => ({ ...section, fields: section.fields.filter((f) => group.fieldTypes.includes(f.type)) }))
      .filter((section) => section.fields.length);
    return `
      <option value="any">${esc(group.anyLabel)}</option>
      ${sections.length > 1 ? `<optgroup label="Whole section">
        ${sections.map((s) => `<option value="section:${esc(s.name)}">All ${esc(s.name)} fields</option>`).join('')}
      </optgroup>` : ''}
      ${sections.map((s) => `<optgroup label="${esc(s.name)}">
        ${s.fields.map((f) => `<option value="field:${esc(f.key)}">${esc(f.label)}</option>`).join('')}
      </optgroup>`).join('')}
    `;
  }

  function constraintBody(values) {
    const body = { type: values.type, section: null, fieldKey: null, limit: null, text: null };
    if (values.scope.startsWith('section:')) body.section = values.scope.slice('section:'.length);
    if (values.scope.startsWith('field:')) body.fieldKey = values.scope.slice('field:'.length);
    if (TYPES_NEEDING_TEXT.includes(values.type)) {
      body.text = values.text;
    } else {
      body.limit = values.limit === '' ? null : Number(values.limit);
    }
    return body;
  }

  function renderConstraintForm(system, group, index) {
    return `
      <section class="panel">
        <h2>${esc(group.title)}</h2>
        ${group.hint ? `<p class="muted small">${esc(group.hint)}</p>` : ''}
        <form class="constraint-form stack" data-group="${index}">
          <label><span>Rule</span><select name="type">${options(group.types)}</select></label>
          <label><span>Applies to</span><select name="scope">${scopeOptions(system, group)}</select></label>
          <label class="limit-field"><span>Limit</span><input type="number" name="limit" min="0" required></label>
          <label class="text-field" hidden><span>Text</span><input type="text" name="text" maxlength="120"></label>
          <div class="actions end"><button type="submit" class="button small">Add rule</button></div>
        </form>
      </section>
    `;
  }

  async function renderConstraints(id) {
    const [campaign] = await Promise.all([api('GET', `/api/campaigns/${id}`), loadSystems()]);
    if (!campaign.viewerIsDungeonMaster) {
      navigate(`#/campaigns/${id}`);
      return;
    }
    const system = systemByCode(campaign.gameSystem);
    const offenders = campaign.characters.filter((c) => c.buildViolations.length);

    view.innerHTML = `
      <div class="breadcrumb"><a href="#/">Dashboard</a> / <a href="#/campaigns/${id}">${esc(campaign.name)}</a> / Character build constraints</div>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h1>Character build constraints</h1>
            <p class="muted small" style="margin:0">Characters must follow these rules to join ${esc(campaign.name)} and to save their sheet while in it.</p>
          </div>
        </div>
        ${campaign.buildConstraints.length ? `
          <ul class="list">
            ${campaign.buildConstraints.map((rule) => `
              <li>
                <span>${esc(rule.description)}</span>
                <button class="button danger small remove-constraint" data-id="${rule.id}">Remove</button>
              </li>`).join('')}
          </ul>
        ` : '<div class="empty">No rules yet. Add one below.</div>'}
        ${offenders.length ? `
          <h3 class="section-title">Characters currently breaking a rule</h3>
          <ul class="list">
            ${offenders.map((c) => `
              <li>
                <span><a href="#/characters/${c.id}">${esc(c.name)}</a> <span class="muted small">(${esc(c.owner.username)})</span></span>
                <span class="small">${esc(c.buildViolations.join('; '))}</span>
              </li>`).join('')}
          </ul>
        ` : ''}
      </section>

      <div class="stack" style="margin-top:1.5rem">
        ${CONSTRAINT_GROUPS.map((group, index) => renderConstraintForm(system, group, index)).join('')}
      </div>
    `;

    const reload = () => renderConstraints(id);

    view.querySelectorAll('.constraint-form').forEach((form) => {
      const typeSelect = form.querySelector('[name="type"]');
      const limitField = form.querySelector('.limit-field');
      const textField = form.querySelector('.text-field');
      const syncInputs = () => {
        const needsText = TYPES_NEEDING_TEXT.includes(typeSelect.value);
        limitField.hidden = needsText;
        textField.hidden = !needsText;
        limitField.querySelector('input').required = !needsText;
        textField.querySelector('input').required = needsText;
      };
      typeSelect.addEventListener('change', syncInputs);
      syncInputs();
    });

    on('.constraint-form', 'submit', (event) => {
      event.preventDefault();
      submitting(event.target, async (values) => {
        await api('POST', `/api/campaigns/${id}/constraints`, constraintBody(values));
        toast('Rule added');
        reload();
      });
    });
    on('.remove-constraint', 'click', async (event) => {
      try {
        await api('DELETE', `/api/campaigns/${id}/constraints/${event.target.dataset.id}`);
        reload();
      } catch (error) {
        showError(error);
      }
    });
  }

  // ---------- routing ----------

  const routes = [
    { pattern: /^#\/login$/, render: () => renderAuth('login'), publicRoute: true },
    { pattern: /^#\/register$/, render: () => renderAuth('register'), publicRoute: true },
    { pattern: /^#\/campaigns\/new$/, render: renderNewCampaign },
    { pattern: /^#\/campaigns\/(\d+)$/, render: (match) => renderCampaign(match[1]) },
    { pattern: /^#\/campaigns\/(\d+)\/constraints$/, render: (match) => renderConstraints(match[1]) },
    { pattern: /^#\/characters\/new$/, render: renderNewCharacter },
    { pattern: /^#\/characters\/(\d+)$/, render: (match) => renderCharacter(match[1]) },
    { pattern: /^#\/?$/, render: renderDashboard },
  ];

  async function route() {
    const hash = location.hash || '#/';
    const routeEntry = routes.find((entry) => entry.pattern.test(hash));
    if (!routeEntry) {
      navigate('#/');
      return;
    }
    if (!routeEntry.publicRoute && !state.user) {
      navigate('#/login');
      return;
    }
    if (routeEntry.publicRoute && state.user) {
      navigate('#/');
      return;
    }
    try {
      await routeEntry.render(hash.match(routeEntry.pattern));
    } catch (error) {
      if (error.status === 401) return;
      view.innerHTML = `<div class="panel"><p class="error">${esc(error.message)}</p><a href="#/">Back to dashboard</a></div>`;
    }
  }

  function updateTopnav() {
    topnav.hidden = !state.user;
    currentUserLabel.textContent = state.user ? state.user.username : '';
  }

  document.getElementById('logout-button').addEventListener('click', async () => {
    await api('POST', '/api/auth/logout').catch(() => null);
    state.user = null;
    updateTopnav();
    navigate('#/login');
  });

  async function start() {
    try {
      state.user = await api('GET', '/api/auth/me');
    } catch (error) {
      state.user = null;
    }
    updateTopnav();
    window.addEventListener('hashchange', route);
    route();
  }

  start();
})();
