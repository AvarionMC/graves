package org.avarion.graves.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.avarion.graves.Graves;
import org.avarion.graves.manager.CacheManager;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.ExperienceUtil;
import org.avarion.graves.util.StringUtil;
import org.avarion.graves.util.UUIDUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PlaceholderAPI extends PlaceholderExpansion implements Relational {

    private final Graves plugin;

    public PlaceholderAPI(Graves plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @NotNull
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "graves";
    }

    @NotNull
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        identifier = identifier.toLowerCase();

        if (identifier.equals("author")) {
            return getAuthor();
        }
        else if (identifier.equals("version")) {
            return getVersion();
        }
        else if (identifier.equals("count_total")) {
            return String.valueOf(CacheManager.graveMap.size());
        }
        else if (identifier.startsWith("owner_name_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("owner_name_", ""));

            if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                String ownerName = CacheManager.graveMap.get(uuid).getOwnerName();

                if (ownerName != null) {
                    return ownerName;
                }
            }

            return "";
        }
        else if (identifier.startsWith("owner_type_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("owner_type_", ""));

            if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                EntityType ownerType = CacheManager.graveMap.get(uuid).getOwnerType();

                if (ownerType != null) {
                    return ownerType.name();
                }
            }

            return "";
        }
        else if (identifier.startsWith("owner_uuid_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("owner_uuid_", ""));

            if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                UUID ownerUUID = CacheManager.graveMap.get(uuid).getOwnerUUID();

                if (ownerUUID != null) {
                    return ownerUUID.toString();
                }
            }

            return "";
        }
        else if (identifier.startsWith("killer_name_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("killer_name_", ""));

            if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                String killerName = CacheManager.graveMap.get(uuid).getKillerName();

                if (killerName != null) {
                    return killerName;
                }
            }

            return "";
        }
        else if (identifier.startsWith("killer_type_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("killer_type_", ""));

            if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                EntityType killerType = CacheManager.graveMap.get(uuid).getKillerType();

                if (killerType != null) {
                    return killerType.name();
                }
            }

            return "";
        }
        else if (identifier.startsWith("killer_uuid_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("killer_uuid_", ""));

            if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                UUID killerUUID = CacheManager.graveMap.get(uuid).getKillerUUID();

                if (killerUUID != null) {
                    return killerUUID.toString();
                }
            }

            return "";
        }
        else if (identifier.startsWith("item_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("item_", ""));

            if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                return String.valueOf(CacheManager.graveMap.get(uuid).getItemAmount());
            }

            return "";
        }
        else if (identifier.startsWith("experience_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("experience_", ""));

            if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                return String.valueOf(CacheManager.graveMap.get(uuid).getExperience());
            }

            return "";
        }
        else if (identifier.startsWith("level_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("level_", ""));

            if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                return String.valueOf(ExperienceUtil.getLevelFromExperience(CacheManager.graveMap.get(uuid)
                                                                                                 .getExperience()));
            }

            return "";
        }
        else if (identifier.startsWith("time_creation_")) {
            if (identifier.startsWith("time_creation_formatted")) {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_creation_formatted", ""));

                if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                    Grave grave = CacheManager.graveMap.get(uuid);
                    return StringUtil.getDateString(grave, grave.getTimeCreation(), plugin);
                }
            }
            else {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_creation_", ""));

                if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                    return String.valueOf(CacheManager.graveMap.get(uuid).getTimeCreation() / 1000);
                }
            }

            return "";
        }
        else if (identifier.startsWith("time_alive_remaining_")) {
            if (identifier.startsWith("time_alive_remaining_formatted_")) {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_alive_remaining_formatted_", ""));

                if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                    Grave grave = CacheManager.graveMap.get(uuid);
                    return StringUtil.getTimeString(grave, grave.getTimeAliveRemaining(), plugin);
                }
            }
            else {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_alive_remaining_", ""));

                if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                    return String.valueOf(CacheManager.graveMap.get(uuid).getTimeAliveRemaining() / 1000);
                }
            }

            return "";
        }
        else if (identifier.startsWith("time_protection_remaining_")) {
            if (identifier.startsWith("time_protection_remaining_formatted_")) {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_protection_remaining_formatted_", ""));

                if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                    Grave grave = CacheManager.graveMap.get(uuid);
                    return StringUtil.getTimeString(grave, grave.getTimeProtectionRemaining(), plugin);
                }
            }
            else {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_protection_remaining_", ""));

                if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                    return String.valueOf(CacheManager.graveMap.get(uuid).getTimeProtectionRemaining() / 1000);
                }
            }

            return "";
        }
        else if (identifier.startsWith("time_lived_")) {
            if (identifier.startsWith("time_lived_formatted_")) {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_lived_formatted_", ""));

                if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                    Grave grave = CacheManager.graveMap.get(uuid);
                    return StringUtil.getTimeString(grave, grave.getLivedTime(), plugin);
                }
            }
            else {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_lived_", ""));

                if (uuid != null && CacheManager.graveMap.containsKey(uuid)) {
                    return String.valueOf(CacheManager.graveMap.get(uuid).getLivedTime() / 1000);
                }
            }

            return "";
        }

        if (player != null) {
            if (identifier.equals("count")) {
                return String.valueOf(plugin.getGraveManager().getGraveCount(player));
            }
        }

        return null;
    }

    @Override
    public String onPlaceholderRequest(Player playerOne, Player playerTwo, String identifier) {
        return onPlaceholderRequest(playerOne, identifier); // TODO
    }

}
