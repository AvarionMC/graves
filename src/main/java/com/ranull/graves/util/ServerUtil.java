package com.ranull.graves.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public final class ServerUtil {
    public static String getServerInfoDump(JavaPlugin javaPlugin) {
        List<String> stringList = new ArrayList<>();

        stringList.add("Implementation Name: " + javaPlugin.getServer().getName());
        stringList.add("Implementation Version: " + javaPlugin.getServer().getVersion());
        stringList.add("Bukkit Version: " + javaPlugin.getServer().getBukkitVersion());
        stringList.add("NMS Version: " + javaPlugin.getServer().getClass().getPackage().getName().split("\\.")[3]);
        stringList.add("Player Count: " + javaPlugin.getServer().getOnlinePlayers().size());
        stringList.add("Player List: " + javaPlugin.getServer().getOnlinePlayers().stream().map(Player::getName)
                .collect(Collectors.joining(", ")));
        stringList.add("Plugin Count: " + javaPlugin.getServer().getPluginManager().getPlugins().length);
        stringList.add("Plugin List: " + Arrays.stream(javaPlugin.getServer().getPluginManager().getPlugins())
                .map(Plugin::getName).collect(Collectors.joining(", ")));
        stringList.add(javaPlugin.getDescription().getName() + " Version: "
                + javaPlugin.getDescription().getVersion());
        stringList.add(javaPlugin.getDescription().getName() + " API Version: "
                + javaPlugin.getDescription().getAPIVersion());
        stringList.add(javaPlugin.getDescription().getName() + " Config Version: "
                + javaPlugin.getConfig().getInt("config-version"));
        stringList.add(javaPlugin.getDescription().getName() + " Config Base64: "
                + Base64.getEncoder().encodeToString(javaPlugin.getConfig().saveToString().getBytes()));

        return String.join("\n", stringList);
    }
}
