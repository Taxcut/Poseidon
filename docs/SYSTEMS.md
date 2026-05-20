# Poseidon Systems

Poseidon is organized around evidence signals. Checks produce signals, and the higher-level systems consume those signals for reputation, behavior modeling, cloud export, and mitigations.

## Signal Pipeline

Every alert-worthy or compensation-worthy event becomes a `BehaviorSignal`.

Consumers:

- `CloudSyncManager` writes JSONL records and optionally POSTs them to an HTTP endpoint.
- `CrossInstanceReputationManager` writes compact reputation records and imports them from a shared file.
- `OnlineBehaviorModel` converts signals into decaying feature buckets and a score.
- `MitigationManager` converts weighted signals plus cross-instance risk into player restrictions.

## Reloadable Check Registry

Every registered check now has a config-backed definition:

- enabled
- alert
- punish
- max-vl
- decay
- buffer
- setback
- mitigation
- experimental
- description
- category
- severity

`/poseidon reload` reloads config, rebuilds checks, restarts reloadable managers, and validates that each check has the required config keys.

## Cross-Instance Tracking

Poseidon currently supports a shared-file reputation cache.

Set `cloud.cross-instance.reputation-file` to a path shared by all backend instances. Each instance appends compact records and periodically imports the full recent window. This is intentionally simple, auditable, and backend-neutral. A future HTTP or Redis backend can implement the same record format.

For a single 100-player server, leave `cloud.enabled` and `cloud.cross-instance.enabled` disabled until you actually have a shared backend path or endpoint. Local detections, mitigations, and model scoring still run without cloud.

## Movement Simulation

`SimulationA` uses `MovementSimulator` to build a legal movement envelope from:

- Packet timing
- Ground state
- Speed and jump potion amplifiers
- Recent server velocity
- Liquids
- Webs
- Ice
- Ladders and vines
- Soul sand
- Configured movement tolerance

This is not yet a complete block-collision simulator. It now samples sprint/sneak state, common surface effects, slime, and nearby complex collision blocks, but the next step is still replacing broad envelopes with exact 1.7/1.8 collision-box enumeration.

## Combat Validation

`ReachA` now uses attacker eye position, rotation direction, historical target boxes, latency compensation, sprint/sneak pose adjustment, and a conservative obstruction pass. It remains monitor-first until real HCF fights are logged.

`VelocityA` consumes tracked server velocity packets and compares early horizontal/vertical response ratios with exemptions for liquid, webs, climbables, complex collisions, teleports, and latency compensation.

## Split Interaction Checks

Inventory and world checks are split into focused families:

- `InventoryMove`
- `FastClick`
- `CombatInventory`
- `FastPlace`
- `ImpossiblePlace`
- `ScaffoldRotation`
- `ScaffoldTower`
- `BlockRaytrace`

The old aggregate `InventoryA` and `WorldInteractionA` remain present but disabled by default.

## Replay Foundation

`ReplayRecorder` can be enabled with `replay.enabled: true`. It writes compact JSONL packet sessions for movement, rotations, attacks, velocity, block placements, and inventory clicks. It is disabled by default for performance and should be used during staff-test tuning windows.

## Behavior Model

The online model is ML-ready but intentionally deterministic today. It maintains decaying buckets:

- movement
- combat
- inventory
- world
- packet
- timing
- latency
- behavior

The score is a logistic transform of the weighted feature sum. This keeps the system explainable while leaving a clean path to a trained model later.

## Smart Mitigations

Mitigations have three levels: light, medium, and heavy. In `monitor` mode they only show state. In `enforce` mode Poseidon can:

- Scale or cancel combat damage
- Limit combat range
- Cancel inventory interaction
- Cancel block placement
- Optionally setback heavy movement

Production servers should run `monitor` until live logs show the thresholds are stable.
