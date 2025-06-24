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
        this.versionPattern = Pattern.compile("tag/(.*?)-SNAPSHOT");
        this.currentVersion = plugin.getDescription().getVersion();

        // Display current version in logs
        plugin.getLogger().info("Current version: " + this.currentVersion);
    }

    /**
     * Checks if an update is available
     * @return true if an update is available
     */
    public boolean checkForUpdates() {
        try {
            String latestVersion = getLatestVersion();
            this.latestVersion = latestVersion;

            if (latestVersion == null) {
                plugin.getLogger().warning("Could not check for updates: version not found.");
                return false;
            }

            // Compare versions
            if (isNewer(latestVersion, currentVersion)) {
                updateAvailable = true;
                plugin.getLogger().info("A new version of TNTFireworks is available: " + latestVersion);
                plugin.getLogger().info("Use /tntfireworks update to update the plugin.");
                return true;
            } else {
                plugin.getLogger().info("TNTFireworks is up to date (version " + currentVersion + ").");
                return false;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the latest version from GitHub
     * @return The latest available version
     * @throws IOException If an error occurs while connecting to GitHub
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
            plugin.getLogger().info("Redirect URL: " + redirectUrl);
            Matcher matcher = versionPattern.matcher(redirectUrl);
            if (matcher.find()) {
                String version = matcher.group(1);
                plugin.getLogger().info("Version found: " + version);
                return version;
            }
        } else if (responseCode == HttpURLConnection.HTTP_OK) {
            // If no redirection, read page content
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Matcher matcher = versionPattern.matcher(response.toString());
                if (matcher.find()) {
                    String version = matcher.group(1);
                    plugin.getLogger().info("Version found on page: " + version);
                    return version;
                }
            }
        }

        plugin.getLogger().warning("No version found. Response code: " + responseCode);

        return null;
    }

    /**
     * Compares two versions to determine if the first is newer
     * @param version1 First version
     * @param version2 Second version
     * @return true if version1 is newer than version2
     */
    private boolean isNewer(String version1, String version2) {
        // Remove suffixes like "-SNAPSHOT" before comparison
        version1 = cleanVersionString(version1);
        version2 = cleanVersionString(version2);

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

        return false; // Versions are identical
    }

    /**
     * Cleans a version string by removing non-numeric suffixes
     * @param version The version string to clean
     * @return The cleaned version, containing only numbers and dots
     */
    private String cleanVersionString(String version) {
        // Remove everything after a dash or space
        if (version.contains("-")) {
            version = version.substring(0, version.indexOf("-"));
        }
        if (version.contains(" ")) {
            version = version.substring(0, version.indexOf(" "));
        }
        return version;
    }

    /**
     * @return The latest available version
     */
    public String getLatestVersionString() {
        return latestVersion;
    }

    /**
     * @return true if an update is available
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
}
