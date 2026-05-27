package me.calladius.tpi_vs.admin;

import me.calladius.tpi_vs.TpiVsPlugin;
import me.calladius.tpi_vs.config.PluginConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final TpiVsPlugin plugin;

    public AdminCommand(TpiVsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Команда только для игроков!").color(NamedTextColor.RED));
            return true;
        }

        String perm = plugin.getPluginConfig().getAdminPermission();
        if (!player.hasPermission(perm)) {
            player.sendMessage(Component.text("Нет прав!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Использование: /" + label + " <игрок>").color(NamedTextColor.RED));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Игрок " + targetName + " не найден или не в сети!")
                .color(NamedTextColor.RED));
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        if (cmdName.equals("invsee") || cmdName.equals("inv") || cmdName.equals("inventory")) {
            plugin.getAdminGui().openInventoryGui(player, target);
        } else if (cmdName.equals("endersee") || cmdName.equals("ecsee") || cmdName.equals("ec") || cmdName.equals("enderchest")) {
            plugin.getAdminGui().openEnderChestGui(player, target);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String perm = plugin.getPluginConfig().getAdminPermission();
            if (!sender.hasPermission(perm)) return new ArrayList<>();

            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
