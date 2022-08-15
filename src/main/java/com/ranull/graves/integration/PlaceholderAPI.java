package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.ExperienceUtil;
import com.ranull.graves.util.StringUtil;
import com.ranull.graves.util.UUIDUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        identifier = identifier.toLowerCase();

        if (identifier.equals("author")) {
            return getAuthor();
        } else if (identifier.equals("version")) {
            return getVersion();
        } else if (identifier.equals("count_total")) {
            return String.valueOf(plugin.getCacheManager().getGraveMap().size());
        } else if (identifier.startsWith("owner_name_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("owner_name_", ""));

            if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                String ownerName = plugin.getCacheManager().getGraveMap().get(uuid).getOwnerName();

                if (ownerName != null) {
                    return ownerName;
                }
            }

            return "";
        } else if (identifier.startsWith("owner_type_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("owner_type_", ""));

            if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                EntityType ownerType = plugin.getCacheManager().getGraveMap().get(uuid).getOwnerType();

                if (ownerType != null) {
                    return ownerType.name();
                }
            }

            return "";
        } else if (identifier.startsWith("owner_uuid_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("owner_uuid_", ""));

            if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                UUID ownerUUID = plugin.getCacheManager().getGraveMap().get(uuid).getOwnerUUID();

                if (ownerUUID != null) {
                    return ownerUUID.toString();
                }
            }

            return "";
        } else if (identifier.startsWith("killer_name_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("killer_name_", ""));

            if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                String killerName = plugin.getCacheManager().getGraveMap().get(uuid).getKillerName();

                if (killerName != null) {
                    return killerName;
                }
            }

            return "";
        } else if (identifier.startsWith("killer_type_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("killer_type_", ""));

            if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                EntityType killerType = plugin.getCacheManager().getGraveMap().get(uuid).getKillerType();

                if (killerType != null) {
                    return killerType.name();
                }
            }

            return "";
        } else if (identifier.startsWith("killer_uuid_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("killer_uuid_", ""));

            if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                UUID killerUUID = plugin.getCacheManager().getGraveMap().get(uuid).getKillerUUID();

                if (killerUUID != null) {
                    return killerUUID.toString();
                }
            }

            return "";
        } else if (identifier.startsWith("item_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("item_", ""));

            if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                return String.valueOf(plugin.getCacheManager().getGraveMap().get(uuid).getItemAmount());
            }

            return "";
        } else if (identifier.startsWith("experience_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("experience_", ""));

            if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                return String.valueOf(plugin.getCacheManager().getGraveMap().get(uuid).getExperience());
            }

            return "";
        } else if (identifier.startsWith("level_")) {
            UUID uuid = UUIDUtil.getUUID(identifier.replace("level_", ""));

            if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                return String.valueOf(ExperienceUtil.getLevelFromExperience(plugin.getCacheManager().getGraveMap()
                        .get(uuid).getExperience()));
            }

            return "";
        } else if (identifier.startsWith("time_creation_")) {
            if (identifier.startsWith("time_creation_formatted")) {
                UUID uuid = UUIDUtil.getUUID(identifier
                        .replace("time_creation_formatted", ""));

                if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                    Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);
                    return StringUtil.getDateString(grave, grave.getTimeCreation(), plugin);
                }
            } else {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_creation_", ""));

                if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                    return String.valueOf(plugin.getCacheManager().getGraveMap().get(uuid)
                            .getTimeCreation() / 1000);
                }
            }

            return "";
        } else if (identifier.startsWith("time_alive_remaining_")) {
            if (identifier.startsWith("time_alive_remaining_formatted_")) {
                UUID uuid = UUIDUtil.getUUID(identifier
                        .replace("time_alive_remaining_formatted_", ""));

                if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                    Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);
                    return StringUtil.getTimeString(grave, grave.getTimeAliveRemaining(), plugin);
                }
            } else {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_alive_remaining_", ""));

                if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                    return String.valueOf(plugin.getCacheManager().getGraveMap().get(uuid)
                            .getTimeAliveRemaining() / 1000);
                }
            }

            return "";
        } else if (identifier.startsWith("time_protection_remaining_")) {
            if (identifier.startsWith("time_protection_remaining_formatted_")) {
                UUID uuid = UUIDUtil.getUUID(identifier
                        .replace("time_protection_remaining_formatted_", ""));

                if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                    Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);
                    return StringUtil.getTimeString(grave, grave.getTimeProtectionRemaining(), plugin);
                }
            } else {
                UUID uuid = UUIDUtil.getUUID(identifier
                        .replace("time_protection_remaining_", ""));

                if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                    return String.valueOf(plugin.getCacheManager().getGraveMap().get(uuid)
                            .getTimeProtectionRemaining() / 1000);
                }
            }

            return "";
        } else if (identifier.startsWith("time_lived_")) {
            if (identifier.startsWith("time_lived_formatted_")) {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_lived_formatted_", ""));

                if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                    Grave grave = plugin.getCacheManager().getGraveMap().get(uuid);
                    return StringUtil.getTimeString(grave, grave.getLivedTime(), plugin);
                }
            } else {
                UUID uuid = UUIDUtil.getUUID(identifier.replace("time_lived_", ""));

                if (uuid != null && plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                    return String.valueOf(plugin.getCacheManager().getGraveMap().get(uuid).getLivedTime() / 1000);
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