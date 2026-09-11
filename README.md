# Campaign Manager

A small web app for tabletop RPG groups. Log in and you get two things:

1. **Campaigns you run** as the game master, with players, characters and planning notes.
2. **Your characters**, each with a sheet shaped by its game system and a badge showing which campaign it is in.

Supported game systems: Dungeons & Dragons 5e, Pathfinder 2e, Mutants & Masterminds 3e, Delta Green,
Call of Cthulhu 7e, and a generic/homebrew sheet. Each system defines its own character sheet fields
in [`SheetTemplates`](src/main/java/com/dnd/campaignmanager/gamesystem/SheetTemplates.java).

## Run it

Requires Java 21. No database setup is needed; the default profile uses an embedded H2 database stored in `./data`.

```bash
./mvnw spring-boot:run
```

Open <http://localhost:8080>, create an account, and start.

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
├── common        shared exceptions, error payload, timestamps
└── config        session security and CSRF cookie setup
```

Rules that shape the design:

- The user who creates a campaign is its game master. Only they can edit it, add or remove players, and write notes.
- A note is private to the game master unless it is marked as shared with players.
- A character belongs to one user and can be in at most one campaign at a time. It must leave before joining
  another, and the campaign must use the same game system.
- A campaign can have a max player count. Adding a player past it is rejected, and the game master cannot
  lower it below the current number of players.
- The game master can set **build constraints** per campaign. A character must satisfy them to join, and a
  sheet that breaks them cannot be saved while the character is in the campaign. Adding a rule never kicks
  anyone out; characters that now break a rule are flagged on the campaign page instead.
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
| GET, POST, PUT, DELETE | `/api/characters`, `/api/characters/{id}` | Character CRUD |
| PUT, DELETE | `/api/characters/{id}/campaign` | Join or leave a campaign |

## Adding a game system

1. Add a constant to `GameSystem` with its display name and what the group calls the game master.
2. Add a template method in `SheetTemplates` and register it in the static block.

The API and the frontend pick the new system up automatically.
