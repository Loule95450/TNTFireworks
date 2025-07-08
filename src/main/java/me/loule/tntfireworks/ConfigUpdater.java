package me.loule.tntfireworks;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Utility class to update the config.yml file when new options are added
 * without losing existing configuration values.
 */
public class ConfigUpdater {
    private final JavaPlugin plugin;

    public ConfigUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Updates the configuration file with new options while preserving existing values
     * @return true if the configuration was updated
     */
    public boolean updateConfig() {
        try {
            // Get the current config file
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                // If config doesn't exist, just save the default
                plugin.saveDefaultConfig();
                return true;
            }

            // Load the existing config
            FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);

            // Load the default config from resources
            Reader defaultConfigStream = new InputStreamReader(
                    plugin.getResource("config.yml"), StandardCharsets.UTF_8);
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);

            // Check if there are new keys in the default config
            boolean hasNewKeys = false;
            
            // Map to store the current config's comments and structure
            Map<String, String> comments = extractComments(plugin.getResource("config.yml"));

            // Check for new keys and collect all keys with their values
            Set<String> allKeys = new HashSet<>();
            Map<String, Object> mergedConfig = new HashMap<>();
            
            // Get all keys from both configs
            collectKeys(defaultConfig, "", allKeys);
            collectKeys(currentConfig, "", allKeys);
            
            // Merge the configs
            for (String key : allKeys) {
                if (defaultConfig.contains(key) && !currentConfig.contains(key)) {
                    // This is a new key from the default config
                    mergedConfig.put(key, defaultConfig.get(key));
                    hasNewKeys = true;
                    plugin.getLogger().info("Added new config option: " + key);
                } else if (currentConfig.contains(key)) {
                    // Preserve the existing value from the current config
                    mergedConfig.put(key, currentConfig.get(key));
                }
            }

            if (!hasNewKeys) {
                // No new keys, no need to update
                return false;
            }

            // Create the updated config file with comments preserved
            createUpdatedConfigFile(configFile, mergedConfig, comments);
            
            // Reload the plugin's configuration
            plugin.reloadConfig();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating config.yml: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Extracts comments and structure from the default config file
     * @param configResource The input stream for the default config
     * @return A map of paths to comments
     */
    private Map<String, String> extractComments(InputStream configResource) throws IOException {
        Map<String, String> comments = new LinkedHashMap<>();
        if (configResource == null) {
            return comments;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(configResource, StandardCharsets.UTF_8));
        StringBuilder commentBuilder = new StringBuilder();
        String currentPath = "";
        String line;
        int indentation = 0;
        
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            
            // Collect comments
            if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
                commentBuilder.append(line).append("\n");
                continue;
            }
            
            // Handle section headers (like "# ---- Section ----")
            if (commentBuilder.length() > 0) {
                comments.put(currentPath, commentBuilder.toString());
                commentBuilder = new StringBuilder();
            }
            
            // Handle key-value pairs or sections
            if (trimmedLine.contains(":")) {
                int colonIndex = trimmedLine.indexOf(":");
                String key = trimmedLine.substring(0, colonIndex).trim();
                
                // Calculate indentation
                int currentIndentation = line.indexOf(key);
                
                // Adjust path based on indentation
                if (currentIndentation <= indentation) {
                    // Go back up the path tree
                    String[] parts = currentPath.split("\\.");
                    int levels = (indentation - currentIndentation) / 2 + 1;
                    levels = Math.min(levels, parts.length);
                    
                    if (parts.length >= levels) {
                        StringBuilder newPath = new StringBuilder();
                        for (int i = 0; i < parts.length - levels; i++) {
                            if (i > 0) newPath.append(".");
                            newPath.append(parts[i]);
                        }
                        currentPath = newPath.toString();
                    }
                }
                
                // Update current path
                if (!currentPath.isEmpty()) {
                    currentPath = currentPath + "." + key;
                } else {
                    currentPath = key;
                }
                
                indentation = currentIndentation;
            }
        }
        
        reader.close();
        return comments;
    }

    /**
     * Creates an updated config file with merged values and preserved comments
     * @param configFile The config file to update
     * @param mergedConfig The merged configuration values
     * @param comments The comments from the default config
     */
    private void createUpdatedConfigFile(File configFile, Map<String, Object> mergedConfig, Map<String, String> comments) throws IOException {
        // Create a backup of the current config
        File backupFile = new File(configFile.getParentFile(), "config.yml.bak");
        if (backupFile.exists()) {
            backupFile.delete();
        }
        
        // Copy current config to backup
        try (FileInputStream fis = new FileInputStream(configFile);
             FileOutputStream fos = new FileOutputStream(backupFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
        
        // Create the updated config file
        YamlConfiguration newConfig = new YamlConfiguration();
        
        // Add the values to the new config
        for (Map.Entry<String, Object> entry : mergedConfig.entrySet()) {
            newConfig.set(entry.getKey(), entry.getValue());
        }
        
        // Save the new config with comments
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(configFile), StandardCharsets.UTF_8));
                
        // Write the header comment if it exists
        if (comments.containsKey("")) {
            writer.write(comments.get(""));
        }
        
        // Write the rest of the config with preserved structure
        saveWithStructure(newConfig, writer, comments);
        
        writer.close();
    }

    /**
     * Saves the configuration with preserved structure and comments
     * @param config The configuration to save
     * @param writer The writer to write to
     * @param comments The comments to preserve
     */
    private void saveWithStructure(FileConfiguration config, BufferedWriter writer, Map<String, String> comments) throws IOException {
        // Get a string representation of the config
        String yamlString = config.saveToString();
        String[] lines = yamlString.split("\n");
        
        // Current path being processed
        String currentPath = "";
        int currentIndent = 0;
        
        for (String line : lines) {
            // Skip empty lines from the generated YAML
            if (line.trim().isEmpty()) {
                continue;
            }
            
            // Extract key and update current path
            if (line.contains(":")) {
                int indent = line.indexOf(line.trim());
                String key = line.trim().split(":")[0];
                
                // Handle indentation changes
                if (indent <= currentIndent) {
                    // Go back up the path tree
                    String[] parts = currentPath.split("\\.");
                    int levels = (currentIndent - indent) / 2 + 1;
                    levels = Math.min(levels, parts.length);
                    
                    if (parts.length >= levels) {
                        StringBuilder newPath = new StringBuilder();
                        for (int i = 0; i < parts.length - levels; i++) {
                            if (i > 0) newPath.append(".");
                            newPath.append(parts[i]);
                        }
                        currentPath = newPath.toString();
                    }
                }
                
                // Update current path
                if (!currentPath.isEmpty()) {
                    currentPath = currentPath + "." + key;
                } else {
                    currentPath = key;
                }
                
                currentIndent = indent;
                
                // Write comments for this path if they exist
                if (comments.containsKey(currentPath)) {
                    writer.write(comments.get(currentPath));
                }
            }
            
            // Write the actual config line
            writer.write(line);
            writer.newLine();
        }
    }

    /**
     * Recursively collects all keys from a configuration section
     * @param config The configuration section
     * @param path The current path
     * @param keys The set to collect keys into
     */
    private void collectKeys(ConfigurationSection config, String path, Set<String> keys) {
        for (String key : config.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            keys.add(fullPath);
            
            if (config.isConfigurationSection(key)) {
                collectKeys(config.getConfigurationSection(key), fullPath, keys);
            }
        }
    }
}
