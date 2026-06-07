package me.calladius.tpi_vs.admin;

import me.calladius.tpi_vs.TpiVsPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class AdminGui implements Listener {

    private final TpiVsPlugin plugin;
    private final MiniMessage miniMessage;

    private final Map<UUID, UUID> viewingInventory;
    private final Map<UUID, UUID> viewingEnder;

    private static final String INV_PREFIX = "Инвентарь: ";
    private static final String ENDER_PREFIX = "Эндер-сундук: ";

    private static final int INFO_SLOT = 0;
    private static final int HELMET_SLOT = 2;
    private static final int CHESTPLATE_SLOT = 3;
    private static final int LEGGINGS_SLOT = 4;
    private static final int BOOTS_SLOT = 5;
    private static final int OFFHAND_SLOT = 6;

    private static final Set<Integer> PADDING_SLOTS = Set.of(1, 7, 8);
    private static final Set<Integer> ARMOR_SLOTS = Set.of(HELMET_SLOT, CHESTPLATE_SLOT, LEGGINGS_SLOT, BOOTS_SLOT, OFFHAND_SLOT);
    private static final Set<Integer> SPECIAL_SLOTS;
    static {
        SPECIAL_SLOTS = new HashSet<>();
        SPECIAL_SLOTS.add(INFO_SLOT);
        SPECIAL_SLOTS.addAll(PADDING_SLOTS);
        SPECIAL_SLOTS.addAll(ARMOR_SLOTS);
    }

    public AdminGui(TpiVsPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.viewingInventory = new HashMap<>();
        this.viewingEnder = new HashMap<>();
    }

    public void openInventoryGui(Player admin, Player target) {
        viewingInventory.put(admin.getUniqueId(), target.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 45, INV_PREFIX + target.getName());

        inv.setItem(INFO_SLOT, createInfoItem(target));
        inv.setItem(HELMET_SLOT, createArmorSlotItem(target.getInventory().getHelmet(), "Шлем", Material.IRON_HELMET));
        inv.setItem(CHESTPLATE_SLOT, createArmorSlotItem(target.getInventory().getChestplate(), "Нагрудник", Material.IRON_CHESTPLATE));
        inv.setItem(LEGGINGS_SLOT, createArmorSlotItem(target.getInventory().getLeggings(), "Поножи", Material.IRON_LEGGINGS));
        inv.setItem(BOOTS_SLOT, createArmorSlotItem(target.getInventory().getBoots(), "Ботинки", Material.IRON_BOOTS));
        inv.setItem(OFFHAND_SLOT, createArmorSlotItem(target.getInventory().getItemInOffHand(), "Вторая рука", Material.SHIELD));

        ItemStack padding = createPaddingItem();
        for (int slot : PADDING_SLOTS) {
            inv.setItem(slot, padding);
        }

        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 9; i <= 35; i++) {
            if (i < contents.length && contents[i] != null) {
                inv.setItem(i, contents[i].clone());
            }
        }

        for (int i = 0; i <= 8; i++) {
            if (i < contents.length && contents[i] != null) {
                inv.setItem(36 + i, contents[i].clone());
            }
        }

        admin.openInventory(inv);
    }

    public void openEnderChestGui(Player admin, Player target) {
        viewingEnder.put(admin.getUniqueId(), target.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 27, ENDER_PREFIX + target.getName());

        ItemStack[] contents = target.getEnderChest().getContents();
        for (int i = 0; i < contents.length && i < 27; i++) {
            if (contents[i] != null) {
                inv.setItem(i, contents[i].clone());
            }
        }

        admin.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;

        UUID adminUuid = admin.getUniqueId();
        String title = event.getView().getTitle();

        UUID targetUuid = null;
        boolean isInventory = false;
        boolean isEnder = false;

        if (title.startsWith(INV_PREFIX)) {
            targetUuid = viewingInventory.get(adminUuid);
            isInventory = true;
        } else if (title.startsWith(ENDER_PREFIX)) {
            targetUuid = viewingEnder.get(adminUuid);
            isEnder = true;
        }

        if (targetUuid == null) return;

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            admin.sendMessage(Component.text("Игрок покинул сервер!").color(NamedTextColor.RED));
            admin.closeInventory();
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        boolean clickedTop = rawSlot >= 0 && rawSlot < topSize;

        if (!clickedTop) {
            if (event.isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                event.setCancelled(true);
                handleShiftClickToTarget(admin, target, event, isInventory, isEnder);
            }
            return;
        }

        if (isEnder) {
            handleEnderClick(admin, target, event, rawSlot);
        } else {
            handleInventoryClick(admin, target, event, rawSlot);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player admin)) return;
        UUID adminUuid = admin.getUniqueId();
        viewingInventory.remove(adminUuid);
        viewingEnder.remove(adminUuid);
    }

    // драг ивент — перетаскивание с зажатой кнопкой, просто отменяем чтоб не было десинка
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;

        String title = event.getView().getTitle();
        boolean isInventory = title.startsWith(INV_PREFIX);
        boolean isEnder = title.startsWith(ENDER_PREFIX);
        if (!isInventory && !isEnder) return;

        // если хоть один слот из верхнего инвентаря — отменяем
        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleInventoryClick(Player admin, Player target, InventoryClickEvent event, int guiSlot) {
        if (guiSlot == INFO_SLOT) {
            event.setCancelled(true);
            return;
        }

        if (PADDING_SLOTS.contains(guiSlot)) {
            event.setCancelled(true);
            return;
        }

        if (ARMOR_SLOTS.contains(guiSlot)) {
            event.setCancelled(true);
            handleArmorSlotClick(admin, target, guiSlot, event);
            return;
        }

        event.setCancelled(true);
        handleRegularSlotClick(admin, target, guiSlot, event);
    }

    private void handleEnderClick(Player admin, Player target, InventoryClickEvent event, int guiSlot) {
        event.setCancelled(true);

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (event.isShiftClick()) {
            if (current != null && current.getType() != Material.AIR) {
                admin.getInventory().addItem(current.clone());
                event.setCurrentItem(new ItemStack(Material.AIR));
                syncEnderSlot(target, guiSlot, null);
            }
            return;
        }

        if (isEmptyCursor(cursor)) {
            if (current != null && current.getType() != Material.AIR) {
                event.setCursor(current.clone());
                event.setCurrentItem(new ItemStack(Material.AIR));
                syncEnderSlot(target, guiSlot, null);
            }
        } else {
            if (current == null || current.getType() == Material.AIR) {
                event.setCurrentItem(cursor.clone());
                event.setCursor(new ItemStack(Material.AIR));
                syncEnderSlot(target, guiSlot, cursor.clone());
            } else {
                event.setCursor(current.clone());
                event.setCurrentItem(cursor.clone());
                syncEnderSlot(target, guiSlot, cursor.clone());
            }
        }
    }

    private void handleRegularSlotClick(Player admin, Player target, int guiSlot, InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        int targetSlot = mapGuiSlotToTargetSlot(guiSlot);
        if (targetSlot < 0) return;

        if (event.isShiftClick()) {
            if (current != null && current.getType() != Material.AIR) {
                admin.getInventory().addItem(current.clone());
                event.setCurrentItem(new ItemStack(Material.AIR));
                syncTargetSlot(target, targetSlot, null);
            }
            return;
        }

        if (isEmptyCursor(cursor)) {
            if (current != null && current.getType() != Material.AIR) {
                event.setCursor(current.clone());
                event.setCurrentItem(new ItemStack(Material.AIR));
                syncTargetSlot(target, targetSlot, null);
            }
        } else {
            if (current == null || current.getType() == Material.AIR) {
                event.setCurrentItem(cursor.clone());
                event.setCursor(new ItemStack(Material.AIR));
                syncTargetSlot(target, targetSlot, cursor.clone());
            } else {
                event.setCursor(current.clone());
                event.setCurrentItem(cursor.clone());
                syncTargetSlot(target, targetSlot, cursor.clone());
            }
        }
    }

    private void handleArmorSlotClick(Player admin, Player target, int guiSlot, InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean isMarker = isMarkerItem(current);

        if (event.isShiftClick()) {
            if (!isMarker && current != null && current.getType() != Material.AIR) {
                admin.getInventory().addItem(current.clone());
                event.setCurrentItem(createArmorMarker(guiSlot));
                syncArmorSlot(target, guiSlot, null);
            }
            return;
        }

        if (isEmptyCursor(cursor)) {
            if (!isMarker && current != null && current.getType() != Material.AIR) {
                event.setCursor(current.clone());
                event.setCurrentItem(createArmorMarker(guiSlot));
                syncArmorSlot(target, guiSlot, null);
            }
        } else {
            if (isMarker || current == null || current.getType() == Material.AIR) {
                event.setCurrentItem(cursor.clone());
                event.setCursor(new ItemStack(Material.AIR));
                syncArmorSlot(target, guiSlot, cursor.clone());
            } else {
                event.setCursor(current.clone());
                event.setCurrentItem(cursor.clone());
                syncArmorSlot(target, guiSlot, cursor.clone());
            }
        }
    }

    private void handleShiftClickToTarget(Player admin, Player target, InventoryClickEvent event, boolean isInventory, boolean isEnder) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (isEnder) {
            int slot = findEmptyEnderSlot(target);
            if (slot >= 0) {
                syncEnderSlot(target, slot, item.clone());
                event.setCurrentItem(new ItemStack(Material.AIR));
                admin.getOpenInventory().getTopInventory().setItem(slot, item.clone());
            } else {
                admin.sendMessage(Component.text("Эндер-сундук полон!").color(NamedTextColor.RED));
            }
        } else {
            int slot = findEmptyInventorySlot(target);
            if (slot >= 0) {
                syncTargetSlot(target, slot, item.clone());
                event.setCurrentItem(new ItemStack(Material.AIR));
                int guiSlot = mapTargetSlotToGuiSlot(slot);
                if (guiSlot >= 0) {
                    admin.getOpenInventory().getTopInventory().setItem(guiSlot, item.clone());
                }
            } else {
                admin.sendMessage(Component.text("Инвентарь игрока полон!").color(NamedTextColor.RED));
            }
        }
    }

    // маппинг слотов: строки 2-4 (9-35) = таргет 9-35, строка 5 (36-44) = таргет 0-8
    private int mapGuiSlotToTargetSlot(int guiSlot) {
        if (guiSlot >= 9 && guiSlot <= 35) return guiSlot;
        if (guiSlot >= 36 && guiSlot <= 44) return guiSlot - 36;
        return -1;
    }

    private int mapTargetSlotToGuiSlot(int targetSlot) {
        if (targetSlot >= 9 && targetSlot <= 35) return targetSlot;
        if (targetSlot >= 0 && targetSlot <= 8) return targetSlot + 36;
        return -1;
    }

    private void syncTargetSlot(Player target, int targetSlot, ItemStack item) {
        target.getScheduler().execute(plugin, () -> {
            target.getInventory().setItem(targetSlot, item);
        }, () -> {}, 1L);
    }

    private void syncEnderSlot(Player target, int slot, ItemStack item) {
        target.getScheduler().execute(plugin, () -> {
            target.getEnderChest().setItem(slot, item);
        }, () -> {}, 1L);
    }

    private void syncArmorSlot(Player target, int guiSlot, ItemStack item) {
        target.getScheduler().execute(plugin, () -> {
            switch (guiSlot) {
                case HELMET_SLOT -> target.getInventory().setHelmet(item);
                case CHESTPLATE_SLOT -> target.getInventory().setChestplate(item);
                case LEGGINGS_SLOT -> target.getInventory().setLeggings(item);
                case BOOTS_SLOT -> target.getInventory().setBoots(item);
                case OFFHAND_SLOT -> target.getInventory().setItemInOffHand(item);
            }
        }, () -> {}, 1L);
    }

    private int findEmptyInventorySlot(Player target) {
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < 36; i++) {
            if (contents[i] == null || contents[i].getType() == Material.AIR) return i;
        }
        return -1;
    }

    private int findEmptyEnderSlot(Player target) {
        ItemStack[] contents = target.getEnderChest().getContents();
        for (int i = 0; i < 27; i++) {
            if (contents[i] == null || contents[i].getType() == Material.AIR) return i;
        }
        return -1;
    }

    private ItemStack createArmorSlotItem(ItemStack armorItem, String slotName, Material markerMaterial) {
        if (armorItem != null && armorItem.getType() != Material.AIR) {
            return armorItem.clone();
        } else {
            return createArmorMarker(slotName, markerMaterial);
        }
    }

    private ItemStack createArmorMarker(int guiSlot) {
        return switch (guiSlot) {
            case HELMET_SLOT -> createArmorMarker("Шлем", Material.IRON_HELMET);
            case CHESTPLATE_SLOT -> createArmorMarker("Нагрудник", Material.IRON_CHESTPLATE);
            case LEGGINGS_SLOT -> createArmorMarker("Поножи", Material.IRON_LEGGINGS);
            case BOOTS_SLOT -> createArmorMarker("Ботинки", Material.IRON_BOOTS);
            case OFFHAND_SLOT -> createArmorMarker("Вторая рука", Material.SHIELD);
            default -> createArmorMarker("?", Material.BARRIER);
        };
    }

    private ItemStack createArmorMarker(String slotName, Material material) {
        ItemStack marker = new ItemStack(material);
        ItemMeta meta = marker.getItemMeta();
        meta.displayName(Component.text(slotName).color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Положите предмет сюда").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        marker.setItemMeta(meta);
        return marker;
    }

    private boolean isMarkerItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        List<Component> lore = item.lore();
        if (lore == null) return false;
        for (Component line : lore) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
            if (plain.contains("Положите предмет сюда")) return true;
        }
        return false;
    }

    private ItemStack createInfoItem(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(target);
            meta.displayName(Component.text(target.getName()).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("❤ " + String.format("%.1f", target.getHealth())).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("⭐ Уровень: " + target.getLevel()).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("🎮 " + target.getGameMode().name()).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPaddingItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(""));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isEmptyCursor(ItemStack cursor) {
        return cursor == null || cursor.getType() == Material.AIR;
    }
}
