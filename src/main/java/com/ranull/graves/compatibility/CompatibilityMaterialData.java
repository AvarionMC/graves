package com.ranull.graves.compatibility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.SkinUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Openable;

import java.lang.reflect.Field;
import java.util.UUID;

public final class CompatibilityMaterialData implements Compatibility {
    @Override
    public BlockData placeBlock(Location location, Material material, Grave grave, Graves plugin) {
        if (material != null) {
            Block block = location.getBlock();
            String replaceMaterial = location.getBlock().getType().name();

            // Air
            if (block.getType().name().equals("NETHER_PORTAL") || block.getState().getData() instanceof Openable) {
                replaceMaterial = null;
            }

            // Set type
            location.getBlock().setType(material);

            // Update skull
            if (material.name().equals("SKULL") && block.getState() instanceof Skull) {
                updateSkullBlock(block, grave, plugin);
            }

            return new BlockData(location, grave.getUUID(), replaceMaterial, null);
        }

        return new BlockData(location, grave.getUUID(), null, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canBuild(Player player, Location location, Graves plugin) {
        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(location.getBlock(),
                location.getBlock().getState(), location.getBlock(), player.getItemInHand(),
                player, true);

        plugin.getServer().getPluginManager().callEvent(blockPlaceEvent);

        return blockPlaceEvent.canBuild() && !blockPlaceEvent.isCancelled();
    }

    private void updateSkullBlock(Block block, Grave grave, Graves plugin) {
        int headType = plugin.getConfig("block.head.type", grave).getInt("block.head.type");
        String headBase64 = plugin.getConfig("block.head.base64", grave).getString("block.head.base64");
        String headName = plugin.getConfig("block.head.name", grave).getString("block.head.name");
        Skull skull = (Skull) block.getState();

        skull.setSkullType(SkullType.PLAYER);
        skull.setRotation(BlockFaceUtil.getYawBlockFace(grave.getYaw()).getOppositeFace());

        if (headType == 0) {
            if (grave.getOwnerType() == EntityType.PLAYER) {
                skull.setOwner(grave.getOwnerName());
            } else {
                SkinUtil.setSkullBlockTexture(skull, headBase64);
            }
        } else if (headType == 1 && headBase64 != null && !headBase64.equals("")) {
            SkinUtil.setSkullBlockTexture(skull, headBase64);
        } else if (headType == 2 && headName != null && headName.length() <= 16) {
            skull.setOwner(headName);
        }

        skull.update();
    }

    public ItemStack setSkullItemStackTexture(ItemStack itemStack, String base64) {
        if (itemStack.getType().name().equals("SKULL")) {
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);

            profile.getProperties().put("textures", new Property("textures", base64));

            try {
                Field profileField = skullMeta.getClass().getDeclaredField("profile");

                profileField.setAccessible(true);
                profileField.set(skullMeta, profile);
            } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException exception) {
                exception.printStackTrace();
            }

            itemStack.setItemMeta(skullMeta);
        }

        return itemStack;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getEntitySkullItemStack(Grave grave, Graves plugin) {
        Material material = Material.matchMaterial("SKULL_ITEM");

        if (material != null) {
            ItemStack itemStack = new ItemStack(material, 1, (short) 3);

            if (itemStack.getItemMeta() != null) {
                if (grave.getOwnerType() == EntityType.PLAYER) {
                    SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

                    skullMeta.setOwner(grave.getOwnerName());
                    itemStack.setItemMeta(skullMeta);
                } else {
                    // TODO ENTITY
                }
            }

            return itemStack;
        }

        return null;
    }
}
