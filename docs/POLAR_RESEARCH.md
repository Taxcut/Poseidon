# Polar Public Feature Map

Poseidon is custom code, but its feature roadmap is aligned with public Polar documentation.

## Publicly Documented Polar Ideas

- SaaS/cloud checks reduce plugin CPU usage, while real-time setback checks such as movement stay inside the plugin.
- Polar lists these detection families: Auto Clicker, Heuristics, Invalid Protocol, Movement, Tick Speed, Inventory, World Interaction, and Latency Abuse.
- Polar describes mitigations as limitations instead of immediate bans or kicks, including movement and combat limitations.
- Polar documents movement mitigation strategies such as velocity adjustment, hit cancellation, setbacks, and combat mitigation strategies such as damage and range reduction.
- Polar exposes five movement leniency levels: SEVERE, MODERATE, BALANCED, PERMISSIVE, and LENIENT.

## Poseidon Mapping

- Auto Clicker: `AutoClickerA`
- Heuristics: `ModelA` and the online behavior model
- Invalid Protocol: `BadPacketsA`, `BadPacketsB`, `BadPacketsC`
- Movement: `MoveA`, `SimulationA`
- Tick Speed: `TimerA`
- Inventory: `InventoryA`
- World Interaction / Scaffold: `WorldInteractionA`
- Latency Abuse: `LatencyAbuseA`, silent latency compensation state
- Mitigations: score-based light, medium, and heavy levels in monitor or enforce mode
- Cloud-style tracking: JSONL spool plus optional HTTP export endpoint

## Sources

- https://polar.top/
- https://docs.polar.top/faq
- https://docs.polar.top/configuration
- https://docs.polar.top/detections
- https://docs.polar.top/commands-and-permissions
