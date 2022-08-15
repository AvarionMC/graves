package com.ranull.graves.listener.integration.towny;

import com.palmergames.bukkit.towny.event.TownBlockTypeRegisterEvent;
import com.ranull.graves.integration.Towny;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class TownBlockTypeRegisterListener implements Listener {
    private final Towny towny;

    public TownBlockTypeRegisterListener(Towny towny) {
        this.towny = towny;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTownBlockTypeRegister(TownBlockTypeRegisterEvent ignored) {
        towny.registerGraveyardBlockType();
    }
}
