# ForgeBalance

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10--1.21.11-3fb950?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot-f97316?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17-e11d48?style=for-the-badge&logo=openjdk)
![Config](https://img.shields.io/badge/Config-100%2B%20options-2563eb?style=for-the-badge)
![Admin GUI](https://img.shields.io/badge/Admin%20GUI-In--Game-0f766e?style=for-the-badge)

**ForgeBalance** is an SMP balance/control plugin for PvP servers that want strict rules without installing 10 separate plugins.  
You get damage tuning, cooldowns, combat-tag restrictions, item/effect/enchant rules, grace-period flow, and utility/monitoring tools in one place.

Author: **_4rubka_**

## Compatibility

| Target | Status | Notes |
|---|---|---|
| Paper `1.21.10` | ✅ Supported | Recommended |
| Paper `1.21.11` | ✅ Supported | Recommended |
| Spigot `1.21.10-1.21.11` | ✅ Supported | Core features work |

## Main Modules

| Module | What it does | Default |
|---|---|---|
| Admin GUI (`/forgebalance`) | Multi-page in-game toggles for core/balance/combat/bans/cooldowns/utility | ✅ |
| Damage Balance | Global/PvP/PvE multipliers + per-weapon custom damage table | ✅ |
| Combat System | Combat tag, anti-restock, anti-elytra, anti-pearl, optional anti-escape rules | ✅ |
| Cooldowns | Mace, shield, wind charge, riptide, gapple and more (independent toggles) | ✅ |
| Item & Enchant Rules | Banned items, netherite policy, one-mace rule, enchant caps/sanitizer | ✅ |
| Effect Rules | Block selected potion effects with bypass list support | ✅ |
| SMP Start / Grace | `/smpstart` flow with temporary PvP/block/entity restrictions | ✅ |
| Warden Progression | Warden Heart drop + custom recipe + ritual interaction | ✅ |
| Utility & Monitoring | One-player-sleep, XP clumps, anti-xray heuristic, anti-Xaero checks, etc. | Mixed (mostly optional) |

## Commands

| Command | Description |
|---|---|
| `/forgebalance` | Open admin GUI (or show status if GUI disabled/console) |
| `/forgebalance menu` | Open admin GUI |
| `/forgebalance status` | Print module/grace status |
| `/forgebalance stats [player]` | Show combat hit/damage stats |
| `/forgebalance reload` | Reload config and clear runtime states |
| `/smpstart start [seconds]` | Start grace period |
| `/smpstart stop` | Stop grace period |
| `/smpstart status` | Show grace-period status |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `forgebalance.admin` | Access admin commands + menu | `op` |
| `forgebalance.bypass` | Bypass restrictions/limits | `false` |

## Why ForgeBalance

- One plugin covers most competitive SMP balance needs.
- Fully configurable via `config.yml` (large, documented config).
- Good for seasonal SMPs, private PvP servers, and tournament-style worlds.

## Install

1. Put the plugin `.jar` into `plugins/`.
2. Start/restart the server once to generate `config.yml`.
3. Tune settings and run `/forgebalance reload`.
