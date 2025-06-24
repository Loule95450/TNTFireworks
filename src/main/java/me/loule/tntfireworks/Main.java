package me.loule.tntfireworks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class Main extends JavaPlugin implements Listener {

    private final Random random = new Random();
    private ConfigManager configManager;
    private FireworkManager fireworkManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize managers
        configManager = new ConfigManager(this);
        fireworkManager = new FireworkManager(configManager);

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register tab completer
        getCommand("tntfireworks").setTabCompleter(new CommandTabCompleter());

        getLogger().info("[TNTFireworks] Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("[TNTFireworks] Plugin disabled successfully!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("tntfireworks")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("tntfireworks.reload")) {
                    configManager.loadConfig();
                    sender.sendMessage("§a[TNTFireworks] Configuration reloaded successfully!");
                    return true;
                } else {
                    sender.sendMessage("§c[TNTFireworks] You don't have permission to use this command.");
                    return true;
                }
            }

            sender.sendMessage("§6[TNTFireworks] §fVersion: §e" + getDescription().getVersion());
            if (sender.hasPermission("tntfireworks.reload")) {
                sender.sendMessage("§6[TNTFireworks] §fUse /tntfireworks reload to reload the configuration.");
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onTNTExplode(EntityExplodeEvent event) {
        EntityType entityType = event.getEntityType();

        // Handle block damage separately for all TNT and TNT Minecart explosions
        // regardless of fireworks conversion settings
        if ((entityType == EntityType.TNT || entityType == EntityType.TNT_MINECART) && 
            !configManager.isBlockDamageEnabled()) {
            event.blockList().clear();
            event.setYield(0);
        }

        // Check if this explosion type should be processed for fireworks
        boolean shouldProcess = false;

        if (entityType == EntityType.TNT && configManager.isTntExplosionsEnabled()) {
            shouldProcess = true;
        } else if (entityType == EntityType.TNT_MINECART && configManager.isTntMinecartExplosionsEnabled()) {
            shouldProcess = true;
        }

        if (shouldProcess) {
            Location location = event.getLocation();

            // Check for chain reactions if enabled
            if (configManager.isChainReactionEnabled()) {
                checkAndPrimeTNT(location, configManager.getChainReactionRadius());
            }

            // Spawn fireworks
            fireworkManager.spawnFireworks(location);
        }
    }

    private void checkAndPrimeTNT(Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location blockLoc = center.clone().add(x, y, z);
                    Block block = blockLoc.getBlock();

                    if (block.getType() == Material.TNT) {
                        // Replace TNT block with primed TNT
                        block.setType(Material.AIR);
                        TNTPrimed primedTNT = (TNTPrimed) center.getWorld().spawnEntity(
                                blockLoc.clone().add(0.5, 0.5, 0.5), 
                                EntityType.TNT
                        );

                        // Set random fuse time within configured range
                        int minTicks = configManager.getMinFuseTicks();
                        int maxTicks = configManager.getMaxFuseTicks();
                        int fuseTicks = (minTicks == maxTicks) ? minTicks : 
                                minTicks + random.nextInt(maxTicks - minTicks + 1);

                        primedTNT.setFuseTicks(fuseTicks);
                    }
                }
            }
        }
    }
}