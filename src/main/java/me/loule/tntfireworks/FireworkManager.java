package me.loule.tntfireworks;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;
import java.util.Random;

public class FireworkManager {
    private final ConfigManager configManager;
    private final Random random;

    public FireworkManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.random = new Random();
    }

    /**
     * Spawns fireworks at the specified location based on configuration settings
     * @param location Location to spawn fireworks
     */
    public void spawnFireworks(Location location) {
        // Calculate how many fireworks to spawn
        int min = configManager.getMinFireworks();
        int max = configManager.getMaxFireworks();
        int count = (min == max) ? min : min + random.nextInt(max - min + 1);

        // Spawn the fireworks
        for (int i = 0; i < count; i++) {
            spawnSingleFirework(location);
        }
    }

    /**
     * Spawns a single firework with random or configured properties
     * @param location Location to spawn the firework
     */
    private void spawnSingleFirework(Location location) {
        // Create firework entity
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();

        // Select firework type and colors
        FireworkEffect.Type effectType;
        boolean useTrail;
        boolean useFlicker;

        if (configManager.isRandomizeFireworkEffects()) {
            // Random type
            FireworkEffect.Type[] types = FireworkEffect.Type.values();
            effectType = types[random.nextInt(types.length)];

            // Random trail and flicker
            useTrail = configManager.isFireworkTrailEnabled() && random.nextBoolean();
            useFlicker = configManager.isFireworkFlickerEnabled() && random.nextBoolean();
        } else {
            // Default type if not randomized
            effectType = FireworkEffect.Type.BALL;
            useTrail = configManager.isFireworkTrailEnabled();
            useFlicker = configManager.isFireworkFlickerEnabled();
        }

        // Get colors from config
        List<Color> availableColors = configManager.getFireworkColors();
        Color mainColor = availableColors.get(random.nextInt(availableColors.size()));
        Color fadeColor = availableColors.get(random.nextInt(availableColors.size()));

        // Build the firework effect
        FireworkEffect effect = FireworkEffect.builder()
                .with(effectType)
                .withColor(mainColor)
                .withFade(fadeColor)
                .trail(useTrail)
                .flicker(useFlicker)
                .build();

        // Apply the effect and power
        meta.addEffect(effect);
        meta.setPower(configManager.getFireworkPower());

        // Update the firework with our settings
        firework.setFireworkMeta(meta);
    }
}
