package com.ranull.graves.compatibility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.SkinUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Openable;

import java.lang.reflect.Field;
import java.util.Collection;

public final class CompatibilityMaterialData implements Compatibility {
    @Override
    public BlockData setBlockData(Location location, Material material, Grave grave, Graves plugin) {
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean hasTitleData(Block block) {
        return block.getState() instanceof BlockState;
    }

    @SuppressWarnings("deprecation")
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
                if (!plugin.getVersionManager().is_v1_7()) {
                    SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
                } else {
                    skull.setOwner(grave.getOwnerName());
                }
            }
        } else if (headType == 1 && headBase64 != null && !headBase64.equals("")) {
            if (!plugin.getVersionManager().is_v1_7()) {
                SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
            } else {
                skull.setOwner(grave.getOwnerName());
            }
        } else if (headType == 2 && headName != null && headName.length() <= 16) {
            skull.setOwner(headName);
        }

        skull.update();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getSkullItemStack(Grave grave, Graves plugin) {
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

    @Override
    public String getSkullTexture(ItemStack itemStack) {
        Material material = Material.matchMaterial("SKULL_ITEM");

        if (material != null && itemStack.getType() == material && itemStack.getItemMeta() != null) {
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

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
            } catch (NoSuchFieldException | IllegalAccessException exception) {
                exception.printStackTrace();
            }
        }

        return null;
    }
}
