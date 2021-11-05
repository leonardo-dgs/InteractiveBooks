package net.leonardo_dgs.interactivebooks.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public final class PAPIUtil {

    private static final Plugin PAPI_PLUGIN = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");

    public static String setPlaceholders(String text) {
        return setPlaceholders(null, text);
    }

    public static String setPlaceholders(CommandSender sender, String text) {
        if (isPlaceholderAPISupported()) {
            if (sender instanceof OfflinePlayer) {
                return PlaceholderAPI.setPlaceholders((OfflinePlayer) sender, text);
            } else {
                return PlaceholderAPI.setPlaceholders(null, text);
            }
        } else {
            return ChatColor.translateAlternateColorCodes('&', text);
        }
    }

    private static boolean isPlaceholderAPISupported() {
        return PAPI_PLUGIN != null && PAPI_PLUGIN.isEnabled();
    }
}
