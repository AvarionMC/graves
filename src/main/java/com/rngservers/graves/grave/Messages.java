package com.rngservers.graves.grave;

import com.rngservers.graves.Main;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Messages {
    private Main plugin;

    public Messages(Main plugin) {
        this.plugin = plugin;
    }

    public void graveLoot(Location location, Player player) {
        String lootMessage = plugin.getConfig().getString("settings.lootMessage")
                .replace("&", "§");
        if (!lootMessage.equals("")) {
            player.sendMessage(lootMessage);
        }
        String lootSound = plugin.getConfig().getString("settings.lootSound");
        if (!lootSound.equals("")) {
            location.getWorld().playSound(location, Sound.valueOf(lootSound.toUpperCase()), 1, 1);
        }
        String lootEffect = plugin.getConfig().getString("settings.lootEffect");
        if (!lootEffect.equals("")) {
            location.getWorld().playEffect(location, Effect.valueOf(lootEffect), 0);
        }
    }

    public void graveOpen(Location location) {
        String graveOpenSound = plugin.getConfig().getString("settings.graveOpenSound");
        if (!graveOpenSound.equals("")) {
            location.getWorld().playSound(location,
                    Sound.valueOf(graveOpenSound.toUpperCase()), 1, 1);
        }
    }

    public void graveClose(Location location) {
        String graveCloseSound = plugin.getConfig().getString("settings.graveCloseSound");
        if (!graveCloseSound.equals("")) {
            location.getWorld().playSound(location,
                    Sound.valueOf(graveCloseSound.toUpperCase()), 1, 1);
        }
    }

    public void graveProtected(Player player, Location location) {
        String graveProtectedMessage = plugin.getConfig().getString("settings.graveProtectedMessage")
                .replace("&", "§");
        if (!graveProtectedMessage.equals("")) {
            player.sendMessage(graveProtectedMessage);
        }
        String graveProtectedSound = plugin.getConfig().getString("settings.graveProtectedSound");
        if (!graveProtectedSound.equals("")) {
            location.getWorld().playSound(location, Sound.valueOf(graveProtectedSound.toUpperCase()), 1, 1);
        }
    }

    public void permissionDenied(Player player) {
        String permissionDenied = plugin.getConfig().getString("settings.permissionDenied")
                .replace("&", "§");
        if (!permissionDenied.equals("")) {
            player.sendMessage(permissionDenied);
        }
    }
}
