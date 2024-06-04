package org.avarion.graves.listener.integration.playernpc;

import dev.sergiferry.playernpc.api.NPC;
import org.avarion.graves.Graves;
import org.avarion.graves.manager.CacheManager;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.UUIDUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NPCInteractListener implements Listener {

    private final Graves plugin;

    public NPCInteractListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onNPCInteract(NPC.Events.@NotNull Interact event) {
        if (event.getClickType() == NPC.Interact.ClickType.RIGHT_CLICK) {
            NPC.Personal npcPersonal = event.getNPC();

            if (npcPersonal.hasGlobal()) {
                NPC.Global npcGlobal = npcPersonal.getGlobal();

                if (npcGlobal.hasCustomData("grave_uuid")) {
                    UUID uuid = UUIDUtil.getUUID(npcGlobal.getCustomData("grave_uuid"));

                    if (uuid != null) {
                        Grave grave = CacheManager.graveMap.get(uuid);

                        if (grave != null) {
                            event.setCancelled(plugin.getGraveManager()
                                                     .openGrave(event.getPlayer(), npcGlobal.getLocation(), grave));
                        }
                    }
                }
            }
        }
    }

}
