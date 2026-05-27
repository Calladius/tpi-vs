package me.calladius.tpi_vs.prefix;

import me.calladius.tpi_vs.TpiVsPlugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrefixManager {

    private final TpiVsPlugin plugin;
    private final File dataFile;
    private final Map<UUID, String> prefixes;
    private final Map<UUID, Boolean> lockedPlayers;
    private YamlConfiguration dataConfig;

    public PrefixManager(TpiVsPlugin plugin) {
        this.plugin = plugin;
        this.prefixes = new ConcurrentHashMap<>();
        this.lockedPlayers = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "prefixes.yml");
    }

    public void loadData() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); }
            catch (IOException e) { plugin.getLogger().warning("Не удалось создать prefixes.yml: " + e.getMessage()); }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("prefixes")) {
            for (String key : dataConfig.getConfigurationSection("prefixes").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String prefix = dataConfig.getString("prefixes." + key, "");
                    if (prefix != null && !prefix.isEmpty()) prefixes.put(uuid, prefix);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Некорректный UUID в prefixes.yml: " + key);
                }
            }
        }

        if (dataConfig.contains("locked")) {
            for (String key : dataConfig.getConfigurationSection("locked").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    boolean locked = dataConfig.getBoolean("locked." + key, false);
                    if (locked) lockedPlayers.put(uuid, true);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Некорректный UUID в locked: " + key);
                }
            }
        }
    }

    public void saveData() {
        if (dataConfig == null) dataConfig = new YamlConfiguration();

        dataConfig.set("prefixes", null);
        for (Map.Entry<UUID, String> entry : prefixes.entrySet()) {
            dataConfig.set("prefixes." + entry.getKey().toString(), entry.getValue());
        }

        dataConfig.set("locked", null);
        for (Map.Entry<UUID, Boolean> entry : lockedPlayers.entrySet()) {
            dataConfig.set("locked." + entry.getKey().toString(), entry.getValue());
        }

        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().warning("Не удалось сохранить prefixes.yml: " + e.getMessage()); }
    }

    public String getPrefix(UUID uuid) {
        return prefixes.getOrDefault(uuid, "");
    }

    public boolean setPrefix(UUID uuid, String prefix, boolean bypassLock) {
        if (!bypassLock && isLocked(uuid)) return false;

        int maxLen = plugin.getPluginConfig().getMaxPrefixLength();
        int visibleLen = getVisibleLength(prefix);
        if (visibleLen > maxLen) return false;

        if (prefix.isEmpty()) prefixes.remove(uuid);
        else prefixes.put(uuid, prefix);

        saveDataAsync();
        updateTabForPlayer(uuid);
        return true;
    }

    public boolean setPrefix(UUID uuid, String prefix) {
        return setPrefix(uuid, prefix, false);
    }

    public void removePrefix(UUID uuid) {
        prefixes.remove(uuid);
        saveDataAsync();
        updateTabForPlayer(uuid);
    }

    public boolean isLocked(UUID uuid) {
        return lockedPlayers.getOrDefault(uuid, false);
    }

    public void setLocked(UUID uuid, boolean locked) {
        if (locked) lockedPlayers.put(uuid, true);
        else lockedPlayers.remove(uuid);
        saveDataAsync();
    }

    public int getVisibleLength(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.replaceAll("<[^>]+>", "").length();
    }

    private void saveDataAsync() {
        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> saveData());
    }

    private void updateTabForPlayer(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            plugin.getTabManager().updatePlayerListName(player);
        }
    }
}
