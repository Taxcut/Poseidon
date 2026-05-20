# Poseidon Performance Profile

This profile is intended for roughly 100-player HCF servers.

## Defaults Chosen for 100 Players

- `cloud.enabled: false`
- `cloud.cross-instance.enabled: false`
- `settings.runtime-sampler-interval-ticks: 2`
- Batched flag-log writes every 20 ticks
- Batched cloud/reputation writes when cloud is explicitly enabled
- Latency-abuse compensation signals capped to once per player per second

## Hot Path Optimizations

- Reach no longer scans world entities per attack.
- Reach uses cached player entity IDs and packet-position history.
- Flag logs use one async batch writer instead of one async task per flag.
- Cloud spooling uses batch writes and capped HTTP sends.
- Cross-instance reputation writes are batched.
- Runtime block/environment sampling runs every 2 ticks by default.

## Recommended Production Settings

Start with:

```yaml
cloud:
  enabled: false
  cross-instance:
    enabled: false

mitigations:
  mode: monitor

settings:
  runtime-sampler-interval-ticks: 2
```

After at least a few hours of clean live logs, enable `mitigations.mode: enforce` gradually.

Only enable cloud/cross-instance tracking when you have a real shared path or backend endpoint. For multiple backend instances, put `cloud.cross-instance.reputation-file` on fast shared storage or replace it with a dedicated backend.
