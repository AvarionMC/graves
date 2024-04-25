package com.ranull.graves.compatibility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Collection;

public interface Compatibility {

    BlockData setBlockData(Location location, Material material, Grave grave, Graves plugin);

    boolean canBuild(Player player, Location location, Graves plugin);

    boolean hasTitleData(Block block);

    ItemStack getSkullItemStack(Grave grave, Graves plugin);

    String getSkullTexture(ItemStack itemStack);

    default String getSkullMetaData(ItemStack itemStack) {
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        if (skullMeta == null) {
            return null;
        }

        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");

            profileField.setAccessible(true);

            GameProfile gameProfile = (GameProfile) profileField.get(skullMeta);

            if (gameProfile != null && gameProfile.getProperties().containsKey("textures")) {
                Collection<Property> propertyCollection = gameProfile.getProperties().get("textures");

                if (!propertyCollection.isEmpty()) {
                    return propertyCollection.stream().findFirst().get().getValue();
                }
            }
        }
        catch (NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
        }

        return null;
    }

}
