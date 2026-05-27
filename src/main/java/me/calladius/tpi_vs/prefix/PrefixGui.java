package me.calladius.tpi_vs.prefix;

import me.calladius.tpi_vs.TpiVsPlugin;
import me.calladius.tpi_vs.config.PluginConfig;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrefixGui implements Listener, CommandExecutor, TabCompleter {

    private final TpiVsPlugin plugin;
    private final MiniMessage miniMessage;

    private final Map<UUID, List<PrefixSegment>> editingSegments;
    private final Map<UUID, Boolean> waitingForText;
    private final Map<UUID, String> gradientStartColor;
    private final Map<UUID, String> gradientEndColor;
    private final Map<UUID, Integer> currentColorPage;

    private final Map<UUID, UUID> editingTarget;

    private final Map<UUID, Boolean> waitingForHex;

    private static final String MAIN_TITLE = "Настройка префикса";
    private static final String GRADIENT_TITLE = "Выбор градиента";

    private static final String[] COLOR_NAMES = {
        "black", "dark_blue", "dark_green", "dark_aqua",
        "dark_red", "dark_purple", "gold", "gray",
        "dark_gray", "blue", "green", "aqua",
        "red", "light_purple", "yellow", "white"
    };

    private static final String[] COLOR_DISPLAY_NAMES = {
        "Чёрный", "Тёмно-синий", "Тёмно-зелёный", "Тёмно-голубой",
        "Тёмно-красный", "Тёмно-фиолетовый", "Золотой", "Серый",
        "Тёмно-серый", "Синий", "Зелёный", "Голубой",
        "Красный", "Светло-фиолетовый", "Жёлтый", "Белый"
    };

    private static final Material[] COLOR_MATERIALS = {
        Material.BLACK_CONCRETE, Material.BLUE_CONCRETE, Material.GREEN_CONCRETE, Material.CYAN_CONCRETE,
        Material.RED_CONCRETE, Material.PURPLE_CONCRETE, Material.ORANGE_CONCRETE, Material.LIGHT_GRAY_CONCRETE,
        Material.GRAY_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.LIME_CONCRETE, Material.LIGHT_BLUE_CONCRETE,
        Material.PINK_CONCRETE, Material.MAGENTA_CONCRETE, Material.YELLOW_CONCRETE, Material.WHITE_CONCRETE
    };

    private static final String[] EXTRA_HEX = {
        "#FF0000", "#FF4400", "#FF8800", "#FFBB00",
        "#FFFF00", "#88FF00", "#00FF00", "#00FF88",
        "#00FFFF", "#0088FF", "#0000FF", "#8800FF",
        "#FF00FF", "#FF0088", "#FF6688", "#FFAACC"
    };

    private static final String[] EXTRA_NAMES = {
        "Ярко-красный", "Оранжево-красный", "Оранжевый", "Золотистый",
        "Лимонный", "Лаймовый", "Ярко-зелёный", "Мятный",
        "Голубой", "Небесный", "Синий", "Фиолетовый",
        "Пурпурный", "Розовый", "Лососевый", "Персиковый"
    };

    private static final Material[] EXTRA_MATERIALS = {
        Material.RED_STAINED_GLASS_PANE,
        Material.ORANGE_STAINED_GLASS_PANE,
        Material.ORANGE_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.LIME_STAINED_GLASS_PANE,
        Material.GREEN_STAINED_GLASS_PANE,
        Material.GREEN_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.BLUE_STAINED_GLASS_PANE,
        Material.PURPLE_STAINED_GLASS_PANE,
        Material.MAGENTA_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE
    };

    private static final String[] GRADIENT_HEX;
    static {
        GRADIENT_HEX = new String[32];
        GRADIENT_HEX[0] = "#000000"; GRADIENT_HEX[1] = "#0000AA"; GRADIENT_HEX[2] = "#00AA00"; GRADIENT_HEX[3] = "#00AAAA";
        GRADIENT_HEX[4] = "#AA0000"; GRADIENT_HEX[5] = "#AA00AA"; GRADIENT_HEX[6] = "#FFAA00"; GRADIENT_HEX[7] = "#AAAAAA";
        GRADIENT_HEX[8] = "#555555"; GRADIENT_HEX[9] = "#5555FF"; GRADIENT_HEX[10] = "#55FF55"; GRADIENT_HEX[11] = "#55FFFF";
        GRADIENT_HEX[12] = "#FF5555"; GRADIENT_HEX[13] = "#FF55FF"; GRADIENT_HEX[14] = "#FFFF55"; GRADIENT_HEX[15] = "#FFFFFF";
        for (int i = 0; i < 16; i++) {
            GRADIENT_HEX[16 + i] = EXTRA_HEX[i];
        }
    }

    private static final String[][] PRESET_GRADIENTS = {
        {"#FF0000", "#FFFF00", "Огонь"},
        {"#0000FF", "#00FFFF", "Океан"},
        {"#00FF00", "#FFFF00", "Природа"},
        {"#FF00FF", "#FF5555", "Закат"},
        {"#5555FF", "#FF55FF", "Аура"},
        {"#FFAA00", "#FF5555", "Лава"},
        {"#00FFFF", "#55FF55", "Изумруд"},
        {"#FFFFFF", "#AAAAAA", "Серебро"},
    };

    public PrefixGui(TpiVsPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.editingSegments = new ConcurrentHashMap<>();
        this.waitingForText = new ConcurrentHashMap<>();
        this.gradientStartColor = new ConcurrentHashMap<>();
        this.gradientEndColor = new ConcurrentHashMap<>();
        this.currentColorPage = new ConcurrentHashMap<>();
        this.editingTarget = new ConcurrentHashMap<>();
        this.waitingForHex = new ConcurrentHashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String adminPerm = plugin.getPluginConfig().getAdminPermission();

        if (args.length >= 1) {
            String subCmd = args[0].toLowerCase();

            if (!subCmd.equals("set") && !subCmd.equals("reset")
                && !subCmd.equals("lock") && !subCmd.equals("unlock")) {
                sender.sendMessage(Component.text("Неизвестная подкоманда: " + subCmd).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Доступные: set, reset, lock, unlock").color(NamedTextColor.GRAY));
                return true;
            }

            if (!sender.hasPermission(adminPerm)) {
                sender.sendMessage(Component.text("Нет прав!").color(NamedTextColor.RED));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(Component.text("Использование: /prefix " + subCmd + " <игрок>").color(NamedTextColor.RED));
                return true;
            }

            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);

            if (target == null || !target.isOnline()) {
                sender.sendMessage(Component.text("Игрок " + targetName + " не найден!").color(NamedTextColor.RED));
                return true;
            }

            switch (subCmd) {
                case "set" -> {
                    if (!(sender instanceof Player admin)) {
                        sender.sendMessage(Component.text("Команда только для игроков!").color(NamedTextColor.RED));
                        return true;
                    }
                    editingTarget.put(admin.getUniqueId(), target.getUniqueId());
                    openMainGui(admin);
                    return true;
                }
                case "reset" -> {
                    plugin.getPrefixManager().removePrefix(target.getUniqueId());
                    target.getScheduler().execute(plugin, () -> {
                        plugin.getTabManager().updatePlayerListName(target);
                    }, () -> {}, 1L);
                    sender.sendMessage(Component.text("Префикс сброшен для " + target.getName()).color(NamedTextColor.GREEN));
                    return true;
                }
                case "lock" -> {
                    plugin.getPrefixManager().setLocked(target.getUniqueId(), true);
                    sender.sendMessage(Component.text(target.getName() + " заблокирован от смены префикса").color(NamedTextColor.YELLOW));
                    target.sendMessage(Component.text("Вам заблокирована смена префикса!").color(NamedTextColor.RED));
                    return true;
                }
                case "unlock" -> {
                    plugin.getPrefixManager().setLocked(target.getUniqueId(), false);
                    sender.sendMessage(Component.text(target.getName() + " разблокирован для смены префикса").color(NamedTextColor.GREEN));
                    target.sendMessage(Component.text("Вы снова можете менять префикс!").color(NamedTextColor.GREEN));
                    return true;
                }
                default -> {
                    return true;
                }
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Команда только для игроков!").color(NamedTextColor.RED));
            return true;
        }

        if (plugin.getPrefixManager().isLocked(player.getUniqueId())) {
            player.sendMessage(Component.text("Вам заблокирована смена префикса!").color(NamedTextColor.RED));
            return true;
        }

        UUID uuid = player.getUniqueId();
        editingTarget.remove(uuid);
        editingSegments.remove(uuid);
        gradientStartColor.remove(uuid);
        gradientEndColor.remove(uuid);
        currentColorPage.remove(uuid);
        waitingForText.remove(uuid);
        waitingForHex.remove(uuid);

        openMainGui(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String adminPerm = plugin.getPluginConfig().getAdminPermission();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission(adminPerm)) {
                completions.addAll(List.of("set", "reset", "lock", "unlock"));
            }
        } else if (args.length == 2 && sender.hasPermission(adminPerm)) {
            String prefix = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }

    public void openMainGui(Player player) {
        UUID uuid = player.getUniqueId();

        UUID targetUuid = editingTarget.getOrDefault(uuid, uuid);
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        boolean isEditingOther = !uuid.equals(targetUuid);

        if (!editingSegments.containsKey(uuid)) {
            String currentPrefix = plugin.getPrefixManager().getPrefix(targetUuid);
            List<PrefixSegment> segments = parseMiniMessageToSegments(currentPrefix);
            editingSegments.put(uuid, segments);
        }

        String title = isEditingOther && targetPlayer != null
            ? "Префикс: " + targetPlayer.getName()
            : MAIN_TITLE;

        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillBackground(inv);

        updatePreviewItem(inv, player);

        ItemStack divider = createNamedItem(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray>                    ", null);
        inv.setItem(8, divider);
        inv.setItem(17, divider);
        inv.setItem(26, divider);

        int colorPage = currentColorPage.getOrDefault(uuid, 0);
        if (colorPage == 0) {
            for (int i = 0; i < 16; i++) {
                int slot = 9 + i;
                ItemStack colorItem = new ItemStack(COLOR_MATERIALS[i]);
                ItemMeta meta = colorItem.getItemMeta();
                NamedTextColor namedColor = NamedTextColor.NAMES.value(COLOR_NAMES[i]);
                meta.displayName(Component.text("Цвет: " + COLOR_DISPLAY_NAMES[i])
                    .color(namedColor != null ? namedColor : NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Нажмите для применения")
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                colorItem.setItemMeta(meta);
                inv.setItem(slot, colorItem);
            }
        } else {
            for (int i = 0; i < 16; i++) {
                int slot = 9 + i;
                ItemStack colorItem = new ItemStack(EXTRA_MATERIALS[i]);
                ItemMeta meta = colorItem.getItemMeta();
                try {
                    meta.displayName(miniMessage.deserialize("<color:" + EXTRA_HEX[i] + ">" + EXTRA_NAMES[i] + "</color>")
                        .decoration(TextDecoration.ITALIC, false));
                } catch (Exception e) {
                    meta.displayName(Component.text(EXTRA_NAMES[i]).decoration(TextDecoration.ITALIC, false));
                }
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Hex: " + EXTRA_HEX[i])
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Нажмите для применения")
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                colorItem.setItemMeta(meta);
                inv.setItem(slot, colorItem);
            }
        }

        String pageLabel = colorPage == 0 ? "<green>Страница 1/2" : "<green>Страница 2/2";
        inv.setItem(25, createNamedItem(Material.BOOK, pageLabel, List.of(
            "<gray>Нажмите для переключения"
        )));

        if (colorPage == 1) {
            inv.setItem(26, createNamedItem(Material.WRITABLE_BOOK, "<aqua>Ввести свой Hex", List.of(
                "<gray>Нажмите для ввода цвета",
                "<gray>формат: #RRGGBB",
                "<gray>Пример: #FF5500"
            )));
        }

        inv.setItem(27, createNamedItem(Material.IRON_INGOT, "<white><bold>Жирный</bold></white>", List.of("<gray>Нажмите для переключения")));
        inv.setItem(28, createNamedItem(Material.FEATHER, "<white><italic>Курсив</italic></white>", List.of("<gray>Нажмите для переключения")));
        inv.setItem(29, createNamedItem(Material.WRITABLE_BOOK, "<white><underlined>Подчёркнутый</underlined></white>", List.of("<gray>Нажмите для переключения")));
        inv.setItem(30, createNamedItem(Material.BARRIER, "<white><strikethrough>Зачёркнутый</strikethrough></white>", List.of("<gray>Нажмите для переключения")));
        inv.setItem(31, createNamedItem(Material.ENDER_EYE, "<obfuscated>Обфускация</obfuscated> <gray>(скрытый текст)", List.of("<gray>Нажмите для переключения")));

        inv.setItem(33, createNamedItem(Material.MAP, "<yellow>Градиент", List.of(
            "<gray>Нажмите для настройки",
            "<gray>для последнего сегмента"
        )));

        inv.setItem(35, createNamedItem(Material.WATER_BUCKET, "<red>Сбросить форматирование", List.of(
            "<gray>Удалить всё форматирование",
            "<gray>у последнего сегмента"
        )));

        inv.setItem(45, createNamedItem(Material.TNT, "<red>Очистить всё", List.of("<gray>Удалить все сегменты")));
        inv.setItem(47, createNamedItem(Material.WRITABLE_BOOK, "<green>Ввести текст", List.of(
            "<gray>Нажмите для добавления",
            "<gray>Текст вводится в чат"
        )));
        inv.setItem(49, createNamedItem(Material.EMERALD, "<green><bold>Сохранить", List.of(
            "<gray>Нажмите для сохранения"
        )));
        inv.setItem(51, createNamedItem(Material.ARROW, "<red>Удалить последний сегмент", List.of(
            "<gray>Нажмите для удаления",
            "<gray>последнего сегмента"
        )));
        inv.setItem(53, createNamedItem(Material.BARRIER, "<red>Закрыть", List.of("<gray>Без сохранения")));

        player.openInventory(inv);
    }

    public void openGradientGui(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, GRADIENT_TITLE);

        fillBackground(inv);

        for (int i = 0; i < PRESET_GRADIENTS.length; i++) {
            String[] preset = PRESET_GRADIENTS[i];
            ItemStack presetItem = new ItemStack(Material.PAPER);
            ItemMeta meta = presetItem.getItemMeta();
            try {
                meta.displayName(miniMessage.deserialize(
                    "<gradient:" + preset[0] + ":" + preset[1] + ">" + preset[2] + "</gradient>"
                ).decoration(TextDecoration.ITALIC, false));
            } catch (Exception e) {
                meta.displayName(Component.text(preset[2]).decoration(TextDecoration.ITALIC, false));
            }
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Начало: " + preset[0]).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Конец: " + preset[1]).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Нажмите для применения").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            presetItem.setItemMeta(meta);
            inv.setItem(i, presetItem);
        }

        for (int i = 0; i < 16; i++) {
            ItemStack colorItem = new ItemStack(COLOR_MATERIALS[i]);
            ItemMeta meta = colorItem.getItemMeta();
            boolean isSelected = GRADIENT_HEX[i].equals(gradientStartColor.get(uuid));
            NamedTextColor namedColor = NamedTextColor.NAMES.value(COLOR_NAMES[i % COLOR_NAMES.length]);
            meta.displayName(Component.text((isSelected ? "✓ " : "") + "Начало: " + COLOR_DISPLAY_NAMES[i % COLOR_DISPLAY_NAMES.length])
                .color(namedColor != null ? namedColor : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
            colorItem.setItemMeta(meta);
            inv.setItem(9 + i, colorItem);
        }

        for (int i = 0; i < 16; i++) {
            ItemStack colorItem = new ItemStack(COLOR_MATERIALS[i]);
            ItemMeta meta = colorItem.getItemMeta();
            boolean isSelected = GRADIENT_HEX[i].equals(gradientEndColor.get(uuid));
            NamedTextColor namedColor = NamedTextColor.NAMES.value(COLOR_NAMES[i % COLOR_NAMES.length]);
            meta.displayName(Component.text((isSelected ? "✓ " : "") + "Конец: " + COLOR_DISPLAY_NAMES[i % COLOR_DISPLAY_NAMES.length])
                .color(namedColor != null ? namedColor : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
            colorItem.setItemMeta(meta);
            inv.setItem(27 + i, colorItem);
        }

        String startCol = gradientStartColor.getOrDefault(uuid, "#FF0000");
        String endCol = gradientEndColor.getOrDefault(uuid, "#FFFF00");
        ItemStack previewItem = new ItemStack(Material.PAPER);
        ItemMeta previewMeta = previewItem.getItemMeta();
        try {
            previewMeta.displayName(miniMessage.deserialize(
                "<gradient:" + startCol + ":" + endCol + ">Предпросмотр</gradient>"
            ).decoration(TextDecoration.ITALIC, false));
        } catch (Exception e) {
            previewMeta.displayName(Component.text("Предпросмотр").decoration(TextDecoration.ITALIC, false));
        }
        previewItem.setItemMeta(previewMeta);
        inv.setItem(45, previewItem);

        inv.setItem(49, createNamedItem(Material.EMERALD, "<green>Применить градиент", List.of(
            "<gray>Применить к последнему сегменту"
        )));

        inv.setItem(53, createNamedItem(Material.ARROW, "<red>Назад", List.of("<gray>Вернуться к настройке")));

        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = event.getView().getTitle();

        boolean isMainGui = title.equals(MAIN_TITLE) || title.startsWith("Префикс: ");
        boolean isGradientGui = title.equals(GRADIENT_TITLE);

        if (isMainGui) {
            event.setCancelled(true);
            handleMainGuiClick(player, event.getSlot());
        } else if (isGradientGui) {
            event.setCancelled(true);
            handleGradientGuiClick(player, event.getSlot());
        }
    }

    private void handleMainGuiClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        List<PrefixSegment> segments = editingSegments.get(uuid);
        if (segments == null) return;

        if (slot >= 9 && slot <= 24) {
            int colorIndex = slot - 9;
            int colorPage = currentColorPage.getOrDefault(uuid, 0);

            if (colorPage == 0 && colorIndex < COLOR_NAMES.length) {
                applyColorToLastSegment(uuid, COLOR_NAMES[colorIndex], null);
            } else if (colorPage == 1 && colorIndex < EXTRA_HEX.length) {
                applyColorToLastSegment(uuid, null, EXTRA_HEX[colorIndex]);
            }

            updatePreviewItem(player.getOpenInventory().getTopInventory(), player);
            player.updateInventory();
            return;
        }

        if (slot == 25) {
            int currentPage = currentColorPage.getOrDefault(uuid, 0);
            currentColorPage.put(uuid, currentPage == 0 ? 1 : 0);
            openMainGui(player);
            return;
        }

        if (slot == 26 && currentColorPage.getOrDefault(uuid, 0) == 1) {
            player.closeInventory();
            waitingForHex.put(uuid, true);
            player.sendMessage(Component.text("Введите Hex цвет в формате #RRGGBB (или 'отмена'):")
                .color(NamedTextColor.AQUA));
            player.sendMessage(Component.text("Пример: #FF5500").color(NamedTextColor.GRAY));
            return;
        }

        PrefixSegment lastSegment = segments.isEmpty() ? null : segments.get(segments.size() - 1);

        switch (slot) {
            case 27:
                if (lastSegment != null) {
                    lastSegment.bold = !lastSegment.bold;
                    updatePreviewItem(player.getOpenInventory().getTopInventory(), player);
                    player.updateInventory();
                }
                break;

            case 28:
                if (lastSegment != null) {
                    lastSegment.italic = !lastSegment.italic;
                    updatePreviewItem(player.getOpenInventory().getTopInventory(), player);
                    player.updateInventory();
                }
                break;

            case 29:
                if (lastSegment != null) {
                    lastSegment.underlined = !lastSegment.underlined;
                    updatePreviewItem(player.getOpenInventory().getTopInventory(), player);
                    player.updateInventory();
                }
                break;

            case 30:
                if (lastSegment != null) {
                    lastSegment.strikethrough = !lastSegment.strikethrough;
                    updatePreviewItem(player.getOpenInventory().getTopInventory(), player);
                    player.updateInventory();
                }
                break;

            case 31:
                if (lastSegment != null) {
                    lastSegment.obfuscated = !lastSegment.obfuscated;
                    updatePreviewItem(player.getOpenInventory().getTopInventory(), player);
                    player.updateInventory();
                }
                break;

            case 33:
                openGradientGui(player);
                break;

            case 35:
                if (lastSegment != null) {
                    lastSegment.color = null;
                    lastSegment.hexColor = null;
                    lastSegment.bold = false;
                    lastSegment.italic = false;
                    lastSegment.underlined = false;
                    lastSegment.strikethrough = false;
                    lastSegment.obfuscated = false;
                    lastSegment.gradientStart = null;
                    lastSegment.gradientEnd = null;
                    updatePreviewItem(player.getOpenInventory().getTopInventory(), player);
                    player.updateInventory();
                }
                break;

            case 45:
                segments.clear();
                updatePreviewItem(player.getOpenInventory().getTopInventory(), player);
                player.updateInventory();
                break;

            case 47:
                player.closeInventory();
                waitingForText.put(uuid, true);
                player.sendMessage(Component.text("Введите текст префикса в чат (или 'отмена'):")
                    .color(NamedTextColor.GREEN));
                break;

            case 49:
                savePrefix(player);
                player.closeInventory();
                break;

            case 51:
                if (!segments.isEmpty()) {
                    segments.remove(segments.size() - 1);
                    updatePreviewItem(player.getOpenInventory().getTopInventory(), player);
                    player.updateInventory();
                }
                break;

            case 53:
                player.closeInventory();
                break;
        }
    }

    private void handleGradientGuiClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();

        if (slot >= 0 && slot <= 7 && slot < PRESET_GRADIENTS.length) {
            String[] preset = PRESET_GRADIENTS[slot];
            gradientStartColor.put(uuid, preset[0]);
            gradientEndColor.put(uuid, preset[1]);

            List<PrefixSegment> segments = editingSegments.get(uuid);
            if (segments != null && !segments.isEmpty()) {
                PrefixSegment last = segments.get(segments.size() - 1);
                last.gradientStart = preset[0];
                last.gradientEnd = preset[1];
            }

            openMainGui(player);
            return;
        }

        if (slot >= 9 && slot <= 24) {
            int colorIndex = slot - 9;
            if (colorIndex < GRADIENT_HEX.length) {
                gradientStartColor.put(uuid, GRADIENT_HEX[colorIndex]);
                openGradientGui(player);
            }
            return;
        }

        if (slot >= 27 && slot <= 42) {
            int colorIndex = slot - 27;
            if (colorIndex < GRADIENT_HEX.length) {
                gradientEndColor.put(uuid, GRADIENT_HEX[colorIndex]);
                openGradientGui(player);
            }
            return;
        }

        if (slot == 49) {
            String startCol = gradientStartColor.getOrDefault(uuid, "#FF0000");
            String endCol = gradientEndColor.getOrDefault(uuid, "#FFFF00");

            List<PrefixSegment> segments = editingSegments.get(uuid);
            if (segments != null && !segments.isEmpty()) {
                PrefixSegment last = segments.get(segments.size() - 1);
                last.gradientStart = startCol;
                last.gradientEnd = endCol;
            }

            openMainGui(player);
            return;
        }

        if (slot == 53) {
            openMainGui(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (waitingForHex.getOrDefault(uuid, false)) {
            event.setCancelled(true);
            waitingForHex.remove(uuid);

            String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

            if (text.equalsIgnoreCase("cancel") || text.equalsIgnoreCase("отмена")) {
                player.getScheduler().execute(plugin, () -> openMainGui(player), () -> {}, 1L);
                return;
            }

            if (!text.matches("^#[0-9A-Fa-f]{6}$")) {
                player.sendMessage(Component.text("Неверный формат! Используйте #RRGGBB (например #FF5500)")
                    .color(NamedTextColor.RED));
                player.getScheduler().execute(plugin, () -> openMainGui(player), () -> {}, 1L);
                return;
            }

            applyColorToLastSegment(uuid, null, text);
            player.sendMessage(Component.text("Hex цвет применён: ").color(NamedTextColor.GREEN)
                .append(miniMessage.deserialize("<color:" + text + ">" + text + "</color>")));

            player.getScheduler().execute(plugin, () -> openMainGui(player), () -> {}, 1L);
            return;
        }

        if (!waitingForText.getOrDefault(uuid, false)) return;

        event.setCancelled(true);
        waitingForText.remove(uuid);

        String text = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (text.equalsIgnoreCase("cancel") || text.equalsIgnoreCase("отмена")) {
            player.getScheduler().execute(plugin, () -> openMainGui(player), () -> {}, 1L);
            return;
        }

        List<PrefixSegment> segments = editingSegments.get(uuid);
        if (segments == null) {
            segments = new ArrayList<>();
            editingSegments.put(uuid, segments);
        }

        PrefixSegment segment = new PrefixSegment(text);
        segments.add(segment);

        String fullPrefix = buildMiniMessageString(segments);
        int visibleLen = plugin.getPrefixManager().getVisibleLength(fullPrefix);
        int maxLen = plugin.getPluginConfig().getMaxPrefixLength();

        if (visibleLen > maxLen) {
            segments.remove(segments.size() - 1);
            player.sendMessage(Component.text("Превышена максимальная длина префикса (" + maxLen + " символов)!")
                .color(NamedTextColor.RED));
        } else {
            player.sendMessage(Component.text("Текст добавлен: ").color(NamedTextColor.GREEN)
                .append(Component.text(text).color(NamedTextColor.WHITE)));
        }

        player.getScheduler().execute(plugin, () -> openMainGui(player), () -> {}, 1L);
    }

    // не чистим данные при закрытии — переключение гуи вызывает close event
    // чистим в savePrefix(), onCommand(), onPlayerQuit()
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        editingTarget.remove(uuid);
        editingSegments.remove(uuid);
        gradientStartColor.remove(uuid);
        gradientEndColor.remove(uuid);
        currentColorPage.remove(uuid);
        waitingForText.remove(uuid);
        waitingForHex.remove(uuid);
    }

    private void applyColorToLastSegment(UUID uuid, String colorName, String hexColor) {
        List<PrefixSegment> segments = editingSegments.get(uuid);
        if (segments == null || segments.isEmpty()) return;

        PrefixSegment last = segments.get(segments.size() - 1);
        last.color = colorName;
        last.hexColor = hexColor;
    }

    private void updatePreviewItem(Inventory inv, Player player) {
        UUID uuid = player.getUniqueId();
        List<PrefixSegment> segments = editingSegments.get(uuid);

        UUID targetUuid = editingTarget.getOrDefault(uuid, uuid);
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        String targetName = targetPlayer != null ? targetPlayer.getName() : "???";
        boolean isEditingOther = !uuid.equals(targetUuid);

        String prefixStr = segments != null ? buildMiniMessageString(segments) : "";
        int visibleLen = plugin.getPrefixManager().getVisibleLength(prefixStr);
        int maxLen = plugin.getPluginConfig().getMaxPrefixLength();

        ItemStack previewItem = new ItemStack(Material.PAPER);
        ItemMeta meta = previewItem.getItemMeta();

        String previewMiniMsg = prefixStr + " " + targetName;
        try {
            meta.displayName(miniMessage.deserialize(previewMiniMsg));
        } catch (Exception e) {
            meta.displayName(Component.text(targetName));
        }

        List<Component> lore = new ArrayList<>();
        if (isEditingOther) {
            lore.add(Component.text("Редактирование: " + targetName).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Длина: " + visibleLen + "/" + maxLen)
            .color(visibleLen > maxLen ? NamedTextColor.RED : NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));

        if (segments != null && !segments.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("Сегменты:").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            for (int i = 0; i < segments.size(); i++) {
                PrefixSegment seg = segments.get(i);
                StringBuilder segInfo = new StringBuilder("<gray>" + (i + 1) + ". <white>" + seg.text);
                if (seg.color != null) segInfo.append(" <dark_gray>[").append(seg.color).append("]");
                if (seg.hexColor != null) segInfo.append(" <dark_gray>[").append(seg.hexColor).append("]");
                if (seg.bold) segInfo.append(" <dark_gray>[B]");
                if (seg.italic) segInfo.append(" <dark_gray>[I]");
                if (seg.underlined) segInfo.append(" <dark_gray>[U]");
                if (seg.strikethrough) segInfo.append(" <dark_gray>[S]");
                if (seg.obfuscated) segInfo.append(" <dark_gray>[O]");
                if (seg.gradientStart != null) segInfo.append(" <dark_gray>[Grad]");
                lore.add(miniMessage.deserialize(segInfo.toString()));
            }
        }

        meta.lore(lore);
        previewItem.setItemMeta(meta);
        inv.setItem(4, previewItem);
    }

    private void savePrefix(Player player) {
        UUID uuid = player.getUniqueId();

        UUID targetUuid = editingTarget.getOrDefault(uuid, uuid);
        boolean isEditingOther = !uuid.equals(targetUuid);

        if (!isEditingOther && plugin.getPrefixManager().isLocked(targetUuid)) {
            player.sendMessage(Component.text("Вам заблокирована смена префикса!").color(NamedTextColor.RED));
            return;
        }

        List<PrefixSegment> segments = editingSegments.get(uuid);

        if (segments == null || segments.isEmpty()) {
            plugin.getPrefixManager().removePrefix(targetUuid);

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null) {
                targetPlayer.getScheduler().execute(plugin, () -> {
                    plugin.getTabManager().updatePlayerListName(targetPlayer);
                }, () -> {}, 1L);
            }

            if (isEditingOther) {
                String targetName = targetPlayer != null ? targetPlayer.getName() : targetUuid.toString();
                player.sendMessage(Component.text("Префикс для " + targetName + " сброшен!").color(NamedTextColor.GREEN));
            }
            editingTarget.remove(uuid);
            editingSegments.remove(uuid);
            gradientStartColor.remove(uuid);
            gradientEndColor.remove(uuid);
            currentColorPage.remove(uuid);
            waitingForHex.remove(uuid);
            return;
        }

        String prefixStr = buildMiniMessageString(segments);
        int visibleLen = plugin.getPrefixManager().getVisibleLength(prefixStr);
        int maxLen = plugin.getPluginConfig().getMaxPrefixLength();

        if (visibleLen > maxLen) {
            player.sendMessage(Component.text("Превышена максимальная длина префикса!").color(NamedTextColor.RED));
            return;
        }

        plugin.getPrefixManager().setPrefix(targetUuid, prefixStr, isEditingOther);

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null) {
            targetPlayer.getScheduler().execute(plugin, () -> {
                plugin.getTabManager().updatePlayerListName(targetPlayer);
            }, () -> {}, 1L);
        }

        if (isEditingOther) {
            String targetName = Bukkit.getPlayer(targetUuid) != null ? Bukkit.getPlayer(targetUuid).getName() : targetUuid.toString();
            player.sendMessage(Component.text("Префикс для " + targetName + " сохранён!").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Префикс сохранён!").color(NamedTextColor.GREEN));
        }
        editingTarget.remove(uuid);
        editingSegments.remove(uuid);
        gradientStartColor.remove(uuid);
        gradientEndColor.remove(uuid);
        currentColorPage.remove(uuid);
        waitingForHex.remove(uuid);
    }

    private String buildMiniMessageString(List<PrefixSegment> segments) {
        StringBuilder sb = new StringBuilder();
        for (PrefixSegment segment : segments) {
            sb.append(segment.toMiniMessage());
        }
        return sb.toString();
    }

    // упрощённый парсер — покрывает основные случаи
    private List<PrefixSegment> parseMiniMessageToSegments(String miniMsg) {
        List<PrefixSegment> segments = new ArrayList<>();
        if (miniMsg == null || miniMsg.isEmpty()) return segments;

        PrefixSegment segment = new PrefixSegment("");

        java.util.regex.Pattern colorPattern = java.util.regex.Pattern.compile(
            "<(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>([^<]*)</\\1>"
        );
        java.util.regex.Matcher colorMatcher = colorPattern.matcher(miniMsg);
        if (colorMatcher.find()) {
            segment.color = colorMatcher.group(1);
            segment.text = colorMatcher.group(2);
        }

        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("<color:([^>]+)>([^<]*)</color>");
        java.util.regex.Matcher hexMatcher = hexPattern.matcher(miniMsg);
        if (hexMatcher.find()) {
            segment.hexColor = hexMatcher.group(1);
            if (segment.text.isEmpty()) {
                segment.text = hexMatcher.group(2);
            }
        }

        java.util.regex.Pattern gradientPattern = java.util.regex.Pattern.compile("<gradient:([^:]+):([^>]+)>([^<]*)</gradient>");
        java.util.regex.Matcher gradientMatcher = gradientPattern.matcher(miniMsg);
        if (gradientMatcher.find()) {
            segment.gradientStart = gradientMatcher.group(1);
            segment.gradientEnd = gradientMatcher.group(2);
            if (segment.text.isEmpty()) {
                segment.text = gradientMatcher.group(3);
            }
        }

        if (miniMsg.contains("<bold>")) segment.bold = true;
        if (miniMsg.contains("<italic>")) segment.italic = true;
        if (miniMsg.contains("<underlined>")) segment.underlined = true;
        if (miniMsg.contains("<strikethrough>")) segment.strikethrough = true;
        if (miniMsg.contains("<obfuscated>")) segment.obfuscated = true;

        if (segment.text.isEmpty()) {
            String stripped = miniMsg.replaceAll("<[^>]+>", "");
            segment.text = stripped;
        }

        if (!segment.text.isEmpty()) {
            segments.add(segment);
        }

        return segments;
    }

    private void fillBackground(Inventory inv) {
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = bg.getItemMeta();
        meta.displayName(Component.text(""));
        bg.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, bg);
            }
        }
    }

    private ItemStack createNamedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        try {
            meta.displayName(miniMessage.deserialize(name).decoration(TextDecoration.ITALIC, false));
        } catch (Exception e) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        }
        if (lore != null && !lore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                try {
                    loreComponents.add(miniMessage.deserialize(line).decoration(TextDecoration.ITALIC, false));
                } catch (Exception e) {
                    loreComponents.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
                }
            }
            meta.lore(loreComponents);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static class PrefixSegment {
        public String text;
        public String color;
        public String hexColor;
        public boolean bold;
        public boolean italic;
        public boolean underlined;
        public boolean strikethrough;
        public boolean obfuscated;
        public String gradientStart;
        public String gradientEnd;

        public PrefixSegment(String text) {
            this.text = text;
        }

        public String toMiniMessage() {
            StringBuilder sb = new StringBuilder();

            if (gradientStart != null && gradientEnd != null) {
                sb.append("<gradient:").append(gradientStart).append(":").append(gradientEnd).append(">");
            }

            if (hexColor != null) {
                sb.append("<color:").append(hexColor).append(">");
            } else if (color != null) {
                sb.append("<").append(color).append(">");
            }

            if (bold) sb.append("<bold>");
            if (italic) sb.append("<italic>");
            if (underlined) sb.append("<underlined>");
            if (strikethrough) sb.append("<strikethrough>");
            if (obfuscated) sb.append("<obfuscated>");

            sb.append(text);

            if (obfuscated) sb.append("</obfuscated>");
            if (strikethrough) sb.append("</strikethrough>");
            if (underlined) sb.append("</underlined>");
            if (italic) sb.append("</italic>");
            if (bold) sb.append("</bold>");
            if (hexColor != null) {
                sb.append("</color>");
            } else if (color != null) {
                sb.append("</").append(color).append(">");
            }
            if (gradientStart != null && gradientEnd != null) {
                sb.append("</gradient>");
            }

            return sb.toString();
        }
    }
}
