# Poseidon

Custom packet-first Minecraft HCF anticheat foundation for 1.7/1.8 style servers.

## Current slice

- ProtocolLib packet pipeline
- Per-player movement, rotation, combat, and transaction state
- Server transaction latency measurement
- Staff alert command
- Initial low-risk bad packet and timer checks
- Conservative movement delta check with teleport exemptions
- Conservative reach check for obviously impossible player hits
- Attack-packet CPS check
- JSONL flag logging for tuning
- Outbound server correction and velocity tracking
- Cloud-style signal spool/export hook for cross-instance tracking
- Score-driven smart mitigation engine with monitor/enforce modes
- Conservative physics-envelope movement simulation check
- Online behavior model score for ML-ready detections
- Inventory, world interaction/scaffold, and latency-abuse families
- Cross-instance reputation cache using Poseidon signal records
- Environment-aware movement envelope for common 1.7/1.8 movement surfaces

## Build

```bash
mvn clean package
```

The plugin jar is written to `target/Poseidon.jar`.

## Systems

See [SYSTEMS.md](docs/SYSTEMS.md) for the current architecture, [PERFORMANCE.md](docs/PERFORMANCE.md) for the 100-player performance profile, [COMPLETION_AUDIT.md](docs/COMPLETION_AUDIT.md) for what still needs production work, and [POLAR_RESEARCH.md](docs/POLAR_RESEARCH.md) for the public Polar feature map Poseidon is using as inspiration.

## Runtime

Requires:

- Java 8 compatible server runtime
- Spigot/Paper 1.8.8 API compatible server
- ProtocolLib installed on the server

The packet API is kept intentionally small so the runtime ProtocolLib version can be matched to the exact 1.7/1.8 server fork.
