package ru.kforbro.arkcastle.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Colorize {
    public static void sendMessage(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static void sendMessage(String player, String message) {
        Player p = Bukkit.getPlayerExact(player);
        if (p == null) {
            return;
        }
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static void sendMessage(CommandSender player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subtitle), fadeIn, stay, fadeOut);
    }

    public static void sendTitle(String player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player p = Bukkit.getPlayerExact(player);
        if (p == null) {
            return;
        }
        p.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subtitle), fadeIn, stay, fadeOut);
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static void sendActionBar(String player, String message) {
        Player p = Bukkit.getPlayerExact(player);
        if (p == null) {
            return;
        }
        p.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}