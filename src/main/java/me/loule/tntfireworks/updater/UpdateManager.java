package me.loule.tntfireworks.updater;

import me.loule.tntfireworks.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Gestionnaire des mises à jour pour TNTFireworks
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
     * Initialise le gestionnaire de mise à jour
     */
    public void initialize() {
        if (plugin.getConfig().getBoolean("check-updates", true)) {
            // Vérifier les mises à jour de manière asynchrone
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                updateAvailable = updateChecker.checkForUpdates();
            });

            // Vérifier les mises à jour périodiquement (toutes les 6 heures)
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                updateAvailable = updateChecker.checkForUpdates();
            }, 20L * 60L * 60L * 6L, 20L * 60L * 60L * 6L); // Délai initial et période en ticks
        }
    }

    /**
     * Notifie les administrateurs d'une mise à jour disponible lors de leur connexion
     * @param player Le joueur qui vient de se connecter
     */
    public void notifyOnJoin(Player player) {
        if (updateAvailable && player.hasPermission("tntfireworks.update")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§6[TNTFireworks] §eUne nouvelle version est disponible: §b" + updateChecker.getLatestVersionString());
                player.sendMessage("§6[TNTFireworks] §eUtilisez §b/tntfireworks update §epour mettre à jour le plugin.");
            }, 40L); // Délai court pour s'assurer que le joueur voit le message après la connexion
        }
    }

    /**
     * Vérifie les mises à jour et notifie l'expéditeur
     * @param sender L'expéditeur de la commande
     */
    public void checkForUpdates(CommandSender sender) {
        sender.sendMessage("§6[TNTFireworks] §eVérification des mises à jour...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean hasUpdate = updateChecker.checkForUpdates();
            updateAvailable = hasUpdate;

            if (hasUpdate) {
                sender.sendMessage("§6[TNTFireworks] §eUne nouvelle version est disponible: §b" + updateChecker.getLatestVersionString());
                sender.sendMessage("§6[TNTFireworks] §eUtilisez §b/tntfireworks update §epour mettre à jour le plugin.");
            } else {
                sender.sendMessage("§6[TNTFireworks] §aVous utilisez la dernière version du plugin.");
            }
        });
    }

    /**
     * Télécharge et installe la dernière version du plugin
     * @param sender L'expéditeur de la commande
     */
    public void updatePlugin(CommandSender sender) {
        if (!updateAvailable) {
            // Vérifier si une mise à jour est disponible avant de télécharger
            sender.sendMessage("§6[TNTFireworks] §eVérification des mises à jour avant le téléchargement...");

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean hasUpdate = updateChecker.checkForUpdates();
                updateAvailable = hasUpdate;

                if (hasUpdate) {
                    // Télécharger la mise à jour
                    updateDownloader.downloadUpdate(sender, updateChecker.getLatestVersionString());
                } else {
                    sender.sendMessage("§6[TNTFireworks] §aVous utilisez déjà la dernière version du plugin.");
                }
            });
        } else {
            // Télécharger directement si une mise à jour est déjà connue
            updateDownloader.downloadUpdate(sender, updateChecker.getLatestVersionString());
        }
    }

    /**
     * Redémarre le serveur
     * @param sender L'expéditeur de la commande
     */
    public void restartServer(CommandSender sender) {
        updateDownloader.restartServer(sender);
    }

    /**
     * @return true si une mise à jour est disponible
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
}
