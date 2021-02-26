package net.leonardo_dgs.interactivebooks.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class PAPIUtil {

    private static final Plugin PAPIPLUGIN = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");

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

    public static List<String> setPlaceholders(Player player, List<String> text) {
        if (isPlaceholderAPISupported()) {
            return PlaceholderAPI.setPlaceholders(player, text);
        } else {
            List<String> coloredText = new ArrayList<>();
            for (String s : text)
                coloredText.add(ChatColor.translateAlternateColorCodes('&', s));
            return coloredText;
        }
    }

    private static boolean isPlaceholderAPISupported() {
        return PAPIPLUGIN != null && PAPIPLUGIN.isEnabled();
    }
}
