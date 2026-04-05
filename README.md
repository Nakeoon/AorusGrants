# AorusGrants

A LuckPerms-based rank management GUI plugin for Paper/Spigot 1.20+.

## Features

- 5-menu GUI flow for granting/demoting ranks
- 100% LuckPerms API — no custom permission system
- Temporal grants with day/week/month accumulators
- Everything configurable from `menus.yml` and `config.yml`
- Session manager with auto-expiry
- Tab-complete for `/ag <player>`

## Requirements

- Paper/Spigot 1.20+
- Java 17+
- LuckPerms 5.4+

## Building

```bash
mvn clean package
```

The compiled jar will be in `target/AorusGrants-1.0.0.jar`.

## Installation

1. Drop `AorusGrants-1.0.0.jar` into your `plugins/` folder
2. Ensure LuckPerms is installed
3. Start/restart the server
4. Edit `plugins/AorusGrants/config.yml` to define your groups
5. Edit `plugins/AorusGrants/menus.yml` to customize slot layouts

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/ag <player>` | `aorusgrants.use` | Open the grant GUI for a player |
| `/ag reload` | `aorusgrants.admin` | Reload configuration files |

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `aorusgrants.use` | op | Use /ag command |
| `aorusgrants.admin` | op | Admin features (reload) |

## Menu Flow

```
/ag <player>
    └── MAIN MENU (slot 22: player head)
         ├── PROMOTE (slot 20) ──→ RANK SELECT ──→ DURATION ──→ CONFIRM ──→ execute
         ├── DEMOTE  (slot 24) ──→ CONFIRM ──→ execute
         └── INFO    (slot 31) ──→ INFO MENU (all groups as books)
```

## Configuring Groups

In `config.yml`, define your groups:

```yaml
groups:
  vip:
    display-name: "VIP"
    prefix: "&aVIP &r"
    weight: 100
    type: DONATOR          # DEFAULT | DONATOR | STAFF | HIDDEN
    material: LIME_WOOL    # Wool shown in rank select menu
```

## Configuring Menus

All menu slots, materials, names, and lore are in `menus.yml`.
No restart required — use `/ag reload` after editing.

## Placeholders

| Placeholder | Available in |
|------------|--------------|
| `{player}` | Any menu with player name |
| `{prefix}` | Main menu player head |
| `{groups}` | Main menu player head |
| `{group}` | Confirm menu |
| `{action}` | Confirm menu |
| `{duration}` | Duration confirm button, Confirm menu |
| `{amount}` | Duration day/week/month items |
