# KingdomClaim

A Paper plugin for Minecraft **26.2** (Java 25, Paper's calendar-versioned API
`io.papermc.paper:paper-api:26.2.build.+`). Players found kingdoms, claim and
protect chunks of land, manage membership through ranks, and go to war (or
ally) with other kingdoms — all backed by an in-game GUI as well as commands.

## Features

- **Kingdoms & membership** — found a kingdom, invite/accept/kick, three
  ranks (King, Officer, Member), kingship transfer, kingdom-only chat.
- **Claiming & protection** — officers+ claim/unclaim the chunk they're
  standing in; block break/place, containers, doors and PvP are protected
  from non-members inside claimed land.
- **Claim visualization & management GUI** — `/kingdom gui` opens a menu for
  members, claims and relations; the claims screen paginates every claimed
  chunk with click-to-teleport and click-to-unclaim, plus a one-click
  "visualize all" that draws every claim's border in particles (visible only
  to you). `/kingdom map` renders a quick ASCII ownership grid in chat.
- **Diplomacy & PvP** — kingdoms declare ALLY / NEUTRAL / ENEMY / WAR towards
  each other (via command or the Relations GUI screen). PvP and claim-raiding
  are only permitted between kingdoms whose effective relation allows it;
  ALLY requires both sides to agree, while ENEMY/WAR take effect the moment
  either side declares them.

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
| `relation <ally\|neutral\|enemy\|war> <kingdom>` | Set your kingdom's stance towards another (officer+) |
| `chat` | Toggle kingdom-only chat |
| `sethome` / `home` | Set/teleport to the kingdom home (must be on claimed land) |
| `info [kingdom]` | Show kingdom stats |
| `list` | List all kingdoms |
| `admin bypass` | Toggle protection bypass (`kingdomclaim.admin`) |

## Permissions

- `kingdomclaim.use` (default: `true`) — base command access
- `kingdomclaim.admin` (default: `op`) — protection bypass, admin subcommands

## Building

Requires JDK 21+ and network access to `repo.papermc.io` (blocked in some
sandboxed CI environments — build from a machine/CI that can reach it):

```
./gradlew build
```

The compiled plugin jar is written to `build/libs/KingdomClaim-<version>.jar`;
drop it into your Paper 26.2 server's `plugins/` folder.

## Data

Kingdom data is stored in `plugins/KingdomClaim/kingdoms.yml`, autosaved on
the interval configured in `config.yml` (`autosave-minutes`, default 5) and
on server shutdown.
