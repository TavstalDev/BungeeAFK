# BungeeAFK

BungeeAFK is a cross-platform plugin for **BungeeCord**, **Velocity**, and **Spigot/Paper** servers that detects inactive (AFK) players. If a player is idle for a configurable amount of time, the plugin can either **kick them**, **teleport them** or **move them to a separate AFK server** within your Bungee network.

## ✨ Features

- ⚙️ Fully configurable timeout and action
- 🔀 Support for **kick**, **teleport** or **move-to-server** actions
- 🌐 Compatible with BungeeCord, Velocity and Spigot, as well as their forks.
- 📦 Lightweight and easy to install
- 🔗 Detects AFK via movement, chat, interaction, etc.

> Note: The **"move" action is not available on Spigot-only setups** — it requires a Bungee/Velocity network.

---

## 📦 Installation

### 1. Main Plugin

Install `BungeeAFK`:
- put the BungeeAFK-Bungee, Velocity or Spigot version in the plugins folder of the network/main server

### 2. Tracking Plugin (Required on subservers)
- to detect player activity accurately, **every subserver in the network must also have**:

```
BungeeAFK-Tracking
```

- Download `BungeeAFK-Tracking`
- Place it in the `plugins` folder of each Spigot/Paper server
- This allows BungeeAFK to track movement, chat, interaction, etc.

---

## 🛠️ Configuration

In the `config.yml`, you can customize:

```yaml
# Language used for messages and notifications
# Available languages: en, de
lang: en

# Delay after which the warning message is sent to the player (seconds) | Lang entry: "notification.afk_warning"
# e.g., if set to 90, the player will receive a warning message after 1 minute and 30 seconds of inactivity
warning-delay: 90

# Delay after which a player is marked as AFK (seconds)
# e.g., if set to 180, the player will be marked as AFK after 3 minutes of inactivity
afk-delay: 180

# Delay after which a player marked as AFK is connected to the AFK server (seconds)
# e.g., if set to 420, the player will be connected to the AFK server or kicked after 7 minutes of inactivity
action-delay: 420

# Action to be performed after action delay is reached. Possible values: "kick", "connect", "nothing".
# "kick" - player is kicked from the server
# "connect" - player is connected to the server specified in the "afk-server-name" option
# "teleport" - player is teleported to the afk-location as configured below
# "nothing" - nothing happens
action: "kick"

# Server name to which the player is connected when the action is set to "connect"
# !!! Only available for BungeeCord and Velocity !!!
afk-server-name: "afk"

# AFK Location configuration
# If the action is set to "teleport", the player will be teleported to this location
afk-location:
  world: "world"  # World name where the AFK location is located
  x: 0.0          # X coordinate of the AFK location
  y: 100.0        # Y coordinate of the AFK location
  z: 0.0          # Z coordinate of the AFK location

# Whether to allow bypass of AFK detection for players with the "afk.bypass" permission
allow-bypass: true
```

Make sure the AFK server (`afk`) exists in your BungeeCord/Velocity `config.yml`!

- You can also Customize the Messages: [Customize Messages and Captions](https://github.com/Fameless9/BungeeAFK/wiki/Custom-Messages)
- More about custumizing the plugin: [How to Configure](https://github.com/Fameless9/BungeeAFK/wiki/How-to-Configure)

---

## 🚫 Limitations

- The **connect** feature only works on **networks (BungeeCord, etc.)** not on standalone spigot servers.
- You **must install** `BungeeAFK-Tracking` on **all subservers** managed by the BungeeCord/Velovity network for AFK detection to work properly.

---

## 🔗 Compatibility

| Platform     | Supported |
|--------------|-----------|
| BungeeCord   | ✅        |
| Velocity     | ✅        |
| Spigot       | ✅        |
| Paper        | ✅        |
| Purpur       | ✅        |
| Waterfall    | ✅        |

---

## 📣 Commands
You can configure the plugin using the `/bungeeafk` or `/bafk` command.

- `/bafk configure <action | action-delay | afk-delay | allow-bypass | warn-delay | afk-location | caption | reloadconfig> <value>`
- `/bafk lang <en | de | reload>`

---

## 📥 Downloads

Download from the [Latest Release](https://github.com/Fameless9/BungeeAFK/releases/latest).

---

## 🧠 License

Licensed under General Public License v3.0. See [LICENSE](./LICENSE) for details.
