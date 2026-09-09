# KingdomClaim

A Paper plugin for Minecraft **26.2** (Java 25, Paper's calendar-versioned API
`io.papermc.paper:paper-api:26.2.build.+`). Players found kingdoms, claim and
protect chunks of land, manage membership through ranks, and go to war (or
ally) with other kingdoms — all backed by an in-game GUI as well as commands.

## Features

- **Kingdoms & membership** — found a kingdom, invite/accept/kick, three
  ranks (King, Officer, Member), kingship transfer, kingdom-only chat.
- **Kingdom color & crown** — the king picks a color (`/kingdom color <color>`)
  that tints every member's nametag, tab-list entry, and chat name (via two
  per-kingdom scoreboard teams); the king additionally shows a crown (♔)
  next to their name everywhere it appears, so leadership is visible at a
  glance.
- **Claiming & protection** — officers+ claim/unclaim the chunk they're
  standing in. At peace, only members of the owning kingdom may break or
  place any block, use a bucket, or right-click anything in a claim
  (containers, doors, buttons, all of it) — there's no officer-only
  carve-out for everyday building.
- **Claim visualization & management GUI** — `/kingdom gui` opens a menu for
  members, claims and relations; the claims screen paginates every claimed
  chunk with click-to-teleport and click-to-unclaim, plus a one-click
  "visualize all" that draws every claim's border in particles (visible only
  to you). `/kingdom map` renders a quick ASCII ownership grid in chat.
- **Diplomacy, PvP & war raiding** — kingdoms declare ALLY / NEUTRAL / ENEMY
  towards each other instantly (command or the Relations GUI); ALLY needs
  both sides to agree, ENEMY takes effect the moment either side declares
  it and opens PvP. WAR is a heavier, gated declaration (see below) that
  additionally lets the attacker breach the defender's claims with TNT —
  and only TNT: no other block may be broken, placed, or interacted with
  by a non-member, war or not.

  **Declaring war** (`/kingdom declarewar <kingdom>` or the Relations GUI):
  - Only the king of the declaring kingdom may declare war (as with setting
    Ally/Neutral/Enemy — all diplomacy is a king-only power).
  - The defending kingdom's king must be online at the moment of declaration.
  - War doesn't start immediately: there's a 30-minute notice period during
    which nothing war-related is active (no raiding, no forced PvP) —
    both kingdoms are notified when it's declared and again when it begins.
  - Once active, the attacking kingdom's members may place and light TNT
    inside the defender's claims — nothing else. They still cannot break
    blocks directly, open doors/containers, or place anything but TNT;
    the only way in is blowing a way in. Each authorized TNT explosion only
    damages that specific defender's claims — other kingdoms' land, and any
    explosion not tied to an authorized war TNT (creepers, stray TNT, etc.),
    stays protected.

## Commands

All subcommands live under `/kingdom` (aliases: `/k`, `/kd`).

| Command | Description |
|---|---|
| `create <name>` | Found a new kingdom |
| `disband` | Disband your kingdom (king only) |
| `invite <player>` / `accept <kingdom>` / `deny <kingdom>` / `invites` | Membership invites |
| `leave` | Leave your kingdom |
| `kick <player>` | Remove a member (officer+) |
| `promote <player>` / `demote <player>` | Change a member's rank (officer+) |
| `transfer <player>` | Hand kingship to another member (king only) |
| `claim` / `unclaim` | Claim/unclaim the chunk you're standing in (officer+) |
| `map` | Show an ASCII ownership map of nearby chunks |
| `visualize` | Show your kingdom's claim borders as particles |
| `gui` | Open the kingdom management menu |
| `relation <ally\|neutral\|enemy> <kingdom>` | Set your kingdom's stance towards another (king only) |
| `declarewar <kingdom>` | Declare war (king only, defender's king must be online, 30 min notice) |
| `color <color>` | Set your kingdom's color (king only) — tab-completes valid names |
| `chat` | Toggle kingdom-only chat |
| `sethome` / `home` | Set/teleport to the kingdom home (must be on claimed land) |
| `info [kingdom]` | Show kingdom stats |
| `list` | List all kingdoms |
| `admin bypass` | Toggle protection bypass (`kingdomclaim.admin`) |

## Permissions

- `kingdomclaim.use` (default: `true`) — base command access
- `kingdomclaim.admin` (default: `op`) — protection bypass, admin subcommands

## Building

Requires JDK 25+, Gradle 8.x, and network access to `repo.papermc.io`
(blocked in some sandboxed CI environments — build from a machine/CI that can
reach it). No Gradle wrapper is checked in; use a local Gradle install, or
generate one yourself with `gradle wrapper`:

```
gradle build
```

The compiled plugin jar is written to `build/libs/KingdomClaim-<version>.jar`;
drop it into your Paper 26.2 server's `plugins/` folder.

## Data

Kingdom data is stored in `plugins/KingdomClaim/kingdoms.yml`, autosaved on
the interval configured in `config.yml` (`autosave-minutes`, default 5) and
on server shutdown. `config.yml` also has `max-claims-per-kingdom` (default
200) and `home-teleport-enabled` (default `true` — set `false` to disable
`/kingdom home` server-wide; `/kingdom sethome` still works either way).
