package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ProtectionLib {
    private final Graves plugin;
    private final Plugin protectionLibPlugin;

    public ProtectionLib(Graves plugin, Plugin protectionLibPlugin) {
        this.plugin = plugin;
        this.protectionLibPlugin = protectionLibPlugin;
    }

    public boolean canBuild(Location location, Player player) {
        if (plugin.getIntegrationManager().hasFurnitureLib()) {
            return plugin.getIntegrationManager().getFurnitureLib().canBuild(location, player);
        } else {
            try {
                Object protectionLib = Class.forName("de.Ste3et_C0st.ProtectionLib.main.ProtectionLib")
                        .cast(protectionLibPlugin);
                Method canBuild = protectionLib.getClass().getMethod("canBuild", location.getClass(),
                        Class.forName("org.bukkit.entity.Player"));

                canBuild.setAccessible(true);

                return (boolean) canBuild.invoke(protectionLib, location, player);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                     | ClassNotFoundException ignored) {
            }
        }

        return true;
    }
}
