package me.calladius.tpi_vs.config;

import me.calladius.tpi_vs.TpiVsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private final TpiVsPlugin plugin;

    private int tabUpdateInterval;
    private String serverName;
    private String serverSubName;
    private int afkTimeoutSeconds;
    private int hostileMobRadius;
    private boolean afkMobProtection;

    private int maxPrefixLength;
    private String defaultPrefix;

    private String adminPermission;

    public PluginConfig(TpiVsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        tabUpdateInterval = config.getInt("tab.update-interval-ticks", 60);
        serverName = config.getString("tab.server-name", "VNLLA.RU");
        serverSubName = config.getString("tab.server-sub-name", "survival");
        afkTimeoutSeconds = config.getInt("afk.timeout-seconds", 300);
        hostileMobRadius = config.getInt("afk.hostile-mob-radius", 10);
        afkMobProtection = config.getBoolean("afk.mob-protection", true);

        maxPrefixLength = config.getInt("prefix.max-length", 5);
        defaultPrefix = config.getString("prefix.default-prefix", "<bold><aqua>VNLLA</aqua></bold>");

        adminPermission = config.getString("admin.permission", "tpi_vs.admin");
    }

    public void save() {
        FileConfiguration config = plugin.getConfig();

        config.set("tab.update-interval-ticks", tabUpdateInterval);
        config.set("tab.server-name", serverName);
        config.set("tab.server-sub-name", serverSubName);
        config.set("afk.timeout-seconds", afkTimeoutSeconds);
        config.set("afk.hostile-mob-radius", hostileMobRadius);
        config.set("afk.mob-protection", afkMobProtection);

        config.set("prefix.max-length", maxPrefixLength);
        config.set("prefix.default-prefix", defaultPrefix);

        config.set("admin.permission", adminPermission);

        plugin.saveConfig();
    }

    public int getTabUpdateInterval() { return tabUpdateInterval; }
    public String getServerName() { return serverName; }
    public String getServerSubName() { return serverSubName; }
    public int getAfkTimeoutSeconds() { return afkTimeoutSeconds; }
    public int getHostileMobRadius() { return hostileMobRadius; }
    public boolean isAfkMobProtection() { return afkMobProtection; }
    public int getMaxPrefixLength() { return maxPrefixLength; }
    public String getDefaultPrefix() { return defaultPrefix; }
    public String getAdminPermission() { return adminPermission; }

    public void setMaxPrefixLength(int length) { this.maxPrefixLength = length; }
    public void setAfkTimeoutSeconds(int seconds) { this.afkTimeoutSeconds = seconds; }
    public void setHostileMobRadius(int radius) { this.hostileMobRadius = radius; }
}
