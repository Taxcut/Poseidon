# Poseidon Testing Guide

Use Poseidon in monitor mode first. The default config does not enable punishments.

## Compile Test

Run:

```bash
mvn clean package
```

Expected output:

- Build success
- Jar at `target/Poseidon.jar`
- Unit tests pass for action recent-flag thresholds and transaction cleanup

## Server Startup Test

1. Use the exact 1.7/1.8 server fork planned for production.
2. Install a compatible ProtocolLib build.
3. Put `Poseidon.jar` in `plugins`.
4. Start the server and confirm:
   - Poseidon enables
   - ProtocolLib is loaded first
   - No command registration warnings appear
   - Unsupported packet warnings are limited to packets unavailable on that runtime

## Command Test

Run as an operator or staff user:

- `/alerts`
- `/ac help`
- `/ac reload`
- `/ac info`
- `/ac info <player>`
- `/ac checks`
- `/ac check ReachA`
- `/ac verbose`
- `/ac verbose <player>`
- `/ac violations <player>`
- `/ac reset <player>`
- `/ac profile monitor`
- `/ac profile production-safe`
- `/ac logs <player>`
- `/ac alerts on`

## Config Validation

On startup and reload, Poseidon validates that every registered check has the required config keys:

- `enabled`
- `alert`
- `punish`
- `alert-vl`
- `max-vl`
- `decay`
- `buffer`
- `setback`
- `mitigation`
- `experimental`
- `description`
- `category`
- `severity`

Missing keys are logged as warnings.

## Monitor Mode Tuning

Start with:

```yaml
mitigations:
  mode: monitor
```

Watch `plugins/Poseidon/flags.jsonl` while testing:

- Normal PvP
- Potting/refilling
- Pearling
- Webs
- Ladders/vines
- Ice/soul sand
- Slabs/stairs/fences/carpets
- Teleports/world changes
- High ping
- TPS drops
- Bridging/towering

For deeper staff-test sessions, temporarily enable:

```yaml
replay:
  enabled: true
```

Then inspect `plugins/Poseidon/replay-sessions.jsonl` for compact packet context.

Tune first:

- `checks.ReachA.max-distance`
- `checks.SimulationA.horizontal-buffer`
- `checks.SimulationA.vertical-buffer`
- `checks.VelocityA.min-horizontal-ratio`
- `checks.TimerA.max-flying-packets-per-second`
- `settings.alerts.cooldown-ms`

## False Ban Safety

Keep these rules until you have real logs:

- Keep all `punish` values false.
- Keep `mitigations.mode: monitor`.
- Keep `punishments.enabled: false`.
- Treat all `experimental: true` checks as staff evidence only.
- Do not punish from reach or velocity alone.
- Do not tighten movement simulation until legit HCF movement has been logged.

## Punishment Test

Punishments are globally disabled by default:

```yaml
punishments:
  enabled: false
```

When testing actions on a private server, use `aggressive-test` first and keep `punishments.enabled: false` until logs are clean. The action system supports placeholders:

- `{player}`
- `{uuid}`
- `{check}`
- `{type}`
- `{vl}`
- `{ping}`
- `{tps}`
- `{reason}`
