package com.ranull.graves.compatibility;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.inventory.Grave;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Compatibility {
    BlockData placeBlock(Location location, Material material, Grave grave, Graves plugin);

    boolean canBuild(Player player, Location location, Graves plugin);

    ItemStack getEntitySkullItemStack(Grave grave, Graves plugin);
}
