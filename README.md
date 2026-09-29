# Campaign Manager

A small web app for tabletop RPG groups. Log in and you get two things:

1. **Campaigns you run** as the game master, with players, characters and planning notes.
2. **Your characters**, each with a sheet shaped by its game system and a badge showing which campaign it is in.

Supported game systems: Dungeons & Dragons 5e, Pathfinder 2e, Mutants & Masterminds 3e, Delta Green,
Call of Cthulhu 7e, and a generic/homebrew sheet. Each system defines its own character sheet fields
in [`SheetTemplates`](src/main/java/com/dnd/campaignmanager/gamesystem/SheetTemplates.java).

## Two packages

| Package | Stack | Purpose |
| --- | --- | --- |
| repo root | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA | JSON API only. Owns all user data. Never serves HTML. |
| `frontend/` | React 18, TypeScript, Vite, React Router | The browser app. Talks to the API under `/api`. |

Keeping them apart means the backend can be locked down as a pure API: browsers only reach it through
the `/api` paths, every other request is denied, sessions are HttpOnly cookies, and cross-origin calls are
refused unless an origin is listed in `app.cors.allowed-origins`.

## Run it

Requires Java 21 and Node 20+. No database setup is needed; the default profile uses an embedded H2
database stored in `./data`.

Start the API:

```bash
./mvnw spring-boot:run
```

Start the frontend in a second terminal:

```bash
npm --prefix frontend install
npm --prefix frontend run dev
```

Open <http://localhost:5173>, create an account, and start. The Vite dev server proxies `/api` to
`http://localhost:8080`, so the browser sees one origin and cookies work without CORS. Point it elsewhere
with `VITE_API_TARGET` in `frontend/.env.local`.

### Deploying

Build the frontend with `npm --prefix frontend run build` and serve `frontend/dist` from a reverse proxy
(nginx, Caddy, or similar) that forwards `/api` to the Spring app. That keeps a single origin in
production too. If you must host the two on different origins, list the frontend origin in
`app.cors.allowed-origins` (env var `CORS_ALLOWED_ORIGINS` for the `mysql` profile) and switch the
session cookie to `SameSite=None; Secure`.

### Dev login bypass

The login page shows a **"Continue as dev (local only)"** button. It skips the password check
and signs you into a fixed `dev` account, created the first time it's used. This only exists when
`app.dev-login.enabled=true`, which is the default profile's setting.

**It is off for the `mysql` profile and for tests.** If you deploy this app anywhere real, either
run it with a profile other than the default one, or set `app.dev-login.enabled=false` explicitly —
otherwise anyone who can reach the site can log in as `dev` with no credentials.

### Using MySQL instead

Set the connection details as environment variables and start with the `mysql` profile:

```bash
DB_URL=jdbc:mysql://localhost:3306/dnd_db DB_USERNAME=root DB_PASSWORD=secret ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

Hibernate creates and updates the tables on start-up.

### Tests

```bash
./mvnw test
```

## How it fits together

```
com.dnd.campaignmanager
├── auth          register / login / logout / me endpoints
├── user          User entity, credentials lookup for Spring Security
├── gamesystem    GameSystem enum + per-system character sheet templates
├── campaign      Campaign, players, planning notes, and the DM-only rules
├── character     PlayerCharacter with a system-validated sheet and optional campaign
├── invite        pending offers for a specific character to join a specific campaign
├── common        shared exceptions, error payload, timestamps
└── config        session security, CSRF cookie setup, CORS allow-list

frontend/src
├── api           typed DTOs, fetch wrapper with CSRF header, one function per endpoint
├── auth          current-user context (login, register, dev login, logout)
├── components    layout, badges, panels, toast
├── sheet         renders a character sheet from the game system's field template, plus the jump-to-section rail
└── pages         one file per screen: login, dashboard, campaign, constraints, character
```

Rules that shape the design:

- The user who creates a campaign is its game master. Only they can edit it, add players, and write notes.
  Removing a player from the roster works two ways: the game master can remove anyone, and a player can
  remove themselves. Either way releases that player's characters from the campaign, it never deletes them.
- A note is private to the game master unless it is marked as shared with players.
- A character belongs to one user and can be in at most one campaign at a time. It must leave before joining
  another, and the campaign must use the same game system.
- A campaign can have a max player count. Adding a player past it is rejected, and the game master cannot
  lower it below the current number of players.
- The game master can set **build constraints** per campaign. A character must satisfy them to join, and a
  sheet that breaks them cannot be saved while the character is in the campaign. Adding a rule never kicks
  anyone out; characters that now break a rule are flagged on the campaign page instead.
- Bringing a character into a campaign works two ways. A player can join their own character directly, no
  approval needed. A game master can instead **invite** one of an existing player's characters; that invite
  sits pending, with nothing to expire it, until the character's owner accepts or declines it from their
  dashboard. Either path still has to satisfy the build constraints and the single-active-campaign rule.
- Deleting a campaign releases its characters; it never deletes them.
- Sheet values are stored as strings and validated against the system template on every save.

### Build constraint rules

Each rule targets one field, one section, or every field of the matching kind.

| Rule | Applies to | Example |
| --- | --- | --- |
| Max value / Min value | number fields | Strength at most 16, Level at least 3 |
| Max total | number fields in a section, or all of them | Ability Scores total at most 75 |
| Max entries | list fields | Equipment: at most 6 entries |
| Same entry at most N times | list fields | Equipment: the same item at most 2 times |
| Forbidden text | text and list fields | Race may not contain "Aarakocra" |

List fields are read one entry per line, or separated by commas. Rules live in
[`BuildConstraint`](src/main/java/com/dnd/campaignmanager/campaign/BuildConstraint.java); to add a new kind,
add a `ConstraintType` and handle it there.

## API summary

All endpoints except register and login need a session cookie. Write requests also need the `X-XSRF-TOKEN`
header copied from the `XSRF-TOKEN` cookie.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register`, `/api/auth/login`, `/api/auth/logout` | Account and session |
| GET | `/api/auth/me` | Current user |
| GET | `/api/game-systems`, `/api/game-systems/{code}` | Systems and their sheet fields |
| GET | `/api/campaigns/dm`, `/api/campaigns/playing` | Campaigns you run / play in |
| GET, POST, PUT, DELETE | `/api/campaigns`, `/api/campaigns/{id}` | Campaign CRUD |
| POST, DELETE | `/api/campaigns/{id}/players`, `/api/campaigns/{id}/players/{userId}` | Manage players |
| POST, PUT, DELETE | `/api/campaigns/{id}/notes`, `/api/campaigns/{id}/notes/{noteId}` | Planning notes |
| POST, DELETE | `/api/campaigns/{id}/constraints`, `/api/campaigns/{id}/constraints/{constraintId}` | Build constraints |
| GET | `/api/campaigns/{id}/players/{playerId}/characters` | That player's characters eligible to invite |
| POST, DELETE | `/api/campaigns/{id}/invites`, `/api/campaigns/{id}/invites/{inviteId}` | DM sends or cancels an invite |
| GET, POST, PUT, DELETE | `/api/characters`, `/api/characters/{id}` | Character CRUD |
| PUT, DELETE | `/api/characters/{id}/campaign` | Join or leave a campaign directly (the owner's own character) |
| GET | `/api/invites` | Pending invites addressed to your characters |
| POST | `/api/invites/{id}/accept`, `/api/invites/{id}/decline` | Respond to an invite |

## Adding a game system

1. Add a constant to `GameSystem` with its display name and what the group calls the game master.
2. Add a template method in `SheetTemplates` and register it in the static block.

The API and the frontend pick the new system up automatically.
