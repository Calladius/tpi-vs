package me.calladius.tpi_vs.tab;

import me.calladius.tpi_vs.TpiVsPlugin;
import me.calladius.tpi_vs.config.PluginConfig;
import me.calladius.tpi_vs.util.FoliaCompat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class TabManager {

    private final TpiVsPlugin plugin;
    private ScheduledTask updateTask;
    private final MiniMessage miniMessage;

    // невидимые символы для выравнивания из ресурспака
    private static final String PAD_CHAR = "\uE010";
    private static final String SPACE_CHAR = "\uE011";
    private static final String PAD2_CHAR = "\uE012";
    private static final String FONT_OPEN = "<font:vnlla:width>";
    private static final String FONT_CLOSE = "</font>";

    // таблица ширин символов в пикселях, для пропорционального шрифта майна
    private static final int[] CHAR_WIDTHS = new int[65536];
    static {
        for (int i = 0; i < CHAR_WIDTHS.length; i++) CHAR_WIDTHS[i] = 6;
        CHAR_WIDTHS[' '] = 4;
        for (char c : "!.:,;|'`lIi1".toCharArray()) CHAR_WIDTHS[c] = 2;
        for (char c : "[](){}t\"".toCharArray()) CHAR_WIDTHS[c] = 4;
        for (char c : "fk*".toCharArray()) CHAR_WIDTHS[c] = 5;
        for (char c : "~@MW".toCharArray()) CHAR_WIDTHS[c] = 7;
        // кирилица в основном по 6px
        for (char c = 'а'; c <= 'я'; c++) CHAR_WIDTHS[c] = 6;
        for (char c = 'А'; c <= 'Я'; c++) CHAR_WIDTHS[c] = 6;
        CHAR_WIDTHS['ё'] = 6; CHAR_WIDTHS['Ё'] = 6;
        CHAR_WIDTHS['л'] = 6; CHAR_WIDTHS['Л'] = 6;
        CHAR_WIDTHS['д'] = 6; CHAR_WIDTHS['Д'] = 6;
        for (char c = '0'; c <= '9'; c++) CHAR_WIDTHS[c] = 6;
        CHAR_WIDTHS['⏳'] = 6;
    }

    // макс ширина префикса в пикселях
    private int getMaxPixelWidth() {
        return plugin.getPluginConfig().getMaxPrefixLength() * 6;
    }

    public TabManager(TpiVsPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void start() {
        PluginConfig config = plugin.getPluginConfig();
        int interval = config.getTabUpdateInterval();
        updateTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
            plugin, t -> updateAllPlayers(), interval, interval
        );
    }

    public void stop() {
        if (updateTask != null) { updateTask.cancel(); updateTask = null; }
    }

    public void updateAllPlayers() {
        Component header = buildHeader();
        int online = Bukkit.getOnlinePlayers().size();
        double tps = FoliaCompat.getAverageTps();
        String tpsStr = FoliaCompat.formatTps(tps);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().execute(plugin, () -> {
                try {
                    player.sendPlayerListHeader(header);
                    sendPlayerFooter(player, tpsStr, online);
                    updatePlayerListName(player);
                } catch (Exception ignored) {}
            }, () -> {}, 1L);
        }
    }

    public void updatePlayer(Player player) {
        try {
            player.sendPlayerListHeader(buildHeader());
            sendPlayerFooter(player, FoliaCompat.formatTps(FoliaCompat.getAverageTps()), Bukkit.getOnlinePlayers().size());
            updatePlayerListName(player);
        } catch (Exception ignored) {}
    }

    private Component buildHeader() {
        PluginConfig config = plugin.getPluginConfig();
        String spacedName = addLetterSpacing(config.getServerName());
        String spacedSub = addLetterSpacing(config.getServerSubName());

        Component nameComp = miniMessage.deserialize(
            "<bold><gradient:#FF0000:#FFFF00>" + spacedName + "</gradient></bold>"
        );
        Component subComp = Component.text(spacedSub).color(NamedTextColor.GRAY);

        return Component.text("")
            .appendNewline()
            .append(nameComp)
            .appendNewline()
            .appendNewline()
            .append(subComp)
            .appendNewline()
            .append(Component.text(""));
    }

    private void sendPlayerFooter(Player player, String tpsStr, int online) {
        try {
            int ping = player.getPing();
            NamedTextColor pingColor = ping < 50 ? NamedTextColor.GREEN
                : ping < 100 ? NamedTextColor.YELLOW
                : ping < 200 ? NamedTextColor.GOLD : NamedTextColor.RED;

            Component footer = Component.text("")
                .append(Component.text("  TPS: ").color(NamedTextColor.GRAY))
                .append(miniMessage.deserialize(tpsStr))
                .append(Component.text("  │  ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Пинг: ").color(NamedTextColor.GRAY))
                .append(Component.text(ping + "мс").color(pingColor))
                .append(Component.text("  │  ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Онлайн: ").color(NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(online)).color(NamedTextColor.WHITE));

            player.sendPlayerListFooter(Component.text("").appendNewline().append(footer));
        } catch (Exception ignored) {}
    }

    // выравнивание — паддинг перед префиксом чтоб ники были ровно
    public void updatePlayerListName(Player player) {
        String prefix = plugin.getPrefixManager().getPrefix(player.getUniqueId());
        boolean isAfk = plugin.getAfkManager().isAfk(player.getUniqueId());
        boolean hasPrefix = prefix != null && !prefix.isEmpty();
        int maxPx = getMaxPixelWidth();

        StringBuilder sb = new StringBuilder();

        if (hasPrefix) {
            // пиксельная ширина видимого текста
            String visibleText = prefix.replaceAll("<[^>]+>", "");
            int prefixPx = getPixelWidth(visibleText);
            int padPx = Math.max(0, maxPx - prefixPx);

            // 6+4+2 = можно набрать любую чётную ширину
            int pad6 = padPx / 6;
            int remainder = padPx % 6;

            sb.append(FONT_OPEN);
            for (int i = 0; i < pad6; i++) {
                sb.append(PAD_CHAR);
            }
            // добиваем остаток
            if (remainder >= 4) {
                sb.append(SPACE_CHAR);
                remainder -= 4;
            }
            if (remainder >= 2) {
                sb.append(PAD2_CHAR);
            }
            sb.append(FONT_CLOSE);
            sb.append(prefix);
            sb.append(FONT_OPEN).append(SPACE_CHAR).append(FONT_CLOSE);
        } else {
            // без префикса — просто паддинг
            int pad6 = maxPx / 6;
            int remainder = maxPx % 6;
            sb.append(FONT_OPEN);
            for (int i = 0; i < pad6; i++) {
                sb.append(PAD_CHAR);
            }
            if (remainder >= 4) {
                sb.append(SPACE_CHAR);
                remainder -= 4;
            }
            if (remainder >= 2) {
                sb.append(PAD2_CHAR);
            }
            sb.append(SPACE_CHAR);
            sb.append(FONT_CLOSE);
        }

        sb.append("<white>").append(player.getName()).append("</white>");

        if (isAfk) {
            sb.append(" <gray>⏳</gray>");
        }

        try {
            player.playerListName(miniMessage.deserialize(sb.toString()));
        } catch (Exception e) {
            player.playerListName(Component.text(player.getName()).color(NamedTextColor.WHITE));
        }
    }

    // подсчёт ширины текста в пикселях
    private int getPixelWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < CHAR_WIDTHS.length) {
                width += CHAR_WIDTHS[c];
            } else {
                width += 6; // по умолчанию
            }
        }
        return width;
    }

    private String addLetterSpacing(String text) {
        if (text == null || text.length() <= 1) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (i < text.length() - 1) sb.append('\u2009');
        }
        return sb.toString();
    }
}
