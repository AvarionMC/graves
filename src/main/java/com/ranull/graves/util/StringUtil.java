package com.ranull.graves.util;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtil {
    public static String format(String string) {
        return capitalizeFully(string.replace("_", " "));
    }

    public static String parseString(String string, Graves plugin) {
        return parseString(string, null, null, null, null, plugin);
    }

    public static String parseString(String string, Entity entity, Graves plugin) {
        return parseString(string, entity, null, null, null, plugin);
    }

    public static String parseString(String string, String name, Graves plugin) {
        return parseString(string, null, name, null, null, plugin);
    }

    public static String parseString(String string, Grave grave, Graves plugin) {
        return parseString(string, null, null, null, grave, plugin);
    }

    public static String parseString(String string, Location location, Grave grave, Graves plugin) {
        return parseString(string, null, null, location, grave, plugin);
    }

    public static String parseString(String string, Entity entity, Location location, Grave grave, Graves plugin) {
        return parseString(string, entity, plugin.getEntityManager().getEntityName(entity), location, grave, plugin);
    }

    public static String parseString(String string, Entity entity, String name, Location location, Grave grave,
                                     Graves plugin) {
        if (location != null) {
            string = string.replace("%world%",
                            location.getWorld() != null ? location.getWorld().getName() : "")
                    .replace("%x%", String.valueOf(location.getBlockX() + 0.5))
                    .replace("%y%", String.valueOf(location.getBlockY() + 0.5))
                    .replace("%z%", String.valueOf(location.getBlockZ() + 0.5));

            if (string.contains("%distance%") && entity.getWorld().equals(location.getWorld())) {
                string = string.replace("%distance%",
                        String.valueOf(Math.round(entity.getLocation().distance(location))));
            }

            if (string.contains("%teleport_cost%")) {
                string = string.replace("%teleport_cost%", String.valueOf(plugin.getEntityManager()
                        .getTeleportCost(location, grave.getLocationDeath(), grave)));
            }
        }

        if (grave != null) {
            string = string.replace("%uuid%", grave.getUUID().toString())
                    .replace("%owner_name%", grave.getOwnerName() != null
                            ? grave.getOwnerName() : "")
                    .replace("%owner_name_display%", grave.getOwnerNameDisplay() != null
                            ? grave.getOwnerNameDisplay() : (grave.getOwnerName() != null
                            ? grave.getOwnerName() : ""))
                    .replace("%owner_type%", grave.getOwnerType() != null
                            ? grave.getOwnerType().name() : "")
                    .replace("%owner_uuid%", grave.getOwnerUUID() != null
                            ? grave.getOwnerUUID().toString() : "")
                    .replace("%killer_name%", grave.getKillerName() != null
                            ? grave.getKillerName() : "")
                    .replace("%killer_name_display%", grave.getKillerNameDisplay() != null
                            ? grave.getKillerNameDisplay() : (grave.getKillerName() != null
                            ? grave.getKillerName() : ""))
                    .replace("%killer_type%", grave.getKillerType() != null
                            ? grave.getKillerType().name() : "")
                    .replace("%killer_uuid%", grave.getKillerUUID() != null
                            ? grave.getKillerUUID().toString() : "")
                    .replace("%time_creation%",
                            String.valueOf(grave.getTimeCreation()))
                    .replace("%time_creation_formatted%",
                            getDateString(grave, grave.getTimeCreation(), plugin))
                    .replace("%time_alive_remaining%",
                            String.valueOf(grave.getTimeAliveRemaining()))
                    .replace("%time_alive_remaining_formatted%",
                            getTimeString(grave, grave.getTimeAliveRemaining(), plugin))
                    .replace("%time_protection_remaining%",
                            String.valueOf(grave.getTimeProtectionRemaining()))
                    .replace("%time_protection_remaining_formatted%",
                            getTimeString(grave, grave.getTimeProtectionRemaining(), plugin))
                    .replace("%time_lived%",
                            String.valueOf(grave.getLivedTime()))
                    .replace("%time_lived_formatted%",
                            getTimeString(grave, grave.getLivedTime(), plugin))
                    .replace("%state_protection%",
                            grave.getProtection() && (grave.getTimeProtectionRemaining() > 0
                                    || grave.getTimeProtectionRemaining() < 0) ? plugin
                                    .getConfig("protection.state.protected", grave)
                                    .getString("protection.state.protected", "Protected") : plugin
                                    .getConfig("protection.state.unprotected", grave)
                                    .getString("protection.state.unprotected", "Unprotected"))
                    .replace("%item%", String.valueOf(grave.getItemAmount()));
            if (grave.getExperience() > 0) {
                string = string.replace("%level%", String.valueOf(ExperienceUtil
                                .getLevelFromExperience(grave.getExperience())))
                        .replace("%experience%", String.valueOf(grave.getExperience()));
            } else {
                string = string.replace("%level%", "0")
                        .replace("%experience%", "0");
            }

            if (grave.getOwnerType() == EntityType.PLAYER && plugin.getIntegrationManager().hasPlaceholderAPI()) {
                string = PlaceholderAPI.setPlaceholders(plugin.getServer()
                        .getOfflinePlayer(grave.getOwnerUUID()), string);
            }
        }

        if (location != null && location.getWorld() != null && grave != null) {
            string = string.replace("%world_formatted%",
                    location.getWorld() != null ? plugin.getConfig("message.world."
                            + location.getWorld().getName(), grave).getString("message.world."
                            + location.getWorld().getName(), StringUtil.format(location.getWorld().getName())) : "");
        } else {
            string = string.replace("%world_formatted%", "");
        }

        if (name != null) {
            string = string.replace("%name%", name)
                    .replace("%interact_name%", name)
                    .replace("%interact_type%", "null")
                    .replace("%interact_uuid%", "null");
        }

        if (entity != null) {
            string = string.replace("%interact_name%", plugin.getEntityManager().getEntityName(entity))
                    .replace("%interact_type%", entity.getType().name())
                    .replace("%interact_uuid%", entity.getUniqueId().toString());
        }

        if (plugin.getIntegrationManager().hasMineDown()) {
            string = plugin.getIntegrationManager().getMineDown().parseString(string);
        }

        Pattern pattern = Pattern.compile("&#[a-fA-f0-9]{6}");
        Matcher matcher = pattern.matcher(string);

        while (matcher.find()) {
            String colorHex = string.substring(matcher.start() + 1, matcher.end());
            string = plugin.getVersionManager().hasHexColors()
                    ? string.replace("&" + colorHex, ChatColor.of(colorHex).toString())
                    : string.replace(colorHex, "");
            matcher = pattern.matcher(string);
        }

        if (plugin.getIntegrationManager().hasMiniMessage()) {
            string = string.replace("§", "&");
            string = plugin.getIntegrationManager().getMiniMessage().parseString(string);
        }

        return string.replace("&", "§");
    }

    public static String parseTime(String string, Grave grave) {
        long time = grave.getTimeCreation() - grave.getTimeAlive();
        int day = (int) TimeUnit.SECONDS.toDays(time);
        long hour = TimeUnit.SECONDS.toHours(time) - (day * 24L);
        long minute = TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.SECONDS.toHours(time) * 60);
        long second = TimeUnit.SECONDS.toSeconds(time) - (TimeUnit.SECONDS.toMinutes(time) * 60);

        if (day > 0) {
            string = string.replace("%day%", String.valueOf(day));
        }

        if (hour > 0) {
            string = string.replace("%hour%", String.valueOf(hour));
        }

        if (minute > 0) {
            string = string.replace("%minute%", String.valueOf(minute));
        }

        if (second > 0) {
            string = string.replace("%second%", String.valueOf(second));
        }

        return string;
    }

    public static String getDateString(Grave grave, long time, Graves plugin) {
        if (time > 0) {
            return new SimpleDateFormat(plugin.getConfig("time.date", grave)
                    .getString("time.date", "dd-MM-yyyy")).format(new Date(time));
        }

        return plugin.getConfig("time.infinite", grave).getString("time.infinite");
    }

    public static String getTimeString(Grave grave, long time, Graves plugin) {
        if (time > 0) {
            time = time / 1000;
            int day = (int) TimeUnit.SECONDS.toDays(time);
            long hour = TimeUnit.SECONDS.toHours(time) - (day * 24L);
            long minute = TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.SECONDS.toHours(time) * 60);
            long second = TimeUnit.SECONDS.toSeconds(time) - (TimeUnit.SECONDS.toMinutes(time) * 60);

            String timeDay = "";
            String timeHour = "";
            String timeMinute = "";
            String timeSecond = "";

            if (day > 0) {
                timeDay = plugin.getConfig("time.day", grave).getString("time.day")
                        .replace("%day%", String.valueOf(day));
            }

            if (hour > 0) {
                timeHour = plugin.getConfig("time.hour", grave).getString("time.hour")
                        .replace("%hour%", String.valueOf(hour));
            }

            if (minute > 0) {
                timeMinute = plugin.getConfig("time.minute", grave).getString("time.minute")
                        .replace("%minute%", String.valueOf(minute));
            }

            if (second > 0) {
                timeSecond = plugin.getConfig("time.second", grave).getString("time.second")
                        .replace("%second%", String.valueOf(second));
            }

            return normalizeSpace(timeDay + timeHour + timeMinute + timeSecond);
        }

        return plugin.getConfig("time.infinite", grave).getString("time.infinite");
    }

    private static String normalizeSpace(String string) {
        try {
            string = StringUtils.normalizeSpace(string);
        } catch (NoClassDefFoundError ignored) {
        }

        return string;
    }

    private static String capitalizeFully(String string) {
        try {
            string = WordUtils.capitalizeFully(string);
        } catch (NoClassDefFoundError ignored) {
        }

        return string;
    }
}
