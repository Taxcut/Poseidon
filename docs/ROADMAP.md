# Poseidon Roadmap

This project should grow around evidence quality, not raw check count.

## Phase 1: Packet Foundation

- Packet listener and per-player state
- Transaction latency tracking
- Position history
- Outbound correction and velocity tracking
- Staff alerts and JSONL flag logs

Status: implemented as the first working foundation.

## Phase 2: Conservative Production Checks

- Bad packet checks for impossible protocol states
- Timer with lag-aware buffering
- Conservative reach using historical positions
- Autoclicker rate and timing-distribution checks
- Movement delta checks with teleport/correction exemptions

Status: implemented for the first public-family pass.

## Phase 3: Movement Simulation

- Reproduce 1.7/1.8 friction, gravity, jump, sprint, sneak, and potion physics
- Add block-aware collision support for slabs, stairs, webs, liquids, ice, ladders, vines, and soul sand
- Simulate legal movement envelopes instead of comparing against one magic speed number
- Keep false-positive evidence in logs before enabling punishments

Status: envelope simulation implemented; exact collision-box enumeration still pending.

## Phase 4: Combat Model

- Track target position history from packets
- Add ray-box intersection for hit validation
- Compensate by measured transaction latency
- Separate obvious reach from subtle reach so thresholds can be tuned independently
- Add aim consistency checks only after collecting clean legit fight data

Status: conservative historical reach implemented; deeper rotation/aim heuristics pending.

## Phase 5: Velocity and Setbacks

- Pair outbound velocity packets with transaction confirmations
- Validate horizontal and vertical response windows
- Account for collision, liquids, webs, ground friction, and server correction packets
- Add setback support only after movement simulation is reliable

Status: outbound velocity tracking implemented; validation windows pending.

## Phase 6: Tuning and Operations

- Record flag snapshots for replay-style analysis
- Build config profiles for alerts-only, staff-test, and production modes
- Add punishment actions after checks have enough live data
- Test against legit recordings, lag simulation, TPS drops, and common HCF clients

Status: signal logs, cloud spool, behavior model, and mitigation monitor are implemented.
