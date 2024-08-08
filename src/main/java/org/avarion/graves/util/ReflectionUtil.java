package org.avarion.graves.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionUtil {

    private ReflectionUtil() {
        // Don't do anything here
    }

    public static void swingMainHand(@NotNull Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
            Method sendPacket = playerConnection.getClass().getMethod("sendPacket", getClass("Packet"));
            Object packetPlayOutAnimation = getClass("PacketPlayOutAnimation").getConstructor(getClass("Entity"), int.class)
                                                                              .newInstance(entityPlayer, 0);

            sendPacket.invoke(playerConnection, packetPlayOutAnimation);
        }
        catch (IllegalAccessException |
               InvocationTargetException |
               NoSuchMethodException |
               NoSuchFieldException |
               ClassNotFoundException |
               InstantiationException ignored) {
        }
    }

    public static @NotNull Class<?> getClass(String clazz) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + Bukkit.getServer()
                                                             .getClass()
                                                             .getPackage()
                                                             .getName()
                                                             .replace(".", ",")
                                                             .split(",")[3] + "." + clazz);
    }

}
