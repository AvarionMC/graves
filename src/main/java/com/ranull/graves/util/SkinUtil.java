package com.ranull.graves.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.ranull.skulltextureapi.SkullTextureAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

public final class SkinUtil {
    private static String GAMEPROFILE_METHOD;

    public static void setSkullBlockTexture(Skull skull, String name, String base64) {
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);

        gameProfile.getProperties().put("textures", new Property("textures", base64));

        try {
            Field profileField = skull.getClass().getDeclaredField("profile");

            profileField.setAccessible(true);
            profileField.set(skull, gameProfile);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }

    public static String getTexture(Entity entity) {
        if (entity instanceof Player) {
            GameProfile gameProfile = getPlayerGameProfile((Player) entity);

            if (gameProfile != null) {
                PropertyMap propertyMap = gameProfile.getProperties();

                if (propertyMap.containsKey("textures")) {
                    Collection<Property> propertyCollection = propertyMap.get("textures");

                    return !propertyCollection.isEmpty()
                            ? propertyCollection.stream().findFirst().get().getValue() : null;
                }
            }
        } else {
            Plugin skullTextureAPIPlugin = Bukkit.getServer().getPluginManager().getPlugin("SkullTextureAPI");

            if (skullTextureAPIPlugin != null && skullTextureAPIPlugin.isEnabled()
                    && skullTextureAPIPlugin instanceof SkullTextureAPI) {
                try {
                    String base64 = SkullTextureAPI.getTexture(entity);

                    if (base64 != null && !base64.equals("")) {
                        return base64;
                    }
                } catch (NoSuchMethodError ignored) {
                }
            }
        }

        return null;
    }

    public static String getSignature(Entity entity) {
        if (entity instanceof Player) {
            GameProfile gameProfile = getPlayerGameProfile((Player) entity);

            if (gameProfile != null) {
                PropertyMap propertyMap = gameProfile.getProperties();

                if (propertyMap.containsKey("textures")) {
                    Collection<Property> propertyCollection = propertyMap.get("textures");

                    return !propertyCollection.isEmpty()
                            ? propertyCollection.stream().findFirst().get().getSignature() : null;
                }
            }
        }

        return null;
    }

    public static GameProfile getPlayerGameProfile(Player player) {
        try {
            Object playerObject = player.getClass().getMethod("getHandle").invoke(player);

            if (GAMEPROFILE_METHOD == null) {
                findGameProfileMethod(playerObject);
            }

            if (GAMEPROFILE_METHOD != null && !GAMEPROFILE_METHOD.equals("")) {
                Method gameProfile = playerObject.getClass().getMethod(GAMEPROFILE_METHOD);

                gameProfile.setAccessible(true);

                return (GameProfile) gameProfile.invoke(playerObject);
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException ignored) {
        }

        return null;
    }

    private static void findGameProfileMethod(Object playerObject) {
        for (Method method : playerObject.getClass().getMethods()) {
            if (method.getReturnType().getName().endsWith("GameProfile")) {
                GAMEPROFILE_METHOD = method.getName();

                return;
            }
        }

        GAMEPROFILE_METHOD = "";
    }
}
