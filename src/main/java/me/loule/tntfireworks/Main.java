package me.loule.tntfireworks;

import me.loule.tntfireworks.updater.UpdateManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {

    private final Random random = new Random();
    private ConfigManager configManager;
    private FireworkManager fireworkManager;
    private UpdateManager updateManager;

    @Override
    public void onEnable() {
        // Save default config or update existing config with new options
        saveDefaultConfig();
        
        // Update the config to add new options without losing existing values
        ConfigUpdater configUpdater = new ConfigUpdater(this);
        if (configUpdater.updateConfig()) {
            getLogger().info("[TNTFireworks] Configuration updated with new options.");
        }

        // Check and clean up old update files
        cleanupUpdateFiles();

        // Initialize managers
        configManager = new ConfigManager(this);
        fireworkManager = new FireworkManager(configManager);
        updateManager = new UpdateManager(this);

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register tab completer
        getCommand("tntfireworks").setTabCompleter(new CommandTabCompleter());

        // Initialize update checker
        updateManager.initialize();

        getLogger().info("[TNTFireworks] Plugin enabled successfully!");
        getLogger().info("[TNTFireworks] Current version: " + getDescription().getVersion());
    }

    /**
     * Cleans up obsolete update files
     */
    private void cleanupUpdateFiles() {
        try {
            File pluginsDir = getDataFolder().getParentFile();
            if (pluginsDir != null && pluginsDir.exists()) {
                // Get current version
                String currentVersion = getDescription().getVersion();
                getLogger().info("Current version during cleanup: " + currentVersion);

                // Delete the update marker
                File updateMarker = new File(pluginsDir, "TNTFireworks.update");
                if (updateMarker.exists()) {
                    updateMarker.delete();
                    getLogger().info("Update marker deleted");
                }

                // Delete temporary .download files
                File[] downloadFiles = pluginsDir.listFiles((dir, name) -> name.startsWith("TNTFireworks-") && name.endsWith(".download"));
                if (downloadFiles != null) {
                    for (File file : downloadFiles) {
                        file.delete();
                        getLogger().info("Temporary file deleted: " + file.getName());
                    }
                }

                // Delete old plugin versions
                File[] oldVersions = pluginsDir.listFiles((dir, name) ->
                    name.startsWith("TNTFireworks-") &&
                    name.endsWith(".jar") &&
                    !name.contains(currentVersion));

                if (oldVersions != null && oldVersions.length > 0) {
                    for (File oldFile : oldVersions) {
                        String fileName = oldFile.getName();
                        // Do not delete the currently used file
                        if (!isCurrentlyUsed(oldFile)) {
                            if (oldFile.delete()) {
                                getLogger().info("Old version deleted: " + fileName);
                            } else {
                                // If immediate deletion fails, schedule deletion on next startup
                                oldFile.deleteOnExit();
                                getLogger().info("Old version marked for deletion on next startup: " + fileName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error cleaning up update files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if a file is currently being used by the running plugin
     * @param file The file to check
     * @return true if the file is currently in use
     */
    private boolean isCurrentlyUsed(File file) {
        try {
            // Try to move the file temporarily (will fail if locked)
            File tempFile = new File(file.getAbsolutePath() + ".temp");
            boolean canMove = file.renameTo(tempFile);
            if (canMove) {
                // Move the file back to its original location
                tempFile.renameTo(file);
                return false;
            }
            return true;
        } catch (Exception e) {
            // In case of an error, assume the file is in use
            return true;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("[TNTFireworks] Plugin disabled successfully!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Notify administrators of available updates
        Player player = event.getPlayer();
        if (player.hasPermission("tntfireworks.update")) {
            updateManager.notifyOnJoin(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("tntfireworks")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("tntfireworks.reload")) {
                        configManager.loadConfig();
                        sender.sendMessage("§a[TNTFireworks] Configuration reloaded successfully!");
                        return true;
                    } else {
                        sender.sendMessage("§c[TNTFireworks] You don't have permission to use this command.");
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("update")) {
                    if (sender.hasPermission("tntfireworks.update")) {
                        boolean confirmed = args.length == 2 && args[1].equalsIgnoreCase("confirm");
                        updateManager.updatePlugin(sender, confirmed);
                        return true;
                    } else {
                        sender.sendMessage("§c[TNTFireworks] You don't have permission to use this command.");
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("check")) {
                    if (sender.hasPermission("tntfireworks.update")) {
                        updateManager.checkForUpdates(sender);
                        return true;
                    } else {
                        sender.sendMessage("§c[TNTFireworks] You don't have permission to use this command.");
                        return true;
                    }
                }
            }

            // Display plugin information
            sender.sendMessage("§6[TNTFireworks] §fVersion: §e" + getDescription().getVersion());

            // Display available commands based on permissions
            if (sender.hasPermission("tntfireworks.reload")) {
                sender.sendMessage("§6[TNTFireworks] §f/tntfireworks reload §7- §fReload the configuration");
            }
            if (sender.hasPermission("tntfireworks.update")) {
                sender.sendMessage("§6[TNTFireworks] §f/tntfireworks check §7- §fCheck for updates");
                sender.sendMessage("§6[TNTFireworks] §f/tntfireworks update §7- §fDownload the latest version");
            }

            // Display if an update is available
            if (updateManager.isUpdateAvailable() && sender.hasPermission("tntfireworks.update")) {
                sender.sendMessage("§6[TNTFireworks] §eAn update is available! Use §b/tntfireworks update §eto update.");
            }

            return true;
        }
        return false;
    }

    @EventHandler
    public void onTNTExplode(EntityExplodeEvent event) {
        EntityType entityType = event.getEntityType();

        // Handle block damage separately based on entity type
        if ((entityType == EntityType.TNT || entityType == EntityType.TNT_MINECART) && 
            !configManager.isBlockDamageEnabled()) {
            event.blockList().clear();
            event.setYield(0);
        } else if (entityType == EntityType.CREEPER && 
                  !configManager.isCreeperBlockDamageEnabled()) {
            event.blockList().clear();
            event.setYield(0);
        }

        // Check if this explosion type should be processed for fireworks
        boolean shouldProcess = false;

        if (entityType == EntityType.TNT && configManager.isTntExplosionsEnabled()) {
            shouldProcess = true;
        } else if (entityType == EntityType.TNT_MINECART && configManager.isTntMinecartExplosionsEnabled()) {
            shouldProcess = true;
        } else if (entityType == EntityType.CREEPER && configManager.isCreeperExplosionsEnabled()) {
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

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if the damage is caused by an explosion
        if (event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
            EntityType entityType = event.getEntityType();
            
            // Protect item frames and armor stands if enabled
            if (configManager.isProtectDecorationEntities() && 
                (entityType == EntityType.ITEM_FRAME || entityType == EntityType.GLOW_ITEM_FRAME || 
                 entityType == EntityType.ARMOR_STAND)) {
                event.setCancelled(true);
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