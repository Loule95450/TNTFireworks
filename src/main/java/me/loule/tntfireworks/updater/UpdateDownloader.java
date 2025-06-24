package me.loule.tntfireworks.updater;

import me.loule.tntfireworks.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class UpdateDownloader {
    private final Main plugin;
    private final String githubRepo;

    public UpdateDownloader(Main plugin, String githubRepo) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
    }

    /**
     * Télécharge et installe la dernière version du plugin
     * @param sender L'expéditeur de la commande pour les notifications
     * @param version La version à télécharger
     */
    public void downloadUpdate(CommandSender sender, String version) {
        sender.sendMessage("§6[TNTFireworks] §eDémarrage du téléchargement de la version " + version + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Construire l'URL de téléchargement
                String downloadUrl = "https://github.com/" + githubRepo + "/releases/download/" + 
                                     version + "-SNAPSHOT/TNTFireworks-" + version + "-SNAPSHOT.jar";

                // Obtenir le fichier du plugin actuel
                File currentPluginFile = getPluginFile();
                if (currentPluginFile == null) {
                    notifyError(sender, "Impossible de localiser le fichier jar du plugin.");
                    return;
                }

                // Télécharger le nouveau fichier dans un emplacement temporaire
                File tempFile = new File(currentPluginFile.getParentFile(), "TNTFireworks-" + version + "-SNAPSHOT.jar.download");
                if (tempFile.exists()) {
                    tempFile.delete();
                }

                // Télécharger le fichier
                downloadFile(downloadUrl, tempFile);

                // Préparer pour l'installation au redémarrage
                File updateFile = new File(currentPluginFile.getParentFile(), "TNTFireworks-" + version + "-SNAPSHOT.jar");
                if (updateFile.exists()) {
                    updateFile.delete();
                }

                // Renommer le fichier téléchargé
                if (!tempFile.renameTo(updateFile)) {
                    Files.move(tempFile.toPath(), updateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                // Notifier l'utilisateur
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§6[TNTFireworks] §aMise à jour téléchargée avec succès!");
                    sender.sendMessage("§6[TNTFireworks] §eLa mise à jour sera installée au prochain redémarrage du serveur.");
                    sender.sendMessage("§6[TNTFireworks] §eUtilisez §b/tntfireworks restart §epour redémarrer le serveur maintenant.");
                });

            } catch (Exception e) {
                e.printStackTrace();
                notifyError(sender, "Erreur lors du téléchargement: " + e.getMessage());
            }
        });
    }

    /**
     * Télécharge un fichier depuis une URL
     * @param fileUrl L'URL du fichier à télécharger
     * @param destination Le fichier de destination
     * @throws IOException Si une erreur se produit lors du téléchargement
     */
    private void downloadFile(String fileUrl, File destination) throws IOException {
        URL url = new URL(fileUrl);
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destination)) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }

    /**
     * Trouve le fichier jar du plugin actuel
     * @return Le fichier jar du plugin, ou null s'il n'a pas été trouvé
     */
    private File getPluginFile() {
        try {
            Plugin targetPlugin = Bukkit.getPluginManager().getPlugin("TNTFireworks");
            if (targetPlugin == null) return null;

            PluginDescriptionFile desc = targetPlugin.getDescription();
            String pluginClassName = targetPlugin.getClass().getName();
            String pluginClassFileName = pluginClassName.replace('.', '/') + ".class";

            URL url = targetPlugin.getClass().getClassLoader().getResource(pluginClassFileName);
            if (url == null) return null;

            String urlString = url.toString();
            String jarFilePath = urlString.substring("jar:file:/".length(), urlString.indexOf("!"));

            // Ajuster pour Windows/Unix
            if (System.getProperty("os.name").toLowerCase().contains("win") && jarFilePath.startsWith("/")) {
                jarFilePath = jarFilePath.substring(1);
            }

            return new File(jarFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Notifie l'utilisateur d'une erreur
     * @param sender L'expéditeur de la commande
     * @param message Le message d'erreur
     */
    private void notifyError(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage("§6[TNTFireworks] §c" + message);
        });
    }

    /**
     * Redémarre le serveur
     * @param sender L'expéditeur de la commande
     */
    public void restartServer(CommandSender sender) {
        sender.sendMessage("§6[TNTFireworks] §eRedémarrage du serveur dans 10 secondes...");

        // Annoncer le redémarrage à tous les joueurs
        Bukkit.broadcastMessage("§c[SERVEUR] §eRedémarrage du serveur dans 10 secondes!");

        // Programmer le redémarrage
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("§c[SERVEUR] §eRedémarrage du serveur maintenant!");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }, 20L);
        }, 200L);
    }
}
