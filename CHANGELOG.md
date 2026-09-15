# Changelog

All notable changes to KingdomClaim are tracked here, split into what was
**Added** (new capability) and what was later **Altered** (changed behavior
of something already shipped). Everything below is currently on version
`1.0.0` — nothing has had a version bump yet, so this reads top-to-bottom as
the plugin's full history so far, oldest first.

## Added

- **Kingdoms & membership**: found a kingdom, invite/accept/deny invites,
  leave, kick, promote/demote, transfer kingship, disband. Three ranks
  (King, Officer, Member).
- **Chunk claiming & protection**: claim/unclaim the chunk you're standing
  in, up to a configurable per-kingdom limit. Claimed land is protected
  from non-members.
- **Diplomacy**: Ally / Neutral / Enemy / War relations between kingdoms,
  with PvP gated by relation.
- **War & raiding**: declare war, a notice period before it starts, and a
  TNT-based mechanic for the attacker to breach the defender's claims.
- **In-game GUI**: a main menu with Members, Claims, and Relations screens,
  reachable via `/kingdom gui`.
- **Claim visualization**: in-world particle borders (`/kingdom visualize`)
  and an ASCII ownership map in chat (`/kingdom map`).
- **Kingdom-only chat channel** (`/kingdom chat`).
- **Kingdom home**: set and teleport to a home located on the kingdom's own
  claimed land.
- **Persistence**: all kingdom data saved to `kingdoms.yml`, loaded on
  startup, autosaved on an interval and on shutdown.
- **Permissions**: `kingdomclaim.use` for the base command,
  `kingdomclaim.admin` for protection bypass and admin subcommands.
- **Automatic builds**: a CI workflow that builds the plugin jar on every
  push and publishes it to a rolling "latest" GitHub Release.
- **Kingdom color & crown**: the king picks a color that tints every
  member's nametag, tab-list entry, and chat name; the king additionally
  shows a crown (♔) next to their name everywhere it appears.
- **`/kingdom surrender`**: either king can end an active war immediately.
- **Surrender claim-loss penalty**: surrendering costs the surrendering
  kingdom a percentage of its claimed chunks (config-controlled), plus a
  matching reduction to its claim limit until an admin lifts it.
- **`/kingdom canceldeclare`**: the declaring king can call off a pending
  war declaration any time before its 30-minute notice period elapses;
  notifies both kingdoms and the war never starts.
- **Item frame protection**: non-members can no longer place, rotate, or
  remove an item frame's item in a claim, war or not.
- **Animal protection**: non-members can no longer kill farm animals
  (cows, pigs, sheep, chickens, etc.) in a claim at peace; this opens up
  to the attacker during an active war, same as PvP.
- **War cobwebs**: attackers may now also place cobwebs (not just TNT)
  inside the defender's claims during an active war, to slow defenders
  down; toggleable via `war-cobwebs-enabled` in config.yml (default `true`).

## Altered

- **Claim protection rewritten**: peacetime building/breaking/interacting
  is now member-only with no officer-level exception; wartime raiding was
  narrowed from "enemies can break/place anything" down to "attackers may
  only place and light TNT" against the specific defender they're at war
  with — every other explosion in a claim (creepers, unrelated TNT, blast
  spilling onto an unrelated kingdom) is neutralized.
- **Diplomacy restricted to the king**: setting Ally/Neutral/Enemy and
  declaring war were officer+; both now require the king specifically.
- **Claims must be connected**: every claim after a kingdom's first must be
  edge-adjacent to one of its existing claims, keeping territory as one
  connected shape.
- **Farmland trampling protection added**: non-members could previously
  pop crops off by jumping on farmland without ever triggering the
  block-break protection; this is now blocked the same as any other
  griefing.
- **Claim/home teleports fixed to use the exact recorded spot**: the claims
  GUI was sending players to a guessed highest-block position instead of
  wherever they actually stood when they claimed the chunk. Claims now
  record that exact position and teleport there (a claim made before this
  existed falls back to the old guess, since it has no recorded spot).
- **Nether claiming gated behind a config toggle** (`allow-nether-claims`,
  off by default) instead of always being allowed.
- **Claims GUI now shows both chunk and center coordinates** for each
  claim, instead of only chunk coordinates.
- **Claim teleporting made independently toggleable**
  (`claim-teleport-enabled`) from the kingdom-home teleport toggle
  (`home-teleport-enabled`).
- **Enemy no longer grants claim-interior PvP rights**: declaring a kingdom
  ENEMY is now a pure diplomatic signal — it no longer lets you attack their
  members inside their own claims. Only an active WAR does; wilderness PvP
  was already unrestricted for everyone regardless of relation.

## Commands

All subcommands live under `/kingdom` (aliases: `/k`, `/kd`).

| Command | Description |
|---|---|
| `create <name>` | Found a new kingdom |
| `disband` | Disband your kingdom (king only) |
| `invite <player>` | Invite a player to your kingdom (officer+) |
| `accept <kingdom>` / `deny <kingdom>` | Accept/decline a pending invite |
| `invites` | List your pending invites |
| `leave` | Leave your kingdom |
| `kick <player>` | Remove a member (officer+) |
| `promote <player>` / `demote <player>` | Change a member's rank (officer+) |
| `transfer <player>` | Hand kingship to another member (king only) |
| `claim` | Claim the chunk you're standing in (officer+; must connect to existing territory) |
| `unclaim` | Unclaim the chunk you're standing in (officer+) |
| `map` | Show an ASCII ownership map of nearby chunks |
| `visualize` | Show your kingdom's claim borders as particles |
| `gui` | Open the kingdom management menu |
| `relation <ally\|neutral\|enemy> <kingdom>` | Set your kingdom's stance towards another (king only) |
| `declarewar <kingdom>` | Declare war (king only, defender's king must be online, 30 min notice) |
| `canceldeclare <kingdom>` | Call off your own pending war declaration before it starts (king only) |
| `surrender <kingdom>` | End an active war immediately, at a claim-loss cost (king only, either side) |
| `color <color>` | Set your kingdom's color (king only) — tab-completes valid names |
| `chat` | Toggle kingdom-only chat |
| `sethome` | Set the kingdom home (must be on claimed land, officer+) |
| `home` | Teleport to the kingdom home |
| `info [kingdom]` | Show a kingdom's stats |
| `list` | List all kingdoms |
| `admin bypass` | Toggle protection bypass (`kingdomclaim.admin`) |
| `admin resetpenalty <kingdom>` | Clear a kingdom's surrender claim-limit penalty (`kingdomclaim.admin`) |

## Config reference (`config.yml`)

| Key | Default | Effect |
|---|---|---|
| `autosave-minutes` | `5` | How often kingdom data is autosaved |
| `max-claims-per-kingdom` | `200` | Claim cap per kingdom (before any surrender penalty) |
| `home-teleport-enabled` | `true` | Whether `/kingdom home` works server-wide |
| `claim-teleport-enabled` | `true` | Whether clicking a claim in the GUI teleports you |
| `allow-nether-claims` | `false` | Whether `/kingdom claim` works in the Nether |
| `surrender-claim-loss-percent` | `20` | % of claims lost, edge-first, on surrender |
| `war-cobwebs-enabled` | `true` | Whether attackers may place cobwebs (alongside TNT) in a defender's claim during war |
