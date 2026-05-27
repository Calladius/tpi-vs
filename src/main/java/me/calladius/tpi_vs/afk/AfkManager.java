package me.calladius.tpi_vs.afk;

import me.calladius.tpi_vs.TpiVsPlugin;
import me.calladius.tpi_vs.config.PluginConfig;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkManager implements Listener {

    private final TpiVsPlugin plugin;
    private final Map<UUID, Long> lastActivityTime;
    private final Map<UUID, Boolean> afkStatus;
    private final Map<UUID, Boolean> manualAfk;
    private final Map<UUID, Boolean> pendingMobCheck;
    private ScheduledTask checkTask;

    public AfkManager(TpiVsPlugin plugin) {
        this.plugin = plugin;
        this.lastActivityTime = new ConcurrentHashMap<>();
        this.afkStatus = new ConcurrentHashMap<>();
        this.manualAfk = new ConcurrentHashMap<>();
        this.pendingMobCheck = new ConcurrentHashMap<>();
    }

    public void start() {
        checkTask = Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            t -> checkAfkStatus(),
            1000,
            1000,
            java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    // двухфазная проверка: асинк по таймеру + синк в регионе игрока для проверки мобов
    private void checkAfkStatus() {
        PluginConfig config = plugin.getPluginConfig();
        long timeoutMs = config.getAfkTimeoutSeconds() * 1000L;
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            long lastActivity = lastActivityTime.getOrDefault(uuid, now);
            boolean isCurrentlyAfk = afkStatus.getOrDefault(uuid, false);
            boolean isManualAfk = manualAfk.getOrDefault(uuid, false);
            boolean isPendingMobCheck = pendingMobCheck.getOrDefault(uuid, false);

            if (isManualAfk || isPendingMobCheck) continue;

            long inactiveTime = now - lastActivity;

            if (!isCurrentlyAfk && inactiveTime >= timeoutMs) {
                pendingMobCheck.put(uuid, true);

                player.getScheduler().execute(plugin, () -> {
                    try {
                        if (hasHostileMobsNearby(player)) {
                            player.sendMessage(Component.text("⚠ Нельзя уйти в AFK — рядом враждебные мобы!")
                                .color(NamedTextColor.YELLOW));
                            lastActivityTime.put(uuid, System.currentTimeMillis());
                        } else {
                            setAfkDirect(uuid, true);
                        }
                    } finally {
                        pendingMobCheck.remove(uuid);
                    }
                }, () -> {
                    pendingMobCheck.remove(uuid);
                }, 1L);
            }
        }
    }

    // вызывать только в контексте региона игрока
    private boolean hasHostileMobsNearby(Player player) {
        PluginConfig config = plugin.getPluginConfig();
        int radius = config.getHostileMobRadius();

        try {
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof Monster) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // ставим afk напрямую — мы уже в контексте региона игрока
    private void setAfkDirect(UUID uuid, boolean afk) {
        boolean wasAfk = afkStatus.getOrDefault(uuid, false);
        afkStatus.put(uuid, afk);

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        if (afk && !wasAfk) {
            String playerName = player.getName();
            Component globalMsg = Component.text(playerName + " ушёл в AFK").color(NamedTextColor.GRAY);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(globalMsg);
            }
        } else if (!afk && wasAfk) {
            String playerName = player.getName();
            Component globalMsg = Component.text(playerName + " больше не AFK").color(NamedTextColor.GREEN);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(globalMsg);
            }
            manualAfk.remove(uuid);
        }

        plugin.getTabManager().updatePlayerListName(player);
    }

    // ставим afk через шедулер — если не уверены в контексте
    public void setAfkScheduled(UUID uuid, boolean afk) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            afkStatus.put(uuid, afk);
            return;
        }

        player.getScheduler().execute(plugin, () -> setAfkDirect(uuid, afk), () -> {
            afkStatus.put(uuid, afk);
        }, 1L);
    }

    // переключение из /afk — мы в контексте игрока, проверка мобов ок
    public void toggleAfk(Player player) {
        UUID uuid = player.getUniqueId();
        boolean isCurrentlyAfk = afkStatus.getOrDefault(uuid, false);

        if (isCurrentlyAfk) {
            manualAfk.remove(uuid);
            setAfkDirect(uuid, false);
            updateActivity(uuid);
        } else {
            if (hasHostileMobsNearby(player)) {
                player.sendMessage(Component.text("⚠ Нельзя уйти в AFK — рядом враждебные мобы!")
                    .color(NamedTextColor.YELLOW));
                return;
            }
            manualAfk.put(uuid, true);
            setAfkDirect(uuid, true);
        }
    }

    public void updateActivity(UUID uuid, boolean fromMovement) {
        lastActivityTime.put(uuid, System.currentTimeMillis());

        boolean isAfk = afkStatus.getOrDefault(uuid, false);

        if (isAfk) {
            if (fromMovement) {
                // движение снимает любой афк даже ручной
                setAfkScheduled(uuid, false);
            } else {
                // остальная активность снимает только авто-афк
                boolean isManual = manualAfk.getOrDefault(uuid, false);
                if (!isManual) {
                    setAfkScheduled(uuid, false);
                }
            }
        }
    }

    public void updateActivity(UUID uuid) {
        updateActivity(uuid, false);
    }

    public boolean isAfk(UUID uuid) {
        return afkStatus.getOrDefault(uuid, false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        lastActivityTime.put(uuid, System.currentTimeMillis());
        afkStatus.put(uuid, false);
        manualAfk.remove(uuid);

        player.getScheduler().execute(plugin, () -> {
            plugin.getTabManager().updatePlayer(player);
        }, () -> {}, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getX() != event.getTo().getX() ||
            event.getFrom().getY() != event.getTo().getY() ||
            event.getFrom().getZ() != event.getTo().getZ()) {
            updateActivity(event.getPlayer().getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        updateActivity(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncChatEvent event) {
        updateActivity(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastActivityTime.remove(uuid);
        afkStatus.remove(uuid);
        manualAfk.remove(uuid);
        pendingMobCheck.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!plugin.getPluginConfig().isAfkMobProtection()) return;

        if (event.getTarget() instanceof Player player) {
            if (isAfk(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getPluginConfig().isAfkMobProtection()) return;

        if (event.getEntity() instanceof Player player) {
            if (isAfk(player.getUniqueId())) {
                if (event.getDamager() instanceof Monster) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPush(EntityPushedByEntityAttackEvent event) {
        if (!plugin.getPluginConfig().isAfkMobProtection()) return;

        if (event.getEntity() instanceof Player player) {
            if (isAfk(player.getUniqueId())) {
                if (event.getPushedBy() instanceof Monster) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
