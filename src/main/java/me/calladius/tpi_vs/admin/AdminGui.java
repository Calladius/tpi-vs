package me.calladius.tpi_vs.admin;

import me.calladius.tpi_vs.TpiVsPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

// админская гуи для просмотра инвентаря и эндер-сундука
//
// оба режима: кастомный инвентарь с русским названием
//   - обычные слоты: мс сам обрабатывает клики/драги/дабл-клики
//   - мы только синхроним bulk-снимком на след тике
//   - спец слоты (только инвентарь): отменяем, обрабатываем вручную
public class AdminGui implements Listener {

    private final TpiVsPlugin plugin;

    private final Map<UUID, UUID> viewingInventory;
    private final Map<UUID, UUID> viewingEnder;
    // геймод до открытия гуи
    private final Map<UUID, GameMode> savedGameMode;
    // защита от дублирования синхронизаций
    private final Set<UUID> pendingSync;

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
        this.viewingInventory = new HashMap<>();
        this.viewingEnder = new HashMap<>();
        this.savedGameMode = new HashMap<>();
        this.pendingSync = new HashSet<>();
    }

    // ==================== открытие гуи ====================

    public void openInventoryGui(Player admin, Player target) {
        viewingEnder.remove(admin.getUniqueId());
        viewingInventory.put(admin.getUniqueId(), target.getUniqueId());
        enterInteractMode(admin);

        Inventory inv = Bukkit.createInventory(null, 45, INV_PREFIX + target.getName());

        // спец слоты
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

        // обычные слоты — копии предметов таргета
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
        scheduleRefresh(admin);
    }

    // эндер-сундук — кастомный инвентарь с русским названием
    // мс сам обрабатывает все клики/драги, мы синхроним в таргет
    public void openEnderChestGui(Player admin, Player target) {
        viewingInventory.remove(admin.getUniqueId());
        viewingEnder.put(admin.getUniqueId(), target.getUniqueId());
        enterInteractMode(admin);

        Inventory inv = Bukkit.createInventory(null, 27, ENDER_PREFIX + target.getName());

        ItemStack[] contents = target.getEnderChest().getContents();
        for (int i = 0; i < contents.length && i < 27; i++) {
            if (contents[i] != null) {
                inv.setItem(i, contents[i].clone());
            }
        }

        admin.openInventory(inv);
        scheduleRefresh(admin);
    }

    // ==================== геймод ====================

    // спектатор -> креатив чтоб мог работать с инвентарём
    private void enterInteractMode(Player admin) {
        if (admin.getGameMode() == GameMode.SPECTATOR) {
            savedGameMode.put(admin.getUniqueId(), GameMode.SPECTATOR);
            admin.setGameMode(GameMode.CREATIVE);
            admin.setAllowFlight(true);
            admin.setFlying(true);
        }
    }

    private void restoreGameMode(Player admin) {
        GameMode saved = savedGameMode.remove(admin.getUniqueId());
        if (saved != null) {
            admin.getScheduler().execute(plugin, () -> {
                admin.setGameMode(saved);
            }, () -> {}, 1L);
        }
    }

    // ==================== определение режима просмотра ====================

    // возвращает режим: "inv", "ender" или null
    private String getViewMode(Player admin) {
        UUID uuid = admin.getUniqueId();
        String title = admin.getOpenInventory().getTitle();
        if (title.startsWith(INV_PREFIX) && viewingInventory.containsKey(uuid)) return "inv";
        if (title.startsWith(ENDER_PREFIX) && viewingEnder.containsKey(uuid)) return "ender";
        return null;
    }

    private UUID getTargetUuid(Player admin) {
        UUID uuid = admin.getUniqueId();
        UUID invTarget = viewingInventory.get(uuid);
        if (invTarget != null) return invTarget;
        return viewingEnder.get(uuid);
    }

    // ==================== эвенты ====================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        UUID adminUuid = admin.getUniqueId();

        String title = event.getView().getTitle();
        boolean isInv = title.startsWith(INV_PREFIX) && viewingInventory.containsKey(adminUuid);
        boolean isEnder = title.startsWith(ENDER_PREFIX) && viewingEnder.containsKey(adminUuid);
        if (!isInv && !isEnder) return;

        UUID targetUuid = isInv ? viewingInventory.get(adminUuid) : viewingEnder.get(adminUuid);
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            admin.sendMessage(Component.text("\u0418\u0433\u0440\u043e\u043a \u043f\u043e\u043a\u0438\u043d\u0443\u043b \u0441\u0435\u0440\u0432\u0435\u0440!").color(NamedTextColor.RED));
            admin.closeInventory();
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        boolean clickedTop = rawSlot >= 0 && rawSlot < topSize;
        ClickType click = event.getClick();

        // мидл-клик = дюп в креативе, всегда отменяем
        if (click == ClickType.MIDDLE) {
            event.setCancelled(true);
            return;
        }

        // ===== эндер-сундук =====
        if (isEnder) {
            // все слоты обычные, мс сам обработает
            // только шифт снизу — мс может не найти куда положить, но это ок
            // дроп — дропаем у таргета
            if (clickedTop && (click == ClickType.DROP || click == ClickType.CONTROL_DROP)) {
                event.setCancelled(true);
                handleEnderDrop(admin, target, rawSlot, event);
                return;
            }
            // всё остальное — мс сам, синхроним
            scheduleSync(admin, target, false);
            return;
        }

        // ===== инвентарь =====

        // клик по своему инвентарю (нижнему)
        if (!clickedTop) {
            if (event.isShiftClick()) {
                // шифт снизу — мс может положить в спец слот, обрабатываем сами
                event.setCancelled(true);
                shiftFromBottom(admin, target, event);
            }
            return;
        }

        // клик по спец слоту — отменяем, обрабатываем вручную
        if (SPECIAL_SLOTS.contains(rawSlot)) {
            event.setCancelled(true);
            if (rawSlot == INFO_SLOT || PADDING_SLOTS.contains(rawSlot)) return;
            if (ARMOR_SLOTS.contains(rawSlot)) {
                armorClick(admin, target, rawSlot, event);
            }
            return;
        }

        // дроп из обычного слота — отменяем, дропаем у таргета
        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            handleDrop(admin, target, rawSlot, event);
            return;
        }

        // обычный слот, обычный клик — мс сам обработает!
        scheduleSync(admin, target, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        UUID adminUuid = admin.getUniqueId();

        String title = event.getView().getTitle();
        boolean isInv = title.startsWith(INV_PREFIX) && viewingInventory.containsKey(adminUuid);
        boolean isEnder = title.startsWith(ENDER_PREFIX) && viewingEnder.containsKey(adminUuid);
        if (!isInv && !isEnder) return;

        UUID targetUuid = isInv ? viewingInventory.get(adminUuid) : viewingEnder.get(adminUuid);
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) return;

        // эндер-сундук — все слоты обычные, мс сам разложит
        if (isEnder) {
            scheduleSync(admin, target, false);
            return;
        }

        // инвентарь — проверяем спец слоты
        Inventory topInv = event.getView().getTopInventory();
        int topSize = topInv.getSize();

        boolean hasTopSlots = false;
        boolean hasSpecialSlots = false;
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < topSize) {
                hasTopSlots = true;
                if (SPECIAL_SLOTS.contains(slot)) {
                    hasSpecialSlots = true;
                    break;
                }
            }
        }

        // драг только по нижнему — мс сам разберётся
        if (!hasTopSlots) return;

        // задели спец слот — отменяем весь драг
        if (hasSpecialSlots) {
            event.setCancelled(true);
            return;
        }

        // все слоты обычные — мс сам разложит, синхроним
        scheduleSync(admin, target, true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player admin)) return;
        UUID uuid = admin.getUniqueId();

        UUID invTarget = viewingInventory.remove(uuid);
        UUID enderTarget = viewingEnder.remove(uuid);
        pendingSync.remove(uuid);

        // финальная синхронизация инвентаря
        if (invTarget != null) {
            Player target = Bukkit.getPlayer(invTarget);
            if (target != null && target.isOnline()) {
                syncNowInventory(target, event.getView().getTopInventory());
            }
        }

        // финальная синхронизация эндер-сундука
        if (enderTarget != null) {
            Player target = Bukkit.getPlayer(enderTarget);
            if (target != null && target.isOnline()) {
                syncNowEnder(target, event.getView().getTopInventory());
            }
        }

        restoreGameMode(admin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        UUID quitterUuid = quitter.getUniqueId();

        // таргет вышел — закрываем гуи всем админам кто смотрит
        Iterator<Map.Entry<UUID, UUID>> it = viewingInventory.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            if (entry.getValue().equals(quitterUuid)) {
                it.remove();
                Player admin = Bukkit.getPlayer(entry.getKey());
                if (admin != null && admin.isOnline()) {
                    admin.closeInventory();
                }
            }
        }

        Iterator<Map.Entry<UUID, UUID>> it2 = viewingEnder.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<UUID, UUID> entry = it2.next();
            if (entry.getValue().equals(quitterUuid)) {
                it2.remove();
                Player admin = Bukkit.getPlayer(entry.getKey());
                if (admin != null && admin.isOnline()) {
                    admin.closeInventory();
                }
            }
        }

        // админ вышел — чистим
        viewingInventory.remove(quitterUuid);
        viewingEnder.remove(quitterUuid);
        pendingSync.remove(quitterUuid);
        restoreGameMode(quitter);
    }

    // ==================== клик по броне ====================

    private void armorClick(Player admin, Player target, int guiSlot, InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean isMarker = isMarkerItem(current);
        Inventory topInv = event.getView().getTopInventory();

        switch (event.getClick()) {
            case LEFT:
                if (isEmpty(cursor) && !isMarker && !isEmpty(current)) {
                    topInv.setItem(guiSlot, createArmorMarker(guiSlot));
                    scheduleSync(admin, target, true);
                    setCursorLater(admin, current.clone());
                } else if (!isEmpty(cursor) && (isMarker || isEmpty(current))) {
                    topInv.setItem(guiSlot, cursor.clone());
                    scheduleSync(admin, target, true);
                    setCursorLater(admin, null);
                } else if (!isEmpty(cursor) && !isEmpty(current) && !isMarker) {
                    topInv.setItem(guiSlot, cursor.clone());
                    scheduleSync(admin, target, true);
                    setCursorLater(admin, current.clone());
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
                    scheduleSync(admin, target, true);
                    setCursorLater(admin, taken);
                } else if (!isEmpty(cursor) && (isMarker || isEmpty(current))) {
                    ItemStack placed = cursor.clone();
                    placed.setAmount(1);
                    topInv.setItem(guiSlot, placed);
                    scheduleSync(admin, target, true);
                    ItemStack newCur = cursor.clone();
                    newCur.setAmount(cursor.getAmount() - 1);
                    setCursorLater(admin, newCur.getAmount() > 0 ? newCur : null);
                } else if (!isEmpty(cursor) && !isMarker && !isEmpty(current)) {
                    if (cursor.isSimilar(current) && current.getAmount() < current.getMaxStackSize()) {
                        ItemStack merged = current.clone();
                        merged.setAmount(current.getAmount() + 1);
                        topInv.setItem(guiSlot, merged);
                        scheduleSync(admin, target, true);
                        ItemStack newCur = cursor.clone();
                        newCur.setAmount(cursor.getAmount() - 1);
                        setCursorLater(admin, newCur.getAmount() > 0 ? newCur : null);
                    } else {
                        topInv.setItem(guiSlot, cursor.clone());
                        scheduleSync(admin, target, true);
                        setCursorLater(admin, current.clone());
                    }
                }
                break;

            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                if (!isMarker && !isEmpty(current)) {
                    topInv.setItem(guiSlot, createArmorMarker(guiSlot));
                    scheduleSync(admin, target, true);
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
                    scheduleSync(admin, target, true);
                    dropForTarget(target, dropped);
                }
                break;

            case CONTROL_DROP:
                if (!isMarker && !isEmpty(current)) {
                    topInv.setItem(guiSlot, createArmorMarker(guiSlot));
                    scheduleSync(admin, target, true);
                    dropForTarget(target, current.clone());
                }
                break;

            default:
                break;
        }
    }

    // ==================== дроп из обычного слота инвентаря ====================

    private void handleDrop(Player admin, Player target, int guiSlot, InventoryClickEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        ItemStack current = topInv.getItem(guiSlot);
        if (isEmpty(current)) return;

        if (event.getClick() == ClickType.DROP) {
            ItemStack dropped = current.clone();
            dropped.setAmount(1);
            ItemStack left = current.clone();
            left.setAmount(current.getAmount() - 1);
            topInv.setItem(guiSlot, left.getAmount() > 0 ? left : null);
            dropForTarget(target, dropped);
        } else {
            topInv.setItem(guiSlot, null);
            dropForTarget(target, current.clone());
        }
        scheduleSync(admin, target, true);
    }

    // ==================== дроп из слота эндер-сундука ====================

    private void handleEnderDrop(Player admin, Player target, int slot, InventoryClickEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        ItemStack current = topInv.getItem(slot);
        if (isEmpty(current)) return;

        if (event.getClick() == ClickType.DROP) {
            ItemStack dropped = current.clone();
            dropped.setAmount(1);
            ItemStack left = current.clone();
            left.setAmount(current.getAmount() - 1);
            topInv.setItem(slot, left.getAmount() > 0 ? left : null);
            dropForTarget(target, dropped);
        } else {
            topInv.setItem(slot, null);
            dropForTarget(target, current.clone());
        }
        scheduleSync(admin, target, false);
    }

    // ==================== шифт клик из инвентаря админа ====================

    private void shiftFromBottom(Player admin, Player target, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (isEmpty(item)) return;

        Inventory topInv = admin.getOpenInventory().getTopInventory();

        // ищем пустой обычный слот в топе (9-44)
        int guiSlot = -1;
        // сначала пытаемся найти слот где есть такой же предмет (стак)
        for (int i = 9; i <= 44; i++) {
            ItemStack slotItem = topInv.getItem(i);
            if (!isEmpty(slotItem) && slotItem.isSimilar(item) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                guiSlot = i;
                break;
            }
        }
        // потом пустой слот
        if (guiSlot < 0) {
            for (int i = 9; i <= 44; i++) {
                if (isEmpty(topInv.getItem(i))) {
                    guiSlot = i;
                    break;
                }
            }
        }

        if (guiSlot >= 0) {
            ItemStack existing = topInv.getItem(guiSlot);
            if (!isEmpty(existing) && existing.isSimilar(item)) {
                int total = existing.getAmount() + item.getAmount();
                int max = existing.getMaxStackSize();
                if (total <= max) {
                    ItemStack merged = existing.clone();
                    merged.setAmount(total);
                    topInv.setItem(guiSlot, merged);
                    event.setCurrentItem(new ItemStack(Material.AIR));
                } else {
                    ItemStack merged = existing.clone();
                    merged.setAmount(max);
                    topInv.setItem(guiSlot, merged);
                    item.setAmount(total - max);
                }
            } else {
                topInv.setItem(guiSlot, item.clone());
                event.setCurrentItem(new ItemStack(Material.AIR));
            }
            scheduleSync(admin, target, true);
        } else {
            admin.sendMessage(Component.text("\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c \u0438\u0433\u0440\u043e\u043a\u0430 \u043f\u043e\u043b\u043e\u043d!").color(NamedTextColor.RED));
        }
    }

    // ==================== синхронизация ====================

    // отложенная — на следующем тике, с дедупликацией
    private void scheduleSync(Player admin, Player target, boolean isInventory) {
        if (pendingSync.add(admin.getUniqueId())) {
            admin.getScheduler().execute(plugin, () -> {
                pendingSync.remove(admin.getUniqueId());
                if (!admin.isOnline()) return;
                String title = admin.getOpenInventory().getTitle();
                if (isInventory && title.startsWith(INV_PREFIX)) {
                    syncNowInventory(target, admin.getOpenInventory().getTopInventory());
                } else if (!isInventory && title.startsWith(ENDER_PREFIX)) {
                    syncNowEnder(target, admin.getOpenInventory().getTopInventory());
                }
            }, () -> {}, 1L);
        }
    }

    // bulk синхронизация инвентаря
    private void syncNowInventory(Player target, Inventory topInv) {
        // снимок обычных слотов (9-35 -> 9-35, 36-44 -> 0-8)
        ItemStack[] normalItems = new ItemStack[36];
        for (int guiSlot = 9; guiSlot <= 35; guiSlot++) {
            ItemStack item = topInv.getItem(guiSlot);
            normalItems[guiSlot] = isEmpty(item) ? null : item.clone();
        }
        for (int guiSlot = 36; guiSlot <= 44; guiSlot++) {
            ItemStack item = topInv.getItem(guiSlot);
            normalItems[guiSlot - 36] = isEmpty(item) ? null : item.clone();
        }

        // снимок брони + восстановление маркеров если слот опустел
        ItemStack helmet = null, chestplate = null, leggings = null, boots = null, offhand = null;
        for (int slot : ARMOR_SLOTS) {
            ItemStack item = topInv.getItem(slot);
            if (!isEmpty(item) && !isMarkerItem(item)) {
                switch (slot) {
                    case HELMET_SLOT -> helmet = item.clone();
                    case CHESTPLATE_SLOT -> chestplate = item.clone();
                    case LEGGINGS_SLOT -> leggings = item.clone();
                    case BOOTS_SLOT -> boots = item.clone();
                    case OFFHAND_SLOT -> offhand = item.clone();
                }
            }
            if (isEmpty(item)) {
                topInv.setItem(slot, createArmorMarker(slot));
            }
        }

        final ItemStack fH = helmet, fC = chestplate, fL = leggings, fB = boots, fO = offhand;
        target.getScheduler().execute(plugin, () -> {
            if (!target.isOnline()) return;
            for (int i = 0; i < 36; i++) {
                target.getInventory().setItem(i, normalItems[i]);
            }
            target.getInventory().setHelmet(fH);
            target.getInventory().setChestplate(fC);
            target.getInventory().setLeggings(fL);
            target.getInventory().setBoots(fB);
            if (fO != null) {
                target.getInventory().setItemInOffHand(fO);
            } else {
                target.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
        }, () -> {}, 1L);
    }

    // bulk синхронизация эндер-сундука
    private void syncNowEnder(Player target, Inventory topInv) {
        ItemStack[] snapshot = new ItemStack[27];
        for (int i = 0; i < 27; i++) {
            ItemStack item = topInv.getItem(i);
            snapshot[i] = isEmpty(item) ? null : item.clone();
        }

        target.getScheduler().execute(plugin, () -> {
            if (!target.isOnline()) return;
            for (int i = 0; i < 27; i++) {
                target.getEnderChest().setItem(i, snapshot[i]);
            }
        }, () -> {}, 1L);
    }

    // ==================== обновление гуи из таргета ====================

    // периодическое обновление — каждые 20 тиков (~1 сек) читаем инвентарь таргета
    // и обновляем гуи админа, чтоб видеть изменения таргета в реальном времени
    // самоостанавливается когда админ закрывает гуи
    private void scheduleRefresh(Player admin) {
        UUID adminUuid = admin.getUniqueId();

        admin.getScheduler().execute(plugin, () -> {
            if (!admin.isOnline()) return;

            String title = admin.getOpenInventory().getTitle();
            boolean isInv = title.startsWith(INV_PREFIX) && viewingInventory.containsKey(adminUuid);
            boolean isEnder = title.startsWith(ENDER_PREFIX) && viewingEnder.containsKey(adminUuid);

            // больше не смотрим — останов
            if (!isInv && !isEnder) return;

            // админ только что сделал действие, синк ещё pending — пропустим
            if (pendingSync.contains(adminUuid)) {
                scheduleRefresh(admin);
                return;
            }

            UUID targetUuid = isInv ? viewingInventory.get(adminUuid) : viewingEnder.get(adminUuid);
            Player target = Bukkit.getPlayer(targetUuid);
            if (target == null || !target.isOnline()) {
                scheduleRefresh(admin);
                return;
            }

            // читаем инвентарь таргета на его потоке, потом обновляем гуи на потоке админа
            if (isInv) {
                target.getScheduler().execute(plugin, () -> {
                    if (!target.isOnline()) { scheduleRefresh(admin); return; }

                    // снимок инвентаря таргета
                    final ItemStack[] snapshot = new ItemStack[41];
                    ItemStack[] contents = target.getInventory().getContents();
                    for (int i = 0; i < 36 && i < contents.length; i++) {
                        snapshot[i] = contents[i] != null ? contents[i].clone() : null;
                    }
                    snapshot[36] = !isEmpty(target.getInventory().getHelmet()) ? target.getInventory().getHelmet().clone() : null;
                    snapshot[37] = !isEmpty(target.getInventory().getChestplate()) ? target.getInventory().getChestplate().clone() : null;
                    snapshot[38] = !isEmpty(target.getInventory().getLeggings()) ? target.getInventory().getLeggings().clone() : null;
                    snapshot[39] = !isEmpty(target.getInventory().getBoots()) ? target.getInventory().getBoots().clone() : null;
                    snapshot[40] = !isEmpty(target.getInventory().getItemInOffHand()) ? target.getInventory().getItemInOffHand().clone() : null;

                    // обновляем гуи админа
                    admin.getScheduler().execute(plugin, () -> {
                        if (!admin.isOnline()) return;
                        if (pendingSync.contains(adminUuid)) { scheduleRefresh(admin); return; }

                        Inventory topInv = admin.getOpenInventory().getTopInventory();
                        // обычные слоты
                        for (int i = 9; i <= 35; i++) {
                            topInv.setItem(i, snapshot[i] != null ? snapshot[i].clone() : null);
                        }
                        for (int i = 0; i <= 8; i++) {
                            topInv.setItem(36 + i, snapshot[i] != null ? snapshot[i].clone() : null);
                        }
                        // броня
                        topInv.setItem(HELMET_SLOT, snapshot[36] != null ? snapshot[36].clone() : createArmorMarker(HELMET_SLOT));
                        topInv.setItem(CHESTPLATE_SLOT, snapshot[37] != null ? snapshot[37].clone() : createArmorMarker(CHESTPLATE_SLOT));
                        topInv.setItem(LEGGINGS_SLOT, snapshot[38] != null ? snapshot[38].clone() : createArmorMarker(LEGGINGS_SLOT));
                        topInv.setItem(BOOTS_SLOT, snapshot[39] != null ? snapshot[39].clone() : createArmorMarker(BOOTS_SLOT));
                        topInv.setItem(OFFHAND_SLOT, snapshot[40] != null ? snapshot[40].clone() : createArmorMarker(OFFHAND_SLOT));

                        scheduleRefresh(admin);
                    }, () -> scheduleRefresh(admin), 1L);
                }, () -> scheduleRefresh(admin), 1L);
            } else {
                // эндер-сундук
                target.getScheduler().execute(plugin, () -> {
                    if (!target.isOnline()) { scheduleRefresh(admin); return; }

                    final ItemStack[] snapshot = new ItemStack[27];
                    ItemStack[] contents = target.getEnderChest().getContents();
                    for (int i = 0; i < 27 && i < contents.length; i++) {
                        snapshot[i] = contents[i] != null ? contents[i].clone() : null;
                    }

                    admin.getScheduler().execute(plugin, () -> {
                        if (!admin.isOnline()) return;
                        if (pendingSync.contains(adminUuid)) { scheduleRefresh(admin); return; }

                        Inventory topInv = admin.getOpenInventory().getTopInventory();
                        for (int i = 0; i < 27; i++) {
                            topInv.setItem(i, snapshot[i] != null ? snapshot[i].clone() : null);
                        }

                        scheduleRefresh(admin);
                    }, () -> scheduleRefresh(admin), 1L);
                }, () -> scheduleRefresh(admin), 1L);
            }
        }, () -> {}, 20L);
    }

    // ==================== хелперы ====================

    // установить курсор через тик — при отмене ивента setCursor не работает сразу
    private void setCursorLater(Player admin, ItemStack item) {
        ItemStack cursor = item != null && item.getType() != Material.AIR ? item.clone() : new ItemStack(Material.AIR);
        admin.getScheduler().execute(plugin, () -> {
            admin.setItemOnCursor(cursor);
        }, () -> {}, 1L);
    }

    private void dropForTarget(Player target, ItemStack item) {
        target.getScheduler().execute(plugin, () -> {
            // спавним на уровне глаз, даём скорость вперёд как при обычном Q
            org.bukkit.Location eye = target.getEyeLocation();
            org.bukkit.entity.Item dropped = target.getWorld().dropItem(eye, item);
            org.bukkit.util.Vector vel = target.getLocation().getDirection().multiply(0.3F);
            vel.setY(vel.getY() + 0.15F); // чуть вверх
            dropped.setVelocity(vel);
            dropped.setPickupDelay(40);
        }, () -> {}, 1L);
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
