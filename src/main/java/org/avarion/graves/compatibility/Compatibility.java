package org.avarion.graves.compatibility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.avarion.graves.Graves;
import org.avarion.graves.data.BlockData;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.SkinUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public interface Compatibility {

    BlockData setBlockData(Location location, Material material, Grave grave, Graves plugin);

    boolean canBuild(Player player, Location location, Graves plugin);

    boolean hasTitleData(Block block);

    ItemStack getSkullItemStack(Grave grave, Graves plugin);

    String getSkullTexture(ItemStack itemStack);

    default String getSkullMetaData(@NotNull ItemStack itemStack) {
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        if (skullMeta == null) {
            return null;
        }

        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            GameProfile gameProfile = (GameProfile) profileField.get(skullMeta);

            if (gameProfile != null) {
                return SkinUtil.GET_PROPERTIES.apply(gameProfile).get("textures").stream()
                                              .findFirst()
                                              .map(SkinUtil.GET_VALUE)
                                              .orElse(null);
            }
        }
        catch (NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
        }

        return null;
    }

}