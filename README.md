# BungeeAFK

BungeeAFK is a cross-platform plugin for **BungeeCord**, **Velocity**, and **Spigot/Paper** servers that detects inactive (AFK) players. If a player is idle for a configurable amount of time, the plugin can either **kick them** or **move them to a separate AFK server** within your Bungee network.

## ✨ Features

- ⚙️ Fully configurable timeout and action
- 🔀 Support for **kick** or **move-to-server** actions
- 🌐 Works with BungeeCord, Velocity, and Spigot
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
# Available languages: English (en) & German (de)
lang: en

# Delay after which the warning message is sent to the player (seconds) | Lang entry: "notification.afk_warning"
# e.g., if set to 300, the player will receive a warning message after 5 minutes of inactivity
warning-delay: 300

# Delay after which a player is marked as AFK (seconds)
# e.g., if set to 600, the player will be marked as AFK after 10 minutes of inactivity
afk-delay: 600

# Delay after which a player marked as AFK is connected to the AFK server (seconds)
# e.g., if set to 630, the player will be connected to the AFK server or kicked after 10 minutes and 30 seconds of inactivity
action-delay: 630

# Action to be performed after action delay is reached. Possible values: "kick", "connect", "nothing".
# "kick" - player is kicked from the server
# "connect" - player is connected to the server specified in the "afk-server-name" option
# "nothing" - nothing happens
action: "kick"

# Server name to which the player is connected when the action is set to "connect"
# !!! Only available for BungeeCord and Velocity !!!
afk-server-name: "afk"

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

- `/bafk configure <action | action-delay | afk-delay | allow-bypass | warn-delay | caption | reloadconfig> <value>`
- `/bafk lang <en | de | reload>`

---

## 📥 Downloads

Download from the [Latest Release](https://github.com/Fameless9/BungeeAFK/releases/latest).

---

## 🧠 License

Licensed under General Public License v3.0. See [LICENSE](./LICENSE) for details.
