package me.loule.tntfireworks.updater;

import me.loule.tntfireworks.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {
    private final Main plugin;
    private final String githubRepo;
    private final Pattern versionPattern;
    private String latestVersion;
    private String currentVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(Main plugin, String githubRepo) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
        this.versionPattern = Pattern.compile("tag/([\\d.]+)-SNAPSHOT");
        this.currentVersion = plugin.getDescription().getVersion();
    }

    /**
     * Vérifie si une mise à jour est disponible
     * @return true si une mise à jour est disponible
     */
    public boolean checkForUpdates() {
        try {
            String latestVersion = getLatestVersion();
            this.latestVersion = latestVersion;

            if (latestVersion == null) {
                plugin.getLogger().warning("Impossible de vérifier les mises à jour: version non trouvée.");
                return false;
            }

            // Comparaison des versions
            if (isNewer(latestVersion, currentVersion)) {
                updateAvailable = true;
                plugin.getLogger().info("Une nouvelle version de TNTFireworks est disponible: " + latestVersion);
                plugin.getLogger().info("Utilisez /tntfireworks update pour mettre à jour le plugin.");
                return true;
            } else {
                plugin.getLogger().info("TNTFireworks est à jour (version " + currentVersion + ").");
                return false;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de vérifier les mises à jour: " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupère la dernière version disponible sur GitHub
     * @return La dernière version disponible
     * @throws IOException Si une erreur se produit lors de la connexion à GitHub
     */
    private String getLatestVersion() throws IOException {
        URL url = new URL("https://github.com/" + githubRepo + "/releases/latest");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "TNTFireworks Update Checker");

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String redirectUrl = connection.getHeaderField("Location");
            Matcher matcher = versionPattern.matcher(redirectUrl);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } else if (responseCode == HttpURLConnection.HTTP_OK) {
            // Si pas de redirection, lire le contenu de la page
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Matcher matcher = versionPattern.matcher(response.toString());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }

        return null;
    }

    /**
     * Compare deux versions pour déterminer si la première est plus récente
     * @param version1 Première version
     * @param version2 Deuxième version
     * @return true si version1 est plus récente que version2
     */
    private boolean isNewer(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (v1 > v2) {
                return true;
            } else if (v1 < v2) {
                return false;
            }
        }

        return false; // Les versions sont identiques
    }

    /**
     * @return La dernière version disponible
     */
    public String getLatestVersionString() {
        return latestVersion;
    }

    /**
     * @return true si une mise à jour est disponible
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
}
