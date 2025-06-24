package me.loule.tntfireworks.updater;

import me.loule.tntfireworks.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Update manager for TNTFireworks
 */
public class UpdateManager {
    private final Main plugin;
    private final UpdateChecker updateChecker;
    private final UpdateDownloader updateDownloader;
    private final String GITHUB_REPO = "Loule95450/TNTFireworks";

    private boolean updateAvailable = false;

    public UpdateManager(Main plugin) {
        this.plugin = plugin;
        this.updateChecker = new UpdateChecker(plugin, GITHUB_REPO);
        this.updateDownloader = new UpdateDownloader(plugin, GITHUB_REPO);
    }

    /**
     * Initializes the update manager
     */
    public void initialize() {
        if (plugin.getConfig().getBoolean("check-updates", true)) {
            // Check for updates asynchronously
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                updateAvailable = updateChecker.checkForUpdates();
            });

            // Periodically check for updates (every 6 hours)
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                updateAvailable = updateChecker.checkForUpdates();
            }, 20L * 60L * 60L * 6L, 20L * 60L * 60L * 6L); // Initial delay and period in ticks
        }
    }

    /**
     * Notifies administrators of an available update on join
     * @param player The player who just joined
     */
    public void notifyOnJoin(Player player) {
        if (updateAvailable && player.hasPermission("tntfireworks.update")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§6[TNTFireworks] §eA new version is available: §b" + updateChecker.getLatestVersionString());
                player.sendMessage("§6[TNTFireworks] §eUse §b/tntfireworks update §eto update the plugin.");
            }, 40L); // Short delay to ensure the player sees the message after login
        }
    }

    /**
     * Checks for updates and notifies the sender
     * @param sender The command sender
     */
    public void checkForUpdates(CommandSender sender) {
        sender.sendMessage("§6[TNTFireworks] §eChecking for updates...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean hasUpdate = updateChecker.checkForUpdates();
            updateAvailable = hasUpdate;

            if (hasUpdate) {
                sender.sendMessage("§6[TNTFireworks] §eA new version is available: §b" + updateChecker.getLatestVersionString());
                sender.sendMessage("§6[TNTFireworks] §eUse §b/tntfireworks update §eto update the plugin.");
            } else {
                sender.sendMessage("§6[TNTFireworks] §aYou are using the latest version of the plugin.");
            }
        });
    }

    /**
     * Downloads and installs the latest version of the plugin
     * @param sender The command sender
     * @param confirmed Whether the user has confirmed the update
     */
    public void updatePlugin(CommandSender sender, boolean confirmed) {
        // First, always check for updates
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean hasUpdate = updateChecker.checkForUpdates();
            updateAvailable = hasUpdate;

            if (!hasUpdate) {
                sender.sendMessage("§6[TNTFireworks] §aYou are already using the latest version of the plugin.");
                return;
            }

            if (confirmed) {
                // Download and restart
                updateDownloader.downloadAndRestart(sender, updateChecker.getLatestVersionString());
            } else {
                // Ask for confirmation
                sender.sendMessage("§6[TNTFireworks] §eA new version is available: §b" + updateChecker.getLatestVersionString());
                sender.sendMessage("§6[TNTFireworks] §eTo update, please run §b/tntfireworks update confirm");
                sender.sendMessage("§6[TNTFireworks] §cThis will download the update and restart the server.");
            }
        });
    }

    /**
     * @return true if an update is available
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
}
