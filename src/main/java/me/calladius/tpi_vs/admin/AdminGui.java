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
import org.bukkit.event.player.PlayerQuitEvent;
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
    // виртуальный курсор — предмет "в руке" админа, работает в спектаторе
    private final Map<UUID, ItemStack> virtualCursor;

    private static final String INV_PREFIX = "\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c: ";
    private static final String ENDER_PREFIX = "\u042d\u043d\u0434\u0435\u0440-\u0441\u0443\u043d\u0434\u0443\u043a: ";

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
        this.virtualCursor = new HashMap<>();
    }

    // получить виртуальный курсор
    private ItemStack getVirtualCursor(Player admin) {
        return virtualCursor.get(admin.getUniqueId());
    }

    // установить виртуальный курсор и показать в action bar
    private void setVirtualCursor(Player admin, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            virtualCursor.remove(admin.getUniqueId());
        } else {
            virtualCursor.put(admin.getUniqueId(), item.clone());
        }
        showCursorInfo(admin);
    }

    // показать что админ держит в action bar
    private void showCursorInfo(Player admin) {
        ItemStack cursor = getVirtualCursor(admin);
        if (cursor != null) {
            String name = cursor.hasItemMeta() && cursor.getItemMeta().hasDisplayName()
                ? cursor.getItemMeta().getDisplayName() : cursor.getType().name();
            admin.sendActionBar(Component.text("\u270b " + cursor.getAmount() + "x " + name).color(NamedTextColor.YELLOW));
        } else {
            admin.sendActionBar(Component.text("\u270b \u0440\u0443\u043a\u0430 \u043f\u0443\u0441\u0442\u0430").color(NamedTextColor.GRAY));
        }
    }

    public void openInventoryGui(Player admin, Player target) {
        viewingInventory.put(admin.getUniqueId(), target.getUniqueId());
        virtualCursor.remove(admin.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 45, INV_PREFIX + target.getName());

        inv.setItem(INFO_SLOT, createInfoItem(target));
        inv.setItem(HELMET_SLOT, createArmorSlotItem(target.getInventory().getHelmet(), "\u0428\u043b\u0435\u043c", Material.IRON_HELMET));
        inv.setItem(CHESTPLATE_SLOT, createArmorSlotItem(target.getInventory().getChestplate(), "\u041d\u0430\u0433\u0440\u0443\u0434\u043d\u0438\u043a", Material.IRON_CHESTPLATE));
        inv.setItem(LEGGINGS_SLOT, createArmorSlotItem(target.getInventory().getLeggings(), "\u041f\u043e\u043d\u043e\u0436\u0438", Material.IRON_LEGGINGS));
        inv.setItem(BOOTS_SLOT, createArmorSlotItem(target.getInventory().getBoots(), "\u0411\u043e\u0442\u0438\u043d\u043a\u0438", Material.IRON_BOOTS));
        inv.setItem(OFFHAND_SLOT, createArmorSlotItem(target.getInventory().getItemInOffHand(), "\u0412\u0442\u043e\u0440\u0430\u044f \u0440\u0443\u043a\u0430", Material.SHIELD));

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
        showCursorInfo(admin);
    }

    public void openEnderChestGui(Player admin, Player target) {
        viewingEnder.put(admin.getUniqueId(), target.getUniqueId());
        virtualCursor.remove(admin.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 27, ENDER_PREFIX + target.getName());

        ItemStack[] contents = target.getEnderChest().getContents();
        for (int i = 0; i < contents.length && i < 27; i++) {
            if (contents[i] != null) {
                inv.setItem(i, contents[i].clone());
            }
        }

        admin.openInventory(inv);
        showCursorInfo(admin);
    }

    // ==================== эвенты ====================

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
            admin.sendMessage(Component.text("\u0418\u0433\u0440\u043e\u043a \u043f\u043e\u043a\u0438\u043d\u0443\u043b \u0441\u0435\u0440\u0432\u0435\u0440!").color(NamedTextColor.RED));
            admin.closeInventory();
            return;
        }

        // отменяем всё — рулим сами через виртуальный курсор
        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        boolean clickedTop = rawSlot >= 0 && rawSlot < topSize;

        if (!clickedTop) {
            // клик по своему инвентарю
            if (event.isShiftClick()) {
                shiftFromBottom(admin, target, event, isInventory, isEnder);
            }
            return;
        }

        // клик по верхнему инвентарю
        if (isInventory) {
            if (rawSlot == INFO_SLOT || PADDING_SLOTS.contains(rawSlot)) return;
            if (ARMOR_SLOTS.contains(rawSlot)) {
                armorClick(admin, target, rawSlot, event);
                return;
            }
        }

        slotClick(admin, target, rawSlot, event, isInventory, isEnder);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;

        String title = event.getView().getTitle();
        boolean isInventory = title.startsWith(INV_PREFIX);
        boolean isEnder = title.startsWith(ENDER_PREFIX);
        if (!isInventory && !isEnder) return;

        UUID targetUuid = isInventory ? viewingInventory.get(admin.getUniqueId()) : viewingEnder.get(admin.getUniqueId());
        if (targetUuid == null) return;

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) return;

        Inventory topInv = event.getView().getTopInventory();
        int topSize = topInv.getSize();

        List<Integer> topSlots = new ArrayList<>();
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < topSize) topSlots.add(slot);
        }
        if (topSlots.isEmpty()) return;

        event.setCancelled(true);

        ItemStack cursor = getVirtualCursor(admin);
        if (isEmpty(cursor)) return;

        if (isInventory) {
            topSlots.removeIf(s -> SPECIAL_SLOTS.contains(s));
        }
        if (topSlots.isEmpty()) return;

        int cursorAmt = cursor.getAmount();
        int slotCount = topSlots.size();
        int perSlot = cursorAmt / slotCount;
        int remainder = cursorAmt % slotCount;

        if (perSlot == 0 && remainder == 0) return;

        int totalPlaced = 0;
        for (int i = 0; i < topSlots.size(); i++) {
            int slot = topSlots.get(i);
            int amount = perSlot + (i < remainder ? 1 : 0);
            if (amount <= 0) continue;

            ItemStack existing = topInv.getItem(slot);
            if (isEmpty(existing)) {
                ItemStack placed = cursor.clone();
                placed.setAmount(amount);
                topInv.setItem(slot, placed);
                totalPlaced += amount;
            } else if (existing.isSimilar(cursor)) {
                int canAdd = Math.min(amount, existing.getMaxStackSize() - existing.getAmount());
                if (canAdd > 0) {
                    ItemStack merged = existing.clone();
                    merged.setAmount(existing.getAmount() + canAdd);
                    topInv.setItem(slot, merged);
                    totalPlaced += canAdd;
                }
            }

            int targetSlot = isInventory ? mapGuiSlotToTargetSlot(slot) : slot;
            if (targetSlot >= 0) {
                syncSlot(target, targetSlot, topInv.getItem(slot), isEnder);
            }
        }

        int newAmt = cursorAmt - totalPlaced;
        if (newAmt > 0) {
            ItemStack newCursor = cursor.clone();
            newCursor.setAmount(newAmt);
            setVirtualCursor(admin, newCursor);
        } else {
            setVirtualCursor(admin, null);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player admin)) return;
        UUID uuid = admin.getUniqueId();
        viewingInventory.remove(uuid);
        viewingEnder.remove(uuid);
        // если админ что-то держал в виртуальном курсоре — дропаем ему
        ItemStack cursor = virtualCursor.remove(uuid);
        if (cursor != null) {
            admin.getInventory().addItem(cursor);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        viewingInventory.remove(uuid);
        viewingEnder.remove(uuid);
        virtualCursor.remove(uuid);
    }

    // ==================== клик по слоту ====================

    private void slotClick(Player admin, Player target, int guiSlot, InventoryClickEvent event, boolean isInventory, boolean isEnder) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = getVirtualCursor(admin);
        int targetSlot = isInventory ? mapGuiSlotToTargetSlot(guiSlot) : guiSlot;
        if (isInventory && targetSlot < 0) return;

        Inventory topInv = event.getView().getTopInventory();

        switch (event.getClick()) {
            case LEFT:
                if (isEmpty(cursor) && !isEmpty(current)) {
                    // берём весь стак в виртуальный курсор
                    topInv.setItem(guiSlot, null);
                    syncSlot(target, targetSlot, null, isEnder);
                    setVirtualCursor(admin, current.clone());
                } else if (!isEmpty(cursor) && isEmpty(current)) {
                    // кладём весь стак из виртуального курсора
                    topInv.setItem(guiSlot, cursor.clone());
                    syncSlot(target, targetSlot, cursor.clone(), isEnder);
                    setVirtualCursor(admin, null);
                } else if (!isEmpty(cursor) && !isEmpty(current)) {
                    if (cursor.isSimilar(current)) {
                        int total = current.getAmount() + cursor.getAmount();
                        int max = current.getMaxStackSize();
                        if (total <= max) {
                            ItemStack merged = current.clone();
                            merged.setAmount(total);
                            topInv.setItem(guiSlot, merged);
                            syncSlot(target, targetSlot, merged, isEnder);
                            setVirtualCursor(admin, null);
                        } else {
                            ItemStack merged = current.clone();
                            merged.setAmount(max);
                            topInv.setItem(guiSlot, merged);
                            syncSlot(target, targetSlot, merged, isEnder);
                            ItemStack rem = cursor.clone();
                            rem.setAmount(total - max);
                            setVirtualCursor(admin, rem);
                        }
                    } else {
                        // свап
                        topInv.setItem(guiSlot, cursor.clone());
                        syncSlot(target, targetSlot, cursor.clone(), isEnder);
                        setVirtualCursor(admin, current.clone());
                    }
                }
                break;

            case RIGHT:
                if (isEmpty(cursor) && !isEmpty(current)) {
                    // берём половину
                    int half = (current.getAmount() + 1) / 2;
                    ItemStack taken = current.clone();
                    taken.setAmount(half);
                    ItemStack left = current.clone();
                    left.setAmount(current.getAmount() - half);
                    topInv.setItem(guiSlot, left.getAmount() > 0 ? left : null);
                    syncSlot(target, targetSlot, left.getAmount() > 0 ? left : null, isEnder);
                    setVirtualCursor(admin, taken);
                } else if (!isEmpty(cursor) && isEmpty(current)) {
                    // кладём 1 штуку
                    ItemStack placed = cursor.clone();
                    placed.setAmount(1);
                    topInv.setItem(guiSlot, placed);
                    syncSlot(target, targetSlot, placed, isEnder);
                    ItemStack newCur = cursor.clone();
                    newCur.setAmount(cursor.getAmount() - 1);
                    setVirtualCursor(admin, newCur.getAmount() > 0 ? newCur : null);
                } else if (!isEmpty(cursor) && !isEmpty(current)) {
                    if (cursor.isSimilar(current) && current.getAmount() < current.getMaxStackSize()) {
                        ItemStack merged = current.clone();
                        merged.setAmount(current.getAmount() + 1);
                        topInv.setItem(guiSlot, merged);
                        syncSlot(target, targetSlot, merged, isEnder);
                        ItemStack newCur = cursor.clone();
                        newCur.setAmount(cursor.getAmount() - 1);
                        setVirtualCursor(admin, newCur.getAmount() > 0 ? newCur : null);
                    } else {
                        topInv.setItem(guiSlot, cursor.clone());
                        syncSlot(target, targetSlot, cursor.clone(), isEnder);
                        setVirtualCursor(admin, current.clone());
                    }
                }
                break;

            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                // шифт из верхнего — в инвентарь админа
                if (!isEmpty(current)) {
                    topInv.setItem(guiSlot, null);
                    syncSlot(target, targetSlot, null, isEnder);
                    admin.getInventory().addItem(current.clone());
                }
                break;

            case DROP:
                if (!isEmpty(current)) {
                    ItemStack dropped = current.clone();
                    dropped.setAmount(1);
                    ItemStack left = current.clone();
                    left.setAmount(current.getAmount() - 1);
                    topInv.setItem(guiSlot, left.getAmount() > 0 ? left : null);
                    syncSlot(target, targetSlot, left.getAmount() > 0 ? left : null, isEnder);
                    dropForTarget(target, dropped);
                }
                break;

            case CONTROL_DROP:
                if (!isEmpty(current)) {
                    topInv.setItem(guiSlot, null);
                    syncSlot(target, targetSlot, null, isEnder);
                    dropForTarget(target, current.clone());
                }
                break;

            case NUMBER_KEY:
                int hotbar = event.getHotbarButton();
                if (hotbar < 0) break;
                ItemStack hotbarItem = admin.getInventory().getItem(hotbar);
                topInv.setItem(guiSlot, !isEmpty(hotbarItem) ? hotbarItem.clone() : null);
                syncSlot(target, targetSlot, !isEmpty(hotbarItem) ? hotbarItem.clone() : null, isEnder);
                admin.getInventory().setItem(hotbar, !isEmpty(current) ? current.clone() : null);
                break;

            case SWAP_OFFHAND:
                ItemStack offhand = admin.getInventory().getItemInOffHand();
                topInv.setItem(guiSlot, !isEmpty(offhand) ? offhand.clone() : null);
                syncSlot(target, targetSlot, !isEmpty(offhand) ? offhand.clone() : null, isEnder);
                admin.getInventory().setItemInOffHand(!isEmpty(current) ? current.clone() : new ItemStack(Material.AIR));
                break;

            default:
                break;
        }
    }

    // ==================== клик по слоту брони ====================

    private void armorClick(Player admin, Player target, int guiSlot, InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = getVirtualCursor(admin);
        boolean isMarker = isMarkerItem(current);
        Inventory topInv = event.getView().getTopInventory();

        switch (event.getClick()) {
            case LEFT:
                if (isEmpty(cursor) && !isMarker && !isEmpty(current)) {
                    topInv.setItem(guiSlot, createArmorMarker(guiSlot));
                    syncArmorSlot(target, guiSlot, null);
                    setVirtualCursor(admin, current.clone());
                } else if (!isEmpty(cursor) && (isMarker || isEmpty(current))) {
                    topInv.setItem(guiSlot, cursor.clone());
                    syncArmorSlot(target, guiSlot, cursor.clone());
                    setVirtualCursor(admin, null);
                } else if (!isEmpty(cursor) && !isEmpty(current) && !isMarker) {
                    topInv.setItem(guiSlot, cursor.clone());
                    syncArmorSlot(target, guiSlot, cursor.clone());
                    setVirtualCursor(admin, current.clone());
                }
                break;

            case RIGHT:
                if (isEmpty(cursor) && !isMarker && !isEmpty(current)) {
                    int half = (current.getAmount() + 1) / 2;
                    ItemStack taken = current.clone();
                    taken.setAmount(half);
                    ItemStack left = current.clone();
                    left.setAmount(current.getAmount() - half);
                    topInv.setItem(guiSlot, left.getAmount() > 0 ? left : createArmorMarker(guiSlot));
                    syncArmorSlot(target, guiSlot, left.getAmount() > 0 ? left : null);
                    setVirtualCursor(admin, taken);
                } else if (!isEmpty(cursor) && (isMarker || isEmpty(current))) {
                    ItemStack placed = cursor.clone();
                    placed.setAmount(1);
                    topInv.setItem(guiSlot, placed);
                    syncArmorSlot(target, guiSlot, placed);
                    ItemStack newCur = cursor.clone();
                    newCur.setAmount(cursor.getAmount() - 1);
                    setVirtualCursor(admin, newCur.getAmount() > 0 ? newCur : null);
                } else if (!isEmpty(cursor) && !isMarker && !isEmpty(current)) {
                    if (cursor.isSimilar(current) && current.getAmount() < current.getMaxStackSize()) {
                        ItemStack merged = current.clone();
                        merged.setAmount(current.getAmount() + 1);
                        topInv.setItem(guiSlot, merged);
                        syncArmorSlot(target, guiSlot, merged);
                        ItemStack newCur = cursor.clone();
                        newCur.setAmount(cursor.getAmount() - 1);
                        setVirtualCursor(admin, newCur.getAmount() > 0 ? newCur : null);
                    } else {
                        topInv.setItem(guiSlot, cursor.clone());
                        syncArmorSlot(target, guiSlot, cursor.clone());
                        setVirtualCursor(admin, current.clone());
                    }
                }
                break;

            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                if (!isMarker && !isEmpty(current)) {
                    topInv.setItem(guiSlot, createArmorMarker(guiSlot));
                    syncArmorSlot(target, guiSlot, null);
                    admin.getInventory().addItem(current.clone());
                }
                break;

            case DROP:
                if (!isMarker && !isEmpty(current)) {
                    ItemStack dropped = current.clone();
                    dropped.setAmount(1);
                    ItemStack left = current.clone();
                    left.setAmount(current.getAmount() - 1);
                    topInv.setItem(guiSlot, left.getAmount() > 0 ? left : createArmorMarker(guiSlot));
                    syncArmorSlot(target, guiSlot, left.getAmount() > 0 ? left : null);
                    dropForTarget(target, dropped);
                }
                break;

            case CONTROL_DROP:
                if (!isMarker && !isEmpty(current)) {
                    topInv.setItem(guiSlot, createArmorMarker(guiSlot));
                    syncArmorSlot(target, guiSlot, null);
                    dropForTarget(target, current.clone());
                }
                break;

            default:
                break;
        }
    }

    // ==================== шифт клик из инвентаря админа в таргет ====================

    private void shiftFromBottom(Player admin, Player target, InventoryClickEvent event, boolean isInventory, boolean isEnder) {
        ItemStack item = event.getCurrentItem();
        if (isEmpty(item)) return;

        Inventory topInv = admin.getOpenInventory().getTopInventory();

        if (isEnder) {
            int slot = findEmptyEnderSlot(target);
            if (slot >= 0) {
                syncEnderSlot(target, slot, item.clone());
                topInv.setItem(slot, item.clone());
                event.setCurrentItem(new ItemStack(Material.AIR));
            } else {
                admin.sendMessage(Component.text("\u042d\u043d\u0434\u0435\u0440-\u0441\u0443\u043d\u0434\u0443\u043a \u043f\u043e\u043b\u043e\u043d!").color(NamedTextColor.RED));
            }
        } else {
            int slot = findEmptyInventorySlot(target);
            if (slot >= 0) {
                syncTargetSlot(target, slot, item.clone());
                int guiSlot = mapTargetSlotToGuiSlot(slot);
                if (guiSlot >= 0) topInv.setItem(guiSlot, item.clone());
                event.setCurrentItem(new ItemStack(Material.AIR));
            } else {
                admin.sendMessage(Component.text("\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c \u0438\u0433\u0440\u043e\u043a\u0430 \u043f\u043e\u043b\u043e\u043d!").color(NamedTextColor.RED));
            }
        }
    }

    // ==================== хелперы ====================

    private void syncSlot(Player target, int targetSlot, ItemStack item, boolean isEnder) {
        if (isEnder) {
            syncEnderSlot(target, targetSlot, item);
        } else {
            syncTargetSlot(target, targetSlot, item);
        }
    }

    private void dropForTarget(Player target, ItemStack item) {
        target.getScheduler().execute(plugin, () -> {
            target.getWorld().dropItemNaturally(target.getLocation(), item);
        }, () -> {}, 1L);
    }

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
            if (isEmpty(contents[i])) return i;
        }
        return -1;
    }

    private int findEmptyEnderSlot(Player target) {
        ItemStack[] contents = target.getEnderChest().getContents();
        for (int i = 0; i < 27; i++) {
            if (isEmpty(contents[i])) return i;
        }
        return -1;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    // ==================== создание предметов для гуи ====================

    private ItemStack createArmorSlotItem(ItemStack armorItem, String slotName, Material markerMaterial) {
        if (!isEmpty(armorItem)) {
            return armorItem.clone();
        } else {
            return createArmorMarker(slotName, markerMaterial);
        }
    }

    private ItemStack createArmorMarker(int guiSlot) {
        return switch (guiSlot) {
            case HELMET_SLOT -> createArmorMarker("\u0428\u043b\u0435\u043c", Material.IRON_HELMET);
            case CHESTPLATE_SLOT -> createArmorMarker("\u041d\u0430\u0433\u0440\u0443\u0434\u043d\u0438\u043a", Material.IRON_CHESTPLATE);
            case LEGGINGS_SLOT -> createArmorMarker("\u041f\u043e\u043d\u043e\u0436\u0438", Material.IRON_LEGGINGS);
            case BOOTS_SLOT -> createArmorMarker("\u0411\u043e\u0442\u0438\u043d\u043a\u0438", Material.IRON_BOOTS);
            case OFFHAND_SLOT -> createArmorMarker("\u0412\u0442\u043e\u0440\u0430\u044f \u0440\u0443\u043a\u0430", Material.SHIELD);
            default -> createArmorMarker("?", Material.BARRIER);
        };
    }

    private ItemStack createArmorMarker(String slotName, Material material) {
        ItemStack marker = new ItemStack(material);
        ItemMeta meta = marker.getItemMeta();
        meta.displayName(Component.text(slotName).color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("\u041f\u043e\u043b\u043e\u0436\u0438\u0442\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0441\u044e\u0434\u0430").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        marker.setItemMeta(meta);
        return marker;
    }

    private boolean isMarkerItem(ItemStack item) {
        if (isEmpty(item) || !item.hasItemMeta()) return false;
        List<Component> lore = item.lore();
        if (lore == null) return false;
        for (Component line : lore) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
            if (plain.contains("\u041f\u043e\u043b\u043e\u0436\u0438\u0442\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0441\u044e\u0434\u0430")) return true;
        }
        return false;
    }

    private ItemStack createInfoItem(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(target);
            meta.displayName(Component.text(target.getName()).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("\u2764 " + String.format("%.1f", target.getHealth())).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("\u2b50 \u0423\u0440\u043e\u0432\u0435\u043d\u044c: " + target.getLevel()).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("\ud83c\udfae " + target.getGameMode().name()).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
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
}
