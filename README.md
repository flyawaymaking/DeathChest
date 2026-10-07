# DeathChest - Death Chest Plugin

A plugin for Minecraft servers that automatically creates a chest containing a player's items upon death.

* Русский перевод конфига расположен [ЗДЕСЬ](/src/main/resources/ru_config.yml)

## 🧩 Version Compatibility

| **Plugin version** | **Supported Paper** | **Java** |
|--------------------|---------------------|----------|
| `1.4.0+`           | `1.20` – `26.3`     | 25       |
| `1.3.2`            | `1.20` – `1.21.11`  | 21       |

## 📦 Features

- **Automatic Chest Creation** – Upon death, all player items are placed into a chest
- **Flexible Configuration** – Multiple settings for various needs
- **Chest Protection** – Explosion, fire, and piston protection
- **Permission System** – Flexible access management
- **WorldGuard Compatibility** – Chests can be opened in protected regions
- **Multi-World Support** – Can restrict chest creation in specific worlds
- **Holograms Above Chest** – Displays the owner's name (requires [DecentHolograms](https://github.com/DecentSoftware-eu/DecentHolograms))
- **Automatic Cleanup** – Periodic task removes expired chests

## ⚙️ Installation

1. Download the **latest version** from [Releases](../../releases)
2. Place the `.jar` file into your server's `plugins/` folder
3. Restart the server
4. Configure the config file to your liking
5. Reload the plugin with `/dchest reload` or restart the server

## 🎮 Commands

```
/deathchest list    – List your death chests
/deathchest reload  – Reload configuration and chests (requires permission)
/deathchest version – Show plugin version
/deathchest help    – Show help
```

Alias: `/dchest`

## 🔐 Permissions

```
deathchest.use    – Basic permission to use the plugin (default: true)
deathchest.reload – Reload configuration and chests (default: op)
deathchest.admin  – Administrator permissions (default: op)
deathchest.*      – All plugin permissions
```

## 📁 Configuration

The `config.yml` file is automatically created on first run:

```yaml
# DeathChest - Configuration

# Chest creation settings
chest-creation:
  # Create a chest only upon death by a mob
  mob-death-only: false
  # Can a chest spawn instead of a solid block
  allow-solid-block-spawn: false
  # Worlds where death chests are allowed (empty = all worlds)
  allowed-worlds: [ ]
  # Disabled worlds where death chests are turned off
  blacklisted-worlds: [ ]

# Chest interaction settings
chest-interactions:
  # Can players interact with others' death chests
  allow-access-others-chests: true
  # Can players break a death chest
  player-breakable: true
  # Is the chest protected from explosions
  explosion-proof: true
  # Do items drop when the chest is exploded
  items-drop-when-exploded: true
  # Do items drop when the chest is broken by a player
  items-drop-when-broken: true
  # Automatically remove empty chests
  remove-empty-chests: true

# Chest appearance settings
chest-appearance:
  # Custom chest title (supports MiniMessages)
  title: "<gradient:gold:white>Death Chest:</gradient> <gold>{player}"
  # Chest expiration time in minutes (0 = never expires)
  expiration-time: 1440  # Default 24 hours
  # Display hologram above chest (requires DecentHolograms)
  hologram-enabled: true
  # Does not support MiniMessages, use colors from https://wiki.decentholograms.eu/general/format-and-colors/colors/
  hologram:
    - "<#800000>☠ <#FFD700>Death Chest </#FFFFFF><#800000>☠"
    - "<#FFD700>Player: <#FFFFFF>{owner}"

# Time translation
time-ago:
  days: "days"
  hours: "hours"
  minutes: "minutes"

# Message prefix (supports MiniMessages)
prefix: "<gradient:gold:white>[DeathChest]</gradient>"

# Messages (supports MiniMessages)
messages:
  # commands
  player-only: "<red>This command can only be used by players."
  no-permission: "<red>You do not have permission to use this command"
  help: |
    <gradient:gold:white>=== DeathChest Commands ===
    <white>/deathchest list <gray>- List your death chests
    <white>/deathchest reload <gray>- Reload configuration (requires permission)
    <white>/deathchest version <gray>- Show plugin version
    <white>/deathchest help <gray>- Show this help
  version: "<white>DeathChest <yellow>v{version}"
  list-header: "<gradient:gold:white>=== Your Death Chests ===</gradient>"
  list-format: "<yellow>World: <white>{world} <yellow>X: <white>{x} <yellow>Y: <white>{y} <yellow>Z: <white>{z} <gray>({time} ago)"
  list-numbered: "<white>{number}. "
  no-chests: "<green>You have no active death chests"
  reload-success: "<green>Configuration reloaded!"
  # listeners
  chest-created: "<white>Your death chest was created at coordinates: <yellow>X: {x} Y: {y} Z: {z}."
  chest-accessed: "<white>You are opening the death chest of player: <yellow>{player}."
  access-denied: "<red>This death chest belongs to player: <yellow>{player}."
  cannot-break: "<red>You cannot break this death chest while it contains items!"
  chest-removed: "<green>The death chest disappeared because you took all items."
  chest-broken-own: "<white>You broke your own death chest."
  chest-broken-other: "<white>You broke the death chest of player <yellow>{player}."
```

## 🔧 Specificity

### Chest Protection
- **Explosion Protection** – Chests are protected from explosions (configurable)
- **Fire Protection** – Chests do not burn or catch fire
- **Piston Protection** – Pistons cannot move death chests
- **Region Access** – Chests can be opened even in protected WorldGuard regions

### Smart Creation
- **Location Search** – The plugin automatically searches for a suitable place for the chest
- **Item Check** – If a player has no items, a chest is not created
- **World Filter** – Can restrict chest creation in specific worlds

### Access Management
- **Ownership Rights** – Only the owner can open their chest (configurable)
- **Breaking Permission** – Ability to break chests can be configured
- **Auto-Removal** – Empty chests are automatically removed
- **Holograms** – Owner's name is displayed above the chest if enabled

### Automatic Management
- **Periodic Cleanup** – Expired chests are automatically removed

## 🐛 Bug Reports and Support

If you find a bug or have suggestions for improving the plugin, please create an [issue](../../issues).

## 📄 License

This plugin is distributed under the MIT License. You are free to use, modify, and distribute it.
