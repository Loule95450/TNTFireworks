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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;

public class UpdateDownloader {
    private final Main plugin;
    private final String githubRepo;

    public UpdateDownloader(Main plugin, String githubRepo) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
    }

    /**
     * Downloads and installs the latest version of the plugin
     * @param sender The sender of the command for notifications
     * @param version The version to download
     */
    public void downloadUpdate(CommandSender sender, String version) {
        sender.sendMessage("§6[TNTFireworks] §eStarting download of version " + version + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Build the download URL
                String downloadUrl = "https://github.com/" + githubRepo + "/releases/download/" + 
                                     version + "-SNAPSHOT/TNTFireworks-" + version + "-SNAPSHOT.jar";

                plugin.getLogger().info("Download URL: " + downloadUrl);

                // Get the current plugin file
                File currentPluginFile = getPluginFile();
                if (currentPluginFile == null) {
                    notifyError(sender, "Could not locate the plugin jar file.");
                    return;
                }

                plugin.getLogger().info("Current plugin file: " + currentPluginFile.getAbsolutePath());

                // Create a temporary file in the plugins folder
                File pluginsDir = new File("plugins");
                if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
                    pluginsDir = currentPluginFile.getParentFile();
                }

                // Update file name
                String updateFileName = "TNTFireworks-" + version + "-SNAPSHOT.jar";

                // Download the new file directly into the plugins folder
                File updateFile = new File(pluginsDir, updateFileName);
                File tempFile = new File(pluginsDir, updateFileName + ".download");

                // Delete existing files
                if (tempFile.exists()) {
                    tempFile.delete();
                }

                if (updateFile.exists()) {
                    updateFile.delete();
                }

                // Download the update file
                plugin.getLogger().info("Downloading to: " + tempFile.getAbsolutePath());
                try {
                    downloadFile(downloadUrl, tempFile);
                } catch (IOException e) {
                    // If the first URL fails, try an alternative URL
                    plugin.getLogger().warning("First download URL failed: " + e.getMessage());
                    String alternativeUrl = "https://github.com/" + githubRepo + "/releases/download/" + 
                                         version + "-SNAPSHOT/TNTFireworks-" + version + ".jar";
                    plugin.getLogger().info("Trying with alternative URL: " + alternativeUrl);
                    downloadFile(alternativeUrl, tempFile);
                    updateFileName = "TNTFireworks-" + version + ".jar";
                    updateFile = new File(pluginsDir, updateFileName);
                }

                // Rename the downloaded file
                if (!tempFile.renameTo(updateFile)) {
                    Files.move(tempFile.toPath(), updateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                plugin.getLogger().info("Update file created: " + updateFile.getAbsolutePath());

                // Delete old versions of the plugin (except the one in use)
                String currentVersion = plugin.getDescription().getVersion();
                // Use the file name consistently in the lambda
                final File finalUpdateFile = updateFile;
                File[] oldVersions = pluginsDir.listFiles((dir, name) -> 
                    name.startsWith("TNTFireworks-") && 
                    name.endsWith(".jar") && 
                    !name.equals(finalUpdateFile.getName()) && 
                    !name.contains(currentVersion));

                if (oldVersions != null && oldVersions.length > 0) {
                    for (File oldFile : oldVersions) {
                        // Do not delete the currently running file
                        if (!oldFile.equals(currentPluginFile)) {
                            if (oldFile.delete()) {
                                plugin.getLogger().info("Old version deleted: " + oldFile.getName());
                            } else {
                                plugin.getLogger().warning("Could not delete old version: " + oldFile.getName());
                                // Mark for deletion on next startup
                                oldFile.deleteOnExit();
                            }
                        }
                    }
                }

                // Replace the current file if possible
                if (!currentPluginFile.equals(updateFile)) {
                    try {
                        // Try to replace directly
                        if (currentPluginFile.delete()) {
                            plugin.getLogger().info("Current file deleted for update");
                        } else {
                            plugin.getLogger().info("Could not delete current file, it will be replaced on restart");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error during direct replacement attempt: " + e.getMessage());
                    }
                }

                // Create an .update file for PaperMC
                File updateMarker = new File(currentPluginFile.getParentFile(), "TNTFireworks.update");
                try (FileOutputStream fos = new FileOutputStream(updateMarker)) {
                    fos.write(updateFile.getAbsolutePath().getBytes());
                }

                // Notify the user
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§6[TNTFireworks] §aUpdate downloaded successfully!");
                    sender.sendMessage("§6[TNTFireworks] §eThe update will be installed on the next server restart.");
                    sender.sendMessage("§6[TNTFireworks] §eUse §b/tntfireworks restart §eto restart the server now.");
                });

            } catch (Exception e) {
                e.printStackTrace();
                notifyError(sender, "Error during download: " + e.getMessage());
            }
        });
    }

    /**
     * Downloads a file from a URL
     * @param fileUrl The URL of the file to download
     * @param destination The destination file
     * @throws IOException If an error occurs during download
     */
    private void downloadFile(String fileUrl, File destination) throws IOException {
        plugin.getLogger().info("Attempting to download from: " + fileUrl);

        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "TNTFireworks Update Downloader");
        connection.setConnectTimeout(15000); // 15 seconds timeout
        connection.setReadTimeout(30000);    // 30 seconds timeout

        int responseCode = connection.getResponseCode();
        plugin.getLogger().info("HTTP Response Code: " + responseCode);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Could not download file. HTTP Response Code: " + responseCode + ", Message: " + connection.getResponseMessage());
        }

        // Create parent directory if needed
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) {
            plugin.getLogger().info("Creating parent directory: " + parent.getAbsolutePath());
            parent.mkdirs();
        }

        // Ensure the file can be created
        if (!destination.exists() && !destination.createNewFile()) {
            throw new IOException("Could not create destination file: " + destination.getAbsolutePath());
        }

        // Check if the file is writable
        if (!destination.canWrite()) {
            throw new IOException("Destination file is not writable: " + destination.getAbsolutePath());
        }

        try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destination)) {

            long contentLength = connection.getContentLengthLong();
            plugin.getLogger().info("File size to download: " + contentLength + " bytes");

            byte[] dataBuffer = new byte[4096]; // Larger buffer for better performance
            int bytesRead;
            long totalRead = 0;

            while ((bytesRead = in.read(dataBuffer)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalRead += bytesRead;

                // Progress log for large files
                if (contentLength > 0 && totalRead % (contentLength / 10) < 4096) {
                    int progress = (int)((totalRead * 100) / contentLength);
                    plugin.getLogger().info("Download: " + progress + "% complete");
                }
            }

            fileOutputStream.flush();

            plugin.getLogger().info("Download finished: " + destination.getAbsolutePath() + 
                                   " (" + totalRead + " bytes downloaded)");
        } catch (IOException e) {
            plugin.getLogger().severe("Error during download: " + e.getMessage());

            // Delete partially downloaded file on error
            if (destination.exists()) {
                destination.delete();
                plugin.getLogger().info("Partially downloaded file deleted: " + destination.getAbsolutePath());
            }

            throw e; // Rethrow exception for caller to handle
        }
    }

    /**
     * Finds the current plugin's jar file
     * @return The plugin's jar file, or null if not found
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

            // Adjust for Windows/Unix
            if (System.getProperty("os.name").toLowerCase().contains("win") && jarFilePath.startsWith("/")) {
                jarFilePath = jarFilePath.substring(1);
            }

            // Decode URL-encoded characters (%20 -> space, etc.)
            jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");

            plugin.getLogger().info("Plugin path: " + jarFilePath);

            return new File(jarFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Notifies the user of an error
     * @param sender The command sender
     * @param message The error message
     */
    private void notifyError(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage("§6[TNTFireworks] §c" + message);
        });
    }

    /**
     * Prepares files for update before restart
     */
    private void prepareUpdateFiles() {
        try {
            // Get the current file and plugins folder
            File currentFile = getPluginFile();
            if (currentFile == null) {
                plugin.getLogger().warning("Could not locate the current plugin file for update preparation");
                return;
            }

            File pluginsDir = currentFile.getParentFile();
            String currentVersion = plugin.getDescription().getVersion();
            plugin.getLogger().info("Preparing files for update, current version: " + currentVersion);

            // Find all TNTFireworks*.jar files
            File[] allVersions = pluginsDir.listFiles((dir, name) -> 
                name.startsWith("TNTFireworks-") && name.endsWith(".jar"));

            if (allVersions == null || allVersions.length == 0) {
                plugin.getLogger().info("No files to prepare for update");
                return;
            }

            // Sort by name (newest version first)
            Arrays.sort(allVersions, Comparator.comparing(File::getName).reversed());

            // Find the newest version that is not the current version
            File newestUpdate = null;
            for (File file : allVersions) {
                if (!file.equals(currentFile)) {
                    newestUpdate = file;
                    break;
                }
            }

            if (newestUpdate == null) {
                plugin.getLogger().info("No update found in plugins folder");
                return;
            }

            plugin.getLogger().info("New version found: " + newestUpdate.getName());

            // Create an update marker
            File updateMarker = new File(pluginsDir, "TNTFireworks.update");
            try (FileOutputStream fos = new FileOutputStream(updateMarker)) {
                fos.write(newestUpdate.getAbsolutePath().getBytes());
            }

            plugin.getLogger().info("Update marker created for: " + newestUpdate.getAbsolutePath());

            // Try to delete other obsolete versions
            for (File file : allVersions) {
                if (!file.equals(currentFile) && !file.equals(newestUpdate)) {
                    if (file.delete()) {
                        plugin.getLogger().info("Obsolete version deleted: " + file.getName());
                    } else {
                        file.deleteOnExit();
                        plugin.getLogger().info("Obsolete version marked for deletion on next startup: " + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error preparing update files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Restarts the server
     * @param sender The command sender
     */
    public void restartServer(CommandSender sender) {
        sender.sendMessage("§6[TNTFireworks] §eRestarting the server in 10 seconds...");

        // Announce the restart to all players
        Bukkit.broadcastMessage("§c[SERVER] §eRestarting the server in 10 seconds!");

        // Prepare update files before restarting
        prepareUpdateFiles();

        // Check if update files exist
        File pluginsDir = new File("plugins");
        File[] updateFiles = pluginsDir.listFiles((dir, name) -> 
            name.startsWith("TNTFireworks-") && 
            (name.endsWith("-SNAPSHOT.jar") || name.endsWith(".jar")));

        if (updateFiles != null && updateFiles.length > 0) {
            // Sort by version name (newest first)
            Arrays.sort(updateFiles, Comparator.comparing(File::getName).reversed());
            File latestUpdate = updateFiles[0];

            plugin.getLogger().info("Update found for restart: " + latestUpdate.getName());

            // Try to delete old versions before restarting
            final File current = getPluginFile();
            if (current != null) {
                for (File file : updateFiles) {
                    if (!file.equals(latestUpdate) && !file.equals(current)) {
                        if (file.delete()) {
                            plugin.getLogger().info("Obsolete version deleted before restart: " + file.getName());
                        } else {
                            file.deleteOnExit();
                            plugin.getLogger().info("Obsolete version marked for deletion on startup: " + file.getName());
                        }
                    }
                }
            }

            // Create an .update file for PaperMC
            try {
                File currentPluginFile = getPluginFile();
                if (currentPluginFile != null) {
                    File updateMarker = new File(currentPluginFile.getParentFile(), "TNTFireworks.update");
                    try (FileOutputStream fos = new FileOutputStream(updateMarker)) {
                        fos.write(latestUpdate.getAbsolutePath().getBytes());
                    }
                    plugin.getLogger().info("Update marker created: " + updateMarker.getAbsolutePath());

                    // Try to rename the update to replace the current file
                    if (!latestUpdate.equals(currentPluginFile)) {
                        try {
                            String currentName = currentPluginFile.getName();
                            File targetFile = new File(currentPluginFile.getParentFile(), currentName);

                            // Create a copy to ensure the file will be there on restart
                            try {
                                Files.copy(latestUpdate.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                plugin.getLogger().info("Update copy created: " + targetFile.getAbsolutePath());
                            } catch (Exception e) {
                                plugin.getLogger().warning("Could not create a copy to replace the current file: " + e.getMessage());
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error during direct replacement: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error creating update marker: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Schedule the restart
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("§c[SERVER] §eRestarting server now!");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Try different restart commands depending on the server
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                } catch (Exception e1) {
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "reload confirm");
                    } catch (Exception e2) {
                        try {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
                        } catch (Exception e3) {
                            plugin.getLogger().severe("Could not restart the server automatically. Please restart it manually.");
                            Bukkit.broadcastMessage("§c[SERVER] §4Could not restart automatically. Please restart manually.");
                        }
                    }
                }
            }, 20L);
        }, 200L);
    }
}
