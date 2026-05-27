package me.calladius.tpi_vs;

import me.calladius.tpi_vs.tab.TabManager;
import me.calladius.tpi_vs.afk.AfkManager;
import me.calladius.tpi_vs.afk.AfkCommand;
import me.calladius.tpi_vs.prefix.PrefixManager;
import me.calladius.tpi_vs.prefix.PrefixGui;
import me.calladius.tpi_vs.admin.AdminCommand;
import me.calladius.tpi_vs.admin.AdminGui;
import me.calladius.tpi_vs.config.PluginConfig;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class TpiVsPlugin extends JavaPlugin implements TabCompleter {

    private static TpiVsPlugin instance;
    private PluginConfig pluginConfig;
    private TabManager tabManager;
    private AfkManager afkManager;
    private PrefixManager prefixManager;
    private PrefixGui prefixGui;
    private AdminGui adminGui;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        prefixManager = new PrefixManager(this);
        prefixManager.loadData();

        afkManager = new AfkManager(this);
        tabManager = new TabManager(this);

        prefixGui = new PrefixGui(this);
        adminGui = new AdminGui(this);

        getServer().getPluginManager().registerEvents(afkManager, this);
        getServer().getPluginManager().registerEvents(prefixGui, this);
        getServer().getPluginManager().registerEvents(adminGui, this);

        AfkCommand afkCommand = new AfkCommand(this);
        getCommand("afk").setExecutor(afkCommand);
        getCommand("afk").setTabCompleter(afkCommand);

        AdminCommand adminCmd = new AdminCommand(this);
        getCommand("invsee").setExecutor(adminCmd);
        getCommand("invsee").setTabCompleter(adminCmd);
        getCommand("endersee").setExecutor(adminCmd);
        getCommand("endersee").setTabCompleter(adminCmd);
        if (getCommand("inv") != null) {
            getCommand("inv").setExecutor(adminCmd);
            getCommand("inv").setTabCompleter(adminCmd);
        }
        if (getCommand("ec") != null) {
            getCommand("ec").setExecutor(adminCmd);
            getCommand("ec").setTabCompleter(adminCmd);
        }
        if (getCommand("ecsee") != null) {
            getCommand("ecsee").setExecutor(adminCmd);
            getCommand("ecsee").setTabCompleter(adminCmd);
        }

        getCommand("prefix").setExecutor(prefixGui);
        getCommand("prefix").setTabCompleter(prefixGui);

        getCommand("tpi_vs").setExecutor(this);
        getCommand("tpi_vs").setTabCompleter(this);

        afkManager.start();
        tabManager.start();

        getLogger().info("TPI_VS запущен!");
    }

    @Override
    public void onDisable() {
        if (tabManager != null) tabManager.stop();
        if (afkManager != null) afkManager.stop();
        if (prefixManager != null) prefixManager.saveData();
        getLogger().info("TPI_VS остановлен!");
    }

    public static TpiVsPlugin getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    public AfkManager getAfkManager() {
        return afkManager;
    }

    public PrefixManager getPrefixManager() {
        return prefixManager;
    }

    public AdminGui getAdminGui() {
        return adminGui;
    }

    public PrefixGui getPrefixGui() {
        return prefixGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("tpi_vs")) return false;

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            pluginConfig.load();

            if (tabManager != null) {
                tabManager.stop();
                tabManager.start();
            }
            if (afkManager != null) {
                afkManager.stop();
                afkManager.start();
            }

            if (tabManager != null) {
                tabManager.updateAllPlayers();
            }

            if (sender instanceof Player player) {
                player.getScheduler().execute(this, () -> {
                    player.sendMessage(Component.text("TPI_VS перезагружен!").color(NamedTextColor.GREEN));
                }, () -> {}, 1L);
            } else {
                sender.sendMessage(Component.text("TPI_VS перезагружен!").color(NamedTextColor.GREEN));
            }
            return true;
        }

        sender.sendMessage(Component.text("Использование: /tpi_vs reload").color(NamedTextColor.GRAY));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if ("reload".startsWith(prefix)) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
