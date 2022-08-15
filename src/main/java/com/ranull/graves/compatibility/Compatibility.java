package com.ranull.graves.compatibility;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Compatibility {
    BlockData setBlockData(Location location, Material material, Grave grave, Graves plugin);

    boolean canBuild(Player player, Location location, Graves plugin);

    boolean hasTitleData(Block block);

    ItemStack getSkullItemStack(Grave grave, Graves plugin);

    String getSkullTexture(ItemStack itemStack);
}
