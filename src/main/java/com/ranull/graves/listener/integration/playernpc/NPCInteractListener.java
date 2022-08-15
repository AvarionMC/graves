package com.ranull.graves.listener.integration.playernpc;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.PlayerNPC;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.UUIDUtil;
import dev.sergiferry.playernpc.api.NPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class NPCInteractListener implements Listener {
    private final Graves plugin;
    private final PlayerNPC playerNPC;

    public NPCInteractListener(Graves plugin, PlayerNPC playerNPC) {
        this.plugin = plugin;
        this.playerNPC = playerNPC;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onNPCInteract(NPC.Events.Interact event) {
        if (event.getClickType() == NPC.Interact.ClickType.RIGHT_CLICK) {
            NPC.Personal npcPersonal = (NPC.Personal) event.getNPC();

            if (npcPersonal.hasGlobal()) {
                NPC.Global npcGlobal = npcPersonal.getGlobal();

                if (npcGlobal.hasCustomData("grave_uuid")) {
                    UUID uuid = UUIDUtil.getUUID(npcGlobal.getCustomData("grave_uuid"));

                    if (uuid != null) {
                        Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);

                        if (grave != null) {
                            event.setCancelled(plugin.getGraveManager().openGrave(event.getPlayer(),
                                    npcGlobal.getLocation(), grave));
                        }
                    }
                }
            }
        }
    }
}
