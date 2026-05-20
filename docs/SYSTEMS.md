# Poseidon Systems

Poseidon is organized around evidence signals. Checks produce signals, and the higher-level systems consume those signals for reputation, behavior modeling, cloud export, and mitigations.

## Signal Pipeline

Every alert-worthy or compensation-worthy event becomes a `BehaviorSignal`.

Consumers:

- `CloudSyncManager` writes JSONL records and optionally POSTs them to an HTTP endpoint.
- `CrossInstanceReputationManager` writes compact reputation records and imports them from a shared file.
- `OnlineBehaviorModel` converts signals into decaying feature buckets and a score.
- `MitigationManager` converts weighted signals plus cross-instance risk into player restrictions.

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

This is not yet a complete block-collision simulator. The next step is replacing broad envelopes with exact 1.7/1.8 collision-box enumeration.

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
