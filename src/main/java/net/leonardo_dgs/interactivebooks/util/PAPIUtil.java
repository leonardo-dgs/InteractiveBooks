package net.leonardo_dgs.interactivebooks.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public final class PAPIUtil {

    private static final Plugin PAPIPLUGIN = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");

    /**
     * Sets PlaceholderAPI placeholders to a text,
     * or just colorizes it if PlaceholderAPI is not installed.
     *
     * @param text the text on which to set placeholders
     * @return the text with placeholders replaced,
     * or just the text colorized if PlaceholderAPI is not installed (and will not be thrown a {@link ClassNotFoundException})
     */
    public static String setPlaceholders(String text)
    {
        return setPlaceholders(null, text);
    }

    /**
     * Sets PlaceholderAPI placeholders to a text,
     * taking information from the supplied sender if it is an {@link OfflinePlayer},
     * or just colorizes it if PlaceholderAPI is not installed.
     *
     * @param sender the sender from which to take information if it is an {@link OfflinePlayer}
     * @param text   the text on which to set placeholders
     * @return the text with the placeholders replaced, with sender's information if it's an {@link OfflinePlayer},
     * or just the text colorized if PlaceholderAPI is not installed (and will not be thrown a {@link ClassNotFoundException})
     */
    public static String setPlaceholders(CommandSender sender, String text)
    {
        if (isPlaceholderAPISupported())
        {
            if (sender instanceof OfflinePlayer)
            {
                return PlaceholderAPI.setPlaceholders((OfflinePlayer) sender, text);
            }

            return PlaceholderAPI.setPlaceholders(null, text);
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static boolean isPlaceholderAPISupported()
    {
        return PAPIPLUGIN != null && PAPIPLUGIN.isEnabled();
    }

}
