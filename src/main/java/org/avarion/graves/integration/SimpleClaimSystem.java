package org.avarion.graves.integration;

import fr.xyness.SCS.API.SimpleClaimSystemAPI;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class SimpleClaimSystem {

    private final SimpleClaimSystemAPI scs;

    public SimpleClaimSystem(fr.xyness.SCS.SimpleClaimSystem simpleClaimSystemPlugin) {
        SimpleClaimSystemAPI_Provider.initialize(simpleClaimSystemPlugin);
        scs = SimpleClaimSystemAPI_Provider.getAPI();
    }

    public boolean canBuild(Player player, Location target) {
        // Check if the target is in a claim
        Claim claim = scs.getClaimAtChunk(target.getChunk());
        // Check if the player has permission to build in the claim
        return claim == null || (
               !claim.isBanned(player.getUniqueId())
               && claim.getPermissionForPlayer("Build", player)
               && claim.getPermissionForPlayer("Destroy", player)
        );
    }
}
