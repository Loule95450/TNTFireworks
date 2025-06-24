package me.loule.tntfireworks;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class TNTFireworks extends JavaPlugin implements Listener {

    private final Random random = new Random();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[TNTFireworks] Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("[TNTFireworks] Plugin disabled successfully!");
    }

    @EventHandler
    public void onTNTExplode(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.TNT || event.getEntityType() == EntityType.TNT_MINECART) {
            Location location = event.getLocation();

            // Vérifie les blocs de TNT dans un rayon de 3 blocs
            checkAndPrimeTNT(location, 3);

            // Supprime les dégâts sur les blocs pour éviter la destruction
            event.blockList().clear();

            // Empêche les dommages sur les entités
            event.setYield(0);

            // Nombre de feux d'artifice à générer (entre 2 et 4)
            int fireworkCount = random.nextInt(3) + 2;

            for (int i = 0; i < fireworkCount; i++) {
                spawnRandomFirework(location);
            }
        }
    }

    private void checkAndPrimeTNT(Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location blockLoc = center.clone().add(x, y, z);
                    Block block = blockLoc.getBlock();

                    if (block.getType() == Material.TNT) {
                        // Remplace le bloc TNT par un TNT amorcé
                        block.setType(Material.AIR);
                        TNTPrimed primedTNT = (TNTPrimed) center.getWorld().spawnEntity(blockLoc.clone().add(0.5, 0.5, 0.5), EntityType.TNT);
                        primedTNT.setFuseTicks(10 + random.nextInt(10)); // Délai aléatoire entre 10 et 19 ticks
                    }
                }
            }
        }
    }

    private void spawnRandomFirework(org.bukkit.Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();

        // Sélectionne un type d'effet aléatoire
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        FireworkEffect.Type effectType = types[random.nextInt(types.length)];

        // Sélectionne des couleurs aléatoires
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.PURPLE,
                Color.WHITE, Color.ORANGE, Color.LIME, Color.AQUA};
        Color mainColor = colors[random.nextInt(colors.length)];
        Color fadeColor = colors[random.nextInt(colors.length)];

        // Configure l'effet du feu d'artifice
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(mainColor)
                .withFade(fadeColor)
                .with(effectType)
                .trail(random.nextBoolean()) // Ajoute ou non un sillage
                .flicker(random.nextBoolean()) // Ajoute ou non un scintillement
                .build();

        meta.addEffect(effect);
        meta.setPower(0); // Puissance 0 pour limiter la hauteur à 1 bloc
        firework.setFireworkMeta(meta);
    }
}