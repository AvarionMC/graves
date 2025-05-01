package org.avarion.graves.integration;

import fr.xyness.SCS.API.SimpleClaimSystemAPI;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SimpleClaimSystem {

    private static SimpleClaimSystemAPI scs;

    public SimpleClaimSystem(Plugin simpleClaimSystemPlugin) {
        SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem) simpleClaimSystemPlugin);
        scs = SimpleClaimSystemAPI_Provider.getAPI();
    }

    /**
     * @param player Player looking to place a block
     * @param target Place where the player seeks to place a block
     * @return true if he can put the block and destroy it
     */
    public boolean canBuild(Player player, Location target) {
        // Check if the target is in a claim
        Claim claim = scs.getClaimAtChunk(target.getChunk());
        if (claim != null) {
            // Check if the player is banned from the claim
            if (claim.isBanned(player.getUniqueId())){
                return false;
            }
            // Check if the player has permission to build in the claim
            else {
                return claim.getPermissionForPlayer("Build", player)
                    && claim.getPermissionForPlayer("Destroy", player);
            }
        }
        return true;
    }
}
