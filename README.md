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

# Time in seconds before a player is considered AFK
afk-timeout: 300

# Action to perform: "kick", "connect" or "nothing"
action: "connect"

# (Only used for connect) The target server name as defined in your Bungee config
afk-server: "afk"
```

Make sure the AFK server (`afk`) exists in your BungeeCord/Velocity `config.yml`!

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

- `/bafk configure <action | action-delay | afk-delay> <param>`
- `/bafk lang <en | de>`

---

## 📥 Downloads

Download from the [Latest Release](https://github.com/Fameless9/BungeeAFK/releases/latest).

---

## 🧠 License

Licensed under General Public License v3.0. See [LICENSE](./LICENSE) for details.
