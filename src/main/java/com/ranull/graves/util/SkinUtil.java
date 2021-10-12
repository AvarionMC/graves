package com.ranull.graves.util;

import com.github.joelgodofwar.mmh.MoreMobHeads;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ranull.graves.Graves;
import com.ranull.skulltextureapi.SkullTextureAPI;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.UUID;

public final class SkinUtil {
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

    public static String getTextureBase64(Entity entity, Graves plugin) {
        if (!(entity instanceof Player)) {
            Plugin skullTextureAPIPlugin = plugin.getServer().getPluginManager().getPlugin("SkullTextureAPI");

            if (skullTextureAPIPlugin != null && skullTextureAPIPlugin.isEnabled()
                    && skullTextureAPIPlugin instanceof SkullTextureAPI) {
                try {
                    String base64 = ((SkullTextureAPI) skullTextureAPIPlugin).getTextureBase64(entity);

                    if (base64 != null && !base64.equals("")) {
                        return base64;
                    }
                } catch (NoSuchMethodError exception) {
                    plugin.debugMessage("SkullTextureAPI detected but can't find method getTextureBase64, " +
                            "maybe you are running an outdated version", 1);
                }
            }

            Plugin moreMobHeadsPlugin = plugin.getServer().getPluginManager().getPlugin("MoreMobHeads");

            if (entity instanceof LivingEntity && moreMobHeadsPlugin != null && moreMobHeadsPlugin.isEnabled()
                    && moreMobHeadsPlugin instanceof MoreMobHeads) {
                try {
                    String base64 = ((MoreMobHeads) moreMobHeadsPlugin).getTextureFromEntity((LivingEntity) entity);

                    if (base64 != null && !base64.equals("")) {
                        return base64;
                    }
                } catch (NoSuchMethodError exception) {
                    plugin.debugMessage("MoreMobHeads detected but can't find method getTextureFromEntity, " +
                            "maybe you are running an outdated version", 1);
                }
            }
        }

        return null;
    }
}
