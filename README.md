# Poseidon

Custom packet-first Minecraft HCF anticheat foundation for 1.7/1.8 style servers.

## Current slice

- ProtocolLib packet pipeline
- Per-player movement, rotation, combat, and transaction state
- Server transaction latency measurement with bounded pending queues
- Staff alert, verbose, profile, check, violation, reset, and log commands
- Low-risk bad packet, timer, movement, combat, inventory, world, latency, and exploit checks
- Conservative movement delta check with teleport exemptions
- Ray-box reach foundation for impossible player hits
- Attack click statistics beyond raw CPS
- JSONL flag logging with rotation for tuning
- Outbound server correction and velocity tracking
- Cloud-style signal spool/export hook for cross-instance tracking
- Score-driven smart mitigation engine with monitor/enforce modes
- Conservative physics-envelope movement simulation check
- Online behavior model score for ML-ready detections
- Inventory, world interaction/scaffold, and latency-abuse families
- Cross-instance reputation cache using Poseidon signal records
- Environment-aware movement envelope for common 1.7/1.8 movement surfaces
- Global punishment/action system disabled by default

## Build

```bash
mvn clean package
```

The plugin jar is written to `target/Poseidon.jar`.

## Install

1. Build the jar or use `target/Poseidon.jar`.
2. Install ProtocolLib on the same server.
3. Place `Poseidon.jar` in the server `plugins` folder.
4. Start the server once to generate `plugins/Poseidon/config.yml`.
5. Keep `mitigations.mode: monitor` and `punishments.enabled: false` while collecting logs.

## Safe Rollout

- Start with `/ac profile monitor`.
- Staff can use `/alerts`, `/ac verbose`, `/ac checks`, `/ac check <check>`, and `/ac logs <player>`.
- Tune from `plugins/Poseidon/flags.jsonl`.
- Enable `replay.enabled: true` only during staff-test windows.
- Do not enable automatic punishments until real HCF sessions prove the thresholds are clean.

## Systems

See [SYSTEMS.md](docs/SYSTEMS.md) for the current architecture, [PERFORMANCE.md](docs/PERFORMANCE.md) for the 100-player performance profile, [COMPLETION_AUDIT.md](docs/COMPLETION_AUDIT.md) for what still needs production work, [TESTING.md](docs/TESTING.md) for live testing, and [POLAR_RESEARCH.md](docs/POLAR_RESEARCH.md) for the public Polar feature map Poseidon is using as inspiration.

## Runtime

Requires:

- Java 8 compatible server runtime
- Spigot/Paper 1.8.8 API compatible server. 1.7-style forks need a matching ProtocolLib/runtime test.
- ProtocolLib installed on the server. Build-time dependency is ProtocolLib 4.8.0; use the ProtocolLib version compatible with your actual server fork.

The packet API is kept intentionally small so the runtime ProtocolLib version can be matched to the exact 1.7/1.8 server fork.
