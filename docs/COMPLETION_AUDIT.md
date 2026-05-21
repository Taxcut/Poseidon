# Poseidon Completion Audit

This audit separates wired systems from systems that still need deeper engineering before Poseidon should be trusted for automatic punishments on an HCF network.

## Wired Now

- Bukkit plugin entrypoint, `plugin.yml`, config filtering, and Maven jar output.
- ProtocolLib client/server packet listeners with unsupported-packet filtering.
- `/poseidon` staff command and `/alerts` direct toggle command with aliases.
- Per-player packet state, transaction ping, movement history, velocity capture, and world tracking.
- Alert pipeline with staff messages, console output, JSONL flag logs, and behavior signals.
- Signal consumers for local behavior scoring, mitigation scoring, optional cloud export, and optional shared-file reputation.
- Conservative checks for bad packets, timer, movement delta, movement envelope, reach, autoclicker rate, inventory behavior, world interaction, latency abuse, and model score.
- Mitigation engine in monitor/enforce modes, with combat range/damage controls and optional inventory/world/movement restrictions.
- Reloadable check metadata covering enabled, alert, punish, max VL, decay, buffer, mitigation, experimental status, category, severity, and description.
- Expanded staff commands for checks, profiles, verbose alerts, violations, resets, logs, and runtime metrics.
- Raytraced reach foundation, velocity response foundation, split inventory/world checks, aim-analysis foundations, improved click statistics, and packet exploit guards.
- Central exemption manager, bounded transaction queues, size-rotated logs/replay files, and a disabled-by-default action system.
- Unit tests for action recent-flag requirements and transaction cleanup.

## Fixed In This Pass

- Removed the slow unused `minevolt.net` Maven repository from `pom.xml`.
- Added null-safe command registration so a broken `plugin.yml` logs a warning instead of throwing during enable.
- Packet listeners now skip packet types unsupported by the active ProtocolLib/runtime pair.
- Disabled checks no longer run side effects such as latency compensation or packet counters.
- `POSITION_LOOK` packets now update position, rotation, and previous-ground state atomically.
- Teleports now reset stored movement deltas, reducing post-teleport false evidence.
- Transaction tracking only stores a pending transaction after the packet is actually sent.
- Transaction tracking disables itself with a warning if the runtime does not support server transaction packets.
- Cross-instance tracking now defaults disabled if config keys are missing, matching the 100-player performance profile.
- Reach ignores cross-world entity-id mappings.
- `/poseidon reload` now rebuilds check instances and restarts reloadable managers instead of leaving stale thresholds in memory.
- Monitor, staff-test, and production profile application is wired, with punishments still off by default.
- `production-safe` and `aggressive-test` profiles are wired; global punishments remain disabled unless explicitly changed.

## Needs More Work Before Enforce Mode

- Replace `SimulationA` movement envelopes with exact 1.7/1.8 physics and full collision-box enumeration for slabs, stairs, carpets, pistons, liquids, webs, climbables, fences, doors, ice, soul sand, block edge cases, and server-specific movement patches.
- Deepen `VelocityA` so every velocity packet is paired with explicit transaction confirmation windows and replayable response traces.
- Deepen `ReachA` with more precise target pose history, server-side hurtbox quirks, and better obstruction exemptions for HCF arena layouts.
- Add rotation/aim checks only after collecting clean legit HCF fight logs; aim checks are easy to false flag without real baseline data.
- Add AutoSoup/AutoPot, ChestStealer, Refill, ScaffoldFace, ScaffoldExpand, Eagle/Safewalk, and block-order checks only when there is enough packet context to avoid weak detections.
- Add replay/tuning tooling for `flags.jsonl` so thresholds can be validated against real legit and cheat sessions.
- Add deeper replay analysis tooling that can summarize sessions automatically instead of manually reading JSONL.

## Live Test Checklist

- Run the jar on the exact server fork and ProtocolLib version that production will use.
- Join with 10-20 normal players and check startup logs for skipped unsupported packets.
- Confirm `/alerts`, `/ac alerts`, `/ac info <player>`, `/ac cloud`, `/ac reload`, and `/ac version`.
- Fight, pearl, pot, refill, climb ladders, swim, use webs, run on ice/soul sand, ride vehicles, teleport, change worlds, and bridge while monitoring `flags.jsonl`.
- Simulate lag and TPS drops before tightening any thresholds.
- Only move to stronger mitigation/action settings after a clean monitor-mode sample from real HCF gameplay.
