package br.com.purplemc.purpleesconde.utils;

import br.com.purplemc.purpleesconde.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class MessageUtils {

    private static final List<ExternalPlaceholderProvider> externalProviders = new ArrayList<>();

    public static void registerExternalPlaceholderProvider(ExternalPlaceholderProvider provider) {
        externalProviders.add(provider);
    }

    public static void broadcastToArena(Arena arena, String message) {
        if (arena == null || message == null) return;
        for (Player player : arena.getPlayers()) {
            player.sendMessage(applyAllPlaceholders(player, message));
        }
    }

    public static void sendActionBar(Player player, String message) {
        String finalMessage = applyAllPlaceholders(player, message);
        try {
            Object packet = getActionBarPacket(finalMessage);
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
        } catch (Throwable t) {
            player.sendMessage(finalMessage);
        }
    }

    private static Object getActionBarPacket(String message) throws Exception {
        Class<?> chatSerializer = getNMSClass("IChatBaseComponent$ChatSerializer");
        Object icbc = chatSerializer.getMethod("a", String.class)
                .invoke(null, "{\"text\": \"" + ChatColor.translateAlternateColorCodes('&', message) + "\"}");
        Class<?> packetPlayOutChat = getNMSClass("PacketPlayOutChat");
        Constructor<?> constructor = packetPlayOutChat.getConstructor(getNMSClass("IChatBaseComponent"), byte.class);
        return constructor.newInstance(icbc, (byte) 2);
    }

    public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        return Class.forName("net.minecraft.server." + version + "." + name);
    }

    public static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        title = applyAllPlaceholders(player, title);
        subtitle = applyAllPlaceholders(player, subtitle);
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Class<?> packetClass = getNMSClass("PacketPlayOutTitle");
            Class<?> enumTitleAction = getNMSClass("PacketPlayOutTitle$EnumTitleAction");
            Class<?> iChatBaseComponent = getNMSClass("IChatBaseComponent");
            Class<?> chatSerializer = getNMSClass("IChatBaseComponent$ChatSerializer");

            Object titleComponent = chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\": \"" + ChatColor.translateAlternateColorCodes('&', title == null ? "" : title) + "\"}");
            Object subtitleComponent = chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\": \"" + ChatColor.translateAlternateColorCodes('&', subtitle == null ? "" : subtitle) + "\"}");

            Object TIMES = enumTitleAction.getEnumConstants()[2];
            Object TITLE = enumTitleAction.getEnumConstants()[0];
            Object SUBTITLE = enumTitleAction.getEnumConstants()[1];

            Object packetTimes = packetClass.getConstructor(enumTitleAction, iChatBaseComponent, int.class, int.class, int.class)
                    .newInstance(TIMES, null, fadeIn, stay, fadeOut);
            Object packetTitle = packetClass.getConstructor(enumTitleAction, iChatBaseComponent)
                    .newInstance(TITLE, titleComponent);
            Object packetSubtitle = packetClass.getConstructor(enumTitleAction, iChatBaseComponent)
                    .newInstance(SUBTITLE, subtitleComponent);

            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet"));

            sendPacket.invoke(playerConnection, packetTimes);
            sendPacket.invoke(playerConnection, packetTitle);
            sendPacket.invoke(playerConnection, packetSubtitle);
        } catch (Throwable t) {
            if (title != null) player.sendMessage(title);
            if (subtitle != null) player.sendMessage(subtitle);
        }
    }

    public static String applyAllPlaceholders(Player player, String message) {
        String result = message;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                Class<?> placeholderAPI = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                result = (String) placeholderAPI.getMethod("setPlaceholders", Player.class, String.class)
                        .invoke(null, player, result);
            } catch (Throwable ignore) {}
        }
        for (ExternalPlaceholderProvider provider : externalProviders) {
            try {
                result = provider.replace(player, result);
            } catch (Throwable ignore) {}
        }
        return result;
    }

    public interface ExternalPlaceholderProvider {
        String replace(Player player, String message);
    }
}