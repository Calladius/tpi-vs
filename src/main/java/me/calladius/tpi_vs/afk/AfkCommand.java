package me.calladius.tpi_vs.afk;

import me.calladius.tpi_vs.TpiVsPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class AfkCommand implements CommandExecutor, TabCompleter {

    private final TpiVsPlugin plugin;

    public AfkCommand(TpiVsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Команда только для игроков!").color(NamedTextColor.RED));
            return true;
        }

        plugin.getAfkManager().toggleAfk(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
