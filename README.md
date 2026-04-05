# ⚡ AorusGrants

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Spigot%20%7C%20Paper-gold.svg)
![Java](https://img.shields.io/badge/java-17%2B-orange.svg)

> **AorusGrants** is a modern, GUI-based rank management plugin powered by **LuckPerms**. Designed for fast, intuitive, and fully configurable staff workflows.

## ✨ Features

  - 🧩 **5-Step GUI Flow** for granting and demoting ranks.
  - 🔒 **100% LuckPerms API Integration** — no custom permission system needed.
  - ⏳ **Temporary Grants** with day/week/month accumulators.
  - ⚙️ **Fully Configurable Menus** via `menus.yml`.
  - 📊 **Session Manager** with automatic expiration to prevent errors.
  - ⚡ **Tab-Complete Support** for `/ag <player>`.
  - 🎨 **Customization Ready** — edit prefixes, materials, and layouts.

-----

## 📸 Preview

> *(Add screenshots here — visual previews are critical for GUI plugins)*

-----

## 📦 Requirements

| Requirement | Minimum Version |
| :--- | :--- |
| **Software** | Paper / Spigot 1.20+ |
| **Java** | Java 17+ |
| **Dependency** | LuckPerms 5.4+ |

-----

## 🚀 Installation

1.  Download the latest **.jar** file.
2.  Place it in your server's `plugins/` folder.
3.  Ensure **LuckPerms** is installed.
4.  Start or restart your server.
5.  Configure your settings in:
      - `plugins/AorusGrants/config.yml`
      - `plugins/AorusGrants/menus.yml`

-----

## 🛠️ Building

```bash
mvn clean package
```

**Output:** `target/AorusGrants-1.0.0.jar`

-----

## 📜 Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/ag <player>` | `aorusgrants.use` | Open the GUI for managing ranks |
| `/ag reload` | `aorusgrants.admin` | Reload all configuration files |

-----

## 🧭 Menu Flow

The interface follows a strict logical path to ensure safety and speed:

```mermaid
graph TD
    A[/ag player] --> B{MAIN MENU}
    B --> C[PROMOTE]
    B --> D[DEMOTE]
    B --> E[INFO]
    
    C --> C1[Select Rank] --> C2[Select Duration] --> C3[Confirm] --> C4((EXECUTE))
    D --> D1[Confirm] --> D2((EXECUTE))
    E --> E1[View All Groups]
```

-----

## ⚙️ Configuration

### Groups (`config.yml`)

```yaml
groups:
  vip:
    display-name: "VIP"
    prefix: "&aVIP &r"
    weight: 100
    type: DONATOR          # Options: DEFAULT | DONATOR | STAFF | HIDDEN
    material: LIME_WOOL
```

### Menus (`menus.yml`)

Fully customizable layouts. You can modify:

  * Slots and Item Materials.
  * Custom Display Names and Lore.
  * Execution logic per item.

> Apply changes instantly with `/ag reload`

-----

## 🔤 Placeholders

| Placeholder | Description |
| :--- | :--- |
| `{player}` | Target player name |
| `{prefix}` | Player's current prefix |
| `{groups}` | Player's current groups |
| `{group}` | Selected group in the GUI |
| `{action}` | Current action (Promote / Demote) |
| `{duration}` | Selected duration string |
| `{amount}` | Numerical time amount |

-----

## 🎯 Why AorusGrants?

Unlike traditional rank plugins:

  * ✅ **No Command Spam:** Avoid typing long LuckPerms strings.
  * ✅ **Intuitive Workflow:** Clean GUI steps reduce staff training time.
  * ✅ **Safety First:** Confirmation screens prevent accidental "fat-finger" demotions.
  * ✅ **Native Performance:** Lightweight implementation with no overhead.

-----

## 📌 Roadmap

  - [ ] Rank history GUI viewer.
  - [ ] MySQL / Remote database support.
  - [ ] Menu animations & sound effects.
  - [ ] Advanced logging (Discord Webhooks).

-----

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss your ideas.

-----

## 📄 License

This project is **FREE**.

-----

## ⭐ Support

If you like this project, please consider giving the repository a **star**\!
