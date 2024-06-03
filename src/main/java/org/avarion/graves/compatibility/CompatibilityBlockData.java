package org.avarion.graves.compatibility;

import org.avarion.graves.Graves;
import org.avarion.graves.data.BlockData;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.BlockFaceUtil;
import org.avarion.graves.util.MaterialUtil;
import org.avarion.graves.util.SkinUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CompatibilityBlockData implements Compatibility {
    @Contract("_, null, _, _ -> new")
    @Override
    public @NotNull BlockData setBlockData(Location location, Material material, Grave grave, Graves plugin) {
        if (material != null) {
            Block block = location.getBlock();
            String originalMaterial = block.getType().name();
            String replaceMaterial = location.getBlock().getType().name();
            String replaceData = location.getBlock().getBlockData().clone().getAsString(true);

            // Levelled
            if (block.getBlockData() instanceof Levelled leveled) {

                if (leveled.getLevel() != 0) {
                    replaceMaterial = null;
                    replaceData = null;
                }
            }

            // Air
            if (block.getType() == Material.NETHER_PORTAL || block.getBlockData() instanceof Openable) {
                replaceMaterial = null;
                replaceData = null;
            }

            // Set type
            location.getBlock().setType(material);

            // Waterlogged
            if (block.getBlockData() instanceof Waterlogged waterlogged) {

                waterlogged.setWaterlogged(MaterialUtil.isWater(originalMaterial));
                block.setBlockData(waterlogged);
            }

            // Update skull
            if (material == Material.PLAYER_HEAD && block.getState() instanceof Skull) {
                updateSkullBlock(block, grave, plugin);
            }

            return new BlockData(location, grave.getUUID(), replaceMaterial, replaceData);
        }

        return new BlockData(location, grave.getUUID(), null, null);
    }

    @Override
    public boolean canBuild(Player player, @NotNull Location location, @NotNull Graves plugin) {
        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(location.getBlock(), location.getBlock()
                                                                                           .getState(), location.getBlock(), player.getInventory()
                                                                                                                                   .getItemInMainHand(), player, true, EquipmentSlot.HAND);

        plugin.getServer().getPluginManager().callEvent(blockPlaceEvent);

        return blockPlaceEvent.canBuild() && !blockPlaceEvent.isCancelled();
    }

    @Override
    public boolean hasTitleData(@NotNull Block block) {
        return block.getState() instanceof TileState;
    }

    @SuppressWarnings("deprecation")
    private void updateSkullBlock(@NotNull Block block, Grave grave, @NotNull Graves plugin) {
        int headType = plugin.getConfigInt("block.head.type", grave);
        String headBase64 = plugin.getConfigString("block.head.base64", grave);
        String headName = plugin.getConfigString("block.head.name", grave);
        Skull skull = (Skull) block.getState();
        Rotatable skullRotate = (Rotatable) block.getBlockData();

        skullRotate.setRotation(BlockFaceUtil.getYawBlockFace(grave.getYaw()).getOppositeFace());
        skull.setBlockData(skullRotate);

        if (headType == 0) {
            if (grave.getOwnerType() == EntityType.PLAYER) {
                skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(grave.getOwnerUUID()));
            }
            else if (grave.getOwnerTexture() != null) {
                SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), grave.getOwnerTexture());
            }
            else if (headBase64 != null && !headBase64.isEmpty()) {
                SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
            }
        }
        else if (headType == 1 && headBase64 != null && !headBase64.isEmpty()) {
            SkinUtil.setSkullBlockTexture(skull, grave.getOwnerName(), headBase64);
        }
        else if (headType == 2 && headName != null && headName.length() <= 16) {
            skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(headName));
        }

        skull.update();
    }

    @Override
    public @NotNull ItemStack getSkullItemStack(Grave grave, Graves plugin) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);

        if (itemStack.getItemMeta() != null) {
            if (grave.getOwnerType() == EntityType.PLAYER) {
                OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(grave.getOwnerUUID());
                SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

                skullMeta.setOwningPlayer(offlinePlayer);
                itemStack.setItemMeta(skullMeta);
            }
            else {
                // TODO ENTITY
            }
        }

        return itemStack;
    }

    @Override
    public @Nullable String getSkullTexture(@NotNull ItemStack itemStack) {
        if (itemStack.getType() == Material.PLAYER_HEAD) {
            return getSkullMetaData(itemStack);
        }

        return null;
    }
}
