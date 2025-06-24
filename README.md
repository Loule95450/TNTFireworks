# TNTFireworks

## Description

TNTFireworks is a Minecraft plugin that transforms TNT explosions into beautiful colorful fireworks without causing damage to surrounding blocks. This plugin is perfect for creative servers, special events, or simply to add an impressive visual touch to your Minecraft world.

## Features

- **Visual explosions without damage**: TNT explodes into colorful fireworks without destroying blocks
- **Low-altitude fireworks**: Fireworks are generated with minimal power to stay close to the ground
- **Random effects**: Each explosion generates multiple fireworks with random colors, shapes, and effects
- **Chain reactions**: Adjacent TNT blocks are automatically activated, creating cascades of visual effects
- **Fully configurable**: Customize every aspect of the plugin through the configuration file
- **Optimized performance**: Designed to have minimal impact on server performance

## Installation

1. Download the `.jar` file from the releases section
2. Place the file in the `plugins/` folder of your Minecraft server
3. Restart your server or use a plugin manager to load the plugin
4. (Optional) Configure the plugin by editing the `config.yml` file
5. Enjoy TNT explosions transformed into fireworks!

## Usage

Once the plugin is installed:

1. Place TNT blocks in your world
2. Ignite them as you normally would (flint and steel, redstone, etc.)
3. Admire the fireworks display!

For more impressive effects, try placing multiple TNT blocks side by side to create chain reactions.

## Configuration

The plugin comes with a detailed configuration file (`config.yml`) that allows you to customize every aspect:

```yaml
# Should regular TNT explosions be converted to fireworks?
tnt-explosions-enabled: true

# Should TNT minecart explosions be converted to fireworks?
tnt-minecart-explosions-enabled: true

# Should explosions cause block damage?
block-damage-enabled: false

# Should nearby TNT blocks be activated?
chain-reaction-enabled: true

# Radius to check for nearby TNT blocks
chain-reaction-radius: 3

# Firework settings (min/max count, colors, effects)
min-fireworks: 2
max-fireworks: 4
# ... and many more options!
```

## Commands

- `/tntfireworks` - Display plugin information
- `/tntfireworks reload` - Reload the configuration file (requires permission `tntfireworks.reload`)

## Permissions

- `tntfireworks.reload` - Allows reloading the plugin configuration (default: op)

## Compatibility

- **Minecraft Version**: 1.21+
- **Bukkit API**: 1.21
- **Compatible Servers**: Bukkit, Spigot, Paper, and other compatible forks

## Benefits for your server

- **Event decoration**: Perfect for celebrations, inaugurations, or special events
- **Risk-free construction**: Allows using TNT decoratively without fear of damage
- **Family-friendly environment**: Offers the fun of explosions without the associated destruction
- **Unique visual experience**: Adds a distinctive visual element to your server
- **Customizable**: Configure to match your server's specific needs

## Support and Contribution

For any questions, suggestions, or issues, feel free to open an issue on the project's GitHub repository.

## License

TNTFireworks is distributed under the MIT license. You are free to use, modify, and distribute it according to the terms of this license.

---

Developed with ❤️ by Loule
