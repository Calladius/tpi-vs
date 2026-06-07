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

    // невидимык символы из ресурспака для паддинга, 1-6px
    private static final char[] PAD_CHARS = {
        '\0',       // 0 не юзается
        '\uE015',   // 1px
        '\uE014',   // 2px
        '\uE013',   // 3px
        '\uE012',   // 4px
        '\uE011',   // 5px
        '\uE010',   // 6px
    };
    private static final String FONT_OPEN = "<font:vnlla:width>";
    private static final String FONT_CLOSE = "</font>";

    // ширины символов в пикселях, вытащил из текстуры майна
    private static final int[] CHAR_WIDTHS = new int[65536];
    static {
        // по дефолту 6
        for (int i = 0; i < CHAR_WIDTHS.length; i++) CHAR_WIDTHS[i] = 6;

        // пробел
        CHAR_WIDTHS[' '] = 4;

        // узкие 2px
        CHAR_WIDTHS['!'] = 2;
        CHAR_WIDTHS['.'] = 2;
        CHAR_WIDTHS[':'] = 2;
        CHAR_WIDTHS[','] = 2;
        CHAR_WIDTHS[';'] = 2;
        CHAR_WIDTHS['|'] = 2;
        CHAR_WIDTHS['\''] = 2;
        CHAR_WIDTHS['i'] = 2;

        // 3px
        CHAR_WIDTHS['l'] = 3;
        CHAR_WIDTHS['`'] = 3;

        // 4px
        CHAR_WIDTHS['['] = 4;
        CHAR_WIDTHS[']'] = 4;
        CHAR_WIDTHS['('] = 4;
        CHAR_WIDTHS[')'] = 4;
        CHAR_WIDTHS['{'] = 4;
        CHAR_WIDTHS['}'] = 4;
        CHAR_WIDTHS['t'] = 4;
        CHAR_WIDTHS['I'] = 4;
        CHAR_WIDTHS['"'] = 4;
        CHAR_WIDTHS['*'] = 4;

        // 5px
        CHAR_WIDTHS['f'] = 5;
        CHAR_WIDTHS['k'] = 5;
        CHAR_WIDTHS['<'] = 5;
        CHAR_WIDTHS['>'] = 5;

        // 7px
        CHAR_WIDTHS['@'] = 7;
        CHAR_WIDTHS['~'] = 7;

        // кирилица, мерил по текстуре nonlatin_european.png
        CHAR_WIDTHS['Б'] = 4;
        CHAR_WIDTHS['Г'] = 8;
        CHAR_WIDTHS['Д'] = 7;
        CHAR_WIDTHS['Е'] = 7;
        CHAR_WIDTHS['Л'] = 7;
        CHAR_WIDTHS['Н'] = 8;
        CHAR_WIDTHS['Ъ'] = 8;
        CHAR_WIDTHS['Ь'] = 7;
        CHAR_WIDTHS['Ю'] = 8;
        CHAR_WIDTHS['Я'] = 9;
        CHAR_WIDTHS['а'] = 7;
        CHAR_WIDTHS['б'] = 8;
        CHAR_WIDTHS['д'] = 8;
        CHAR_WIDTHS['к'] = 5;
        CHAR_WIDTHS['л'] = 7;
        CHAR_WIDTHS['р'] = 5;
        CHAR_WIDTHS['я'] = 7;
    }

    // ширина префиксной зоны — максимум из дефолтного и всех текущих префиксов
    private int getMaxPixelWidth() {
        int max = getPixelWidth(plugin.getPluginConfig().getDefaultPrefix());
        for (Player p : Bukkit.getOnlinePlayers()) {
            String prefix = plugin.getPrefixManager().getPrefix(p.getUniqueId());
            if (prefix == null || prefix.isEmpty()) continue;
            int w = getPixelWidth(prefix);
            if (w > max) max = w;
        }
        return max;
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

    // выравнивание таба — паддинг чтоб ники были ровно
    public void updatePlayerListName(Player player) {
        String prefix = plugin.getPrefixManager().getPrefix(player.getUniqueId());
        boolean isAfk = plugin.getAfkManager().isAfk(player.getUniqueId());
        // если нет своего префикса — юзаем дефолтный
        if (prefix == null || prefix.isEmpty()) {
            prefix = plugin.getPluginConfig().getDefaultPrefix();
        }
        int maxPx = getMaxPixelWidth();
        int prefixPx = getPixelWidth(prefix);
        int padPx = Math.max(0, maxPx - prefixPx);

        StringBuilder sb = new StringBuilder();

        // невидимый паддинг перед префиксом
        sb.append(FONT_OPEN);
        sb.append(buildPadding(padPx));
        sb.append(FONT_CLOSE);
        // сам префикс
        sb.append(prefix);

        // пробел перед ником
        sb.append(' ');
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

    // собрать строку невидимых символов на заданную ширину
    private String buildPadding(int px) {
        if (px <= 0) return "";
        StringBuilder sb = new StringBuilder();
        // сколько 6px
        int count6 = px / 6;
        for (int i = 0; i < count6; i++) {
            sb.append(PAD_CHARS[6]);
        }
        // остаток одним символом
        int rem = px % 6;
        if (rem > 0) {
            sb.append(PAD_CHARS[rem]);
        }
        return sb.toString();
    }

    // посчитать ширину текста в пикселях, парсит минимесседж теги
    // bold даёт +1px на символ, остальное не влияет на ширину
    private int getPixelWidth(String text) {
        int width = 0;
        boolean bold = false;
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '<') {
                int end = text.indexOf('>', i);
                if (end == -1) { i++; continue; }
                String tag = text.substring(i + 1, end).toLowerCase().trim();
                // bold вкл
                if (tag.equals("bold") || tag.equals("b")) {
                    bold = true;
                }
                // bold выкл
                else if (tag.equals("/bold") || tag.equals("/b")) {
                    bold = false;
                }
                // reset скидывает всё
                else if (tag.equals("reset")) {
                    bold = false;
                }
                i = end + 1;
            } else {
                char c = text.charAt(i);
                int cw = (c < CHAR_WIDTHS.length) ? CHAR_WIDTHS[c] : 6;
                if (bold) cw += 1; // жирный на 1px шире
                width += cw;
                i++;
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
