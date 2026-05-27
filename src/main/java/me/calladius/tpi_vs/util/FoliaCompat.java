package me.calladius.tpi_vs.util;

import org.bukkit.Bukkit;

public final class FoliaCompat {

    private FoliaCompat() {}

    public static double[] getRecentTps() {
        try {
            return Bukkit.getTPS();
        } catch (NoSuchMethodError e) {
            try {
                Class<?> minecraftServerClass = Class.forName("net.minecraft.server.MinecraftServer");
                Object server = minecraftServerClass.getMethod("getServer").invoke(null);
                Object recentTps = minecraftServerClass.getField("recentTps").get(server);
                if (recentTps instanceof double[] tpsArray) {
                    return tpsArray;
                }
            } catch (Exception ex) {
                // ignore
            }
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    public static double getAverageTps() {
        double[] tps = getRecentTps();
        return Math.min(tps[0], 20.0);
    }

    public static String formatTps(double tps) {
        if (tps >= 18.0) return "<green>" + String.format("%.1f", tps);
        if (tps >= 14.0) return "<yellow>" + String.format("%.1f", tps);
        if (tps >= 8.0) return "<red>" + String.format("%.1f", tps);
        return "<dark_red>" + String.format("%.1f", tps);
    }
}
