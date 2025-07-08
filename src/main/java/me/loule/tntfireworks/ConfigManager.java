package me.loule.tntfireworks;

import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final Main plugin;
    private FileConfiguration config;

    private boolean tntExplosionsEnabled;
    private boolean tntMinecartExplosionsEnabled;
    private boolean creeperExplosionsEnabled;
    private boolean blockDamageEnabled;
    private boolean creeperBlockDamageEnabled;
    private boolean protectDecorationEntities;
    private boolean chainReactionEnabled;
    private int chainReactionRadius;
    private int minFuseTicks;
    private int maxFuseTicks;
    private int minFireworks;
    private int maxFireworks;
    private int fireworkPower;
    private List<Color> fireworkColors;
    private boolean fireworkTrailEnabled;
    private boolean fireworkFlickerEnabled;
    private boolean randomizeFireworkEffects;

    private static final Map<String, Color> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put("RED", Color.RED);
        COLOR_MAP.put("BLUE", Color.BLUE);
        COLOR_MAP.put("GREEN", Color.GREEN);
        COLOR_MAP.put("YELLOW", Color.YELLOW);
        COLOR_MAP.put("PURPLE", Color.PURPLE);
        COLOR_MAP.put("WHITE", Color.WHITE);
        COLOR_MAP.put("ORANGE", Color.ORANGE);
        COLOR_MAP.put("LIME", Color.LIME);
        COLOR_MAP.put("AQUA", Color.AQUA);
        COLOR_MAP.put("BLACK", Color.BLACK);
        COLOR_MAP.put("GRAY", Color.GRAY);
        COLOR_MAP.put("NAVY", Color.NAVY);
        COLOR_MAP.put("TEAL", Color.TEAL);
        COLOR_MAP.put("OLIVE", Color.OLIVE);
        COLOR_MAP.put("MAROON", Color.MAROON);
        COLOR_MAP.put("SILVER", Color.SILVER);
        COLOR_MAP.put("FUCHSIA", Color.FUCHSIA);
    }

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();

        // Reload config from file
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load settings from config
        tntExplosionsEnabled = config.getBoolean("tnt-explosions-enabled", true);
        tntMinecartExplosionsEnabled = config.getBoolean("tnt-minecart-explosions-enabled", true);
        creeperExplosionsEnabled = config.getBoolean("creeper-explosions-enabled", true);
        creeperBlockDamageEnabled = config.getBoolean("creeper-block-damage-enabled", false);
        blockDamageEnabled = config.getBoolean("block-damage-enabled", false);
        protectDecorationEntities = config.getBoolean("protect-decoration-entities", true);
        chainReactionEnabled = config.getBoolean("chain-reaction-enabled", true);
        chainReactionRadius = config.getInt("chain-reaction-radius", 3);
        minFuseTicks = config.getInt("min-fuse-ticks", 10);
        maxFuseTicks = config.getInt("max-fuse-ticks", 19);
        minFireworks = config.getInt("min-fireworks", 2);
        maxFireworks = config.getInt("max-fireworks", 4);
        fireworkPower = config.getInt("firework-power", 0);
        fireworkTrailEnabled = config.getBoolean("firework-trail-enabled", true);
        fireworkFlickerEnabled = config.getBoolean("firework-flicker-enabled", true);
        randomizeFireworkEffects = config.getBoolean("randomize-firework-effects", true);

        // Load colors
        fireworkColors = new ArrayList<>();
        List<String> colorNames = config.getStringList("firework-colors");

        if (colorNames.isEmpty()) {
            // Default colors if none specified
            fireworkColors.add(Color.RED);
            fireworkColors.add(Color.BLUE);
            fireworkColors.add(Color.GREEN);
            fireworkColors.add(Color.YELLOW);
            fireworkColors.add(Color.PURPLE);
            fireworkColors.add(Color.WHITE);
            fireworkColors.add(Color.ORANGE);
            fireworkColors.add(Color.LIME);
            fireworkColors.add(Color.AQUA);
        } else {
            for (String colorName : colorNames) {
                Color color = COLOR_MAP.get(colorName.toUpperCase());
                if (color != null) {
                    fireworkColors.add(color);
                } else {
                    plugin.getLogger().warning("Unknown color in config: " + colorName);
                }
            }

            // If all colors were invalid, add defaults
            if (fireworkColors.isEmpty()) {
                plugin.getLogger().warning("No valid colors found in config, using defaults");
                fireworkColors.add(Color.RED);
                fireworkColors.add(Color.BLUE);
                fireworkColors.add(Color.GREEN);
            }
        }
    }

    // Getters for all config values
    public boolean isTntExplosionsEnabled() {
        return tntExplosionsEnabled;
    }

    public boolean isTntMinecartExplosionsEnabled() {
        return tntMinecartExplosionsEnabled;
    }

    public boolean isCreeperExplosionsEnabled() {
        return creeperExplosionsEnabled;
    }

    public boolean isCreeperBlockDamageEnabled() {
        return creeperBlockDamageEnabled;
    }

    public boolean isProtectDecorationEntities() {
        return protectDecorationEntities;
    }

    public boolean isBlockDamageEnabled() {
        return blockDamageEnabled;
    }

    public boolean isChainReactionEnabled() {
        return chainReactionEnabled;
    }

    public int getChainReactionRadius() {
        return chainReactionRadius;
    }

    public int getMinFuseTicks() {
        return minFuseTicks;
    }

    public int getMaxFuseTicks() {
        return maxFuseTicks;
    }

    public int getMinFireworks() {
        return minFireworks;
    }

    public int getMaxFireworks() {
        return maxFireworks;
    }

    public int getFireworkPower() {
        return fireworkPower;
    }

    public List<Color> getFireworkColors() {
        return fireworkColors;
    }

    public boolean isFireworkTrailEnabled() {
        return fireworkTrailEnabled;
    }

    public boolean isFireworkFlickerEnabled() {
        return fireworkFlickerEnabled;
    }

    public boolean isRandomizeFireworkEffects() {
        return randomizeFireworkEffects;
    }
}
