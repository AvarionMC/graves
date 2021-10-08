package com.ranull.graves.util;

import org.bukkit.entity.Player;

public final class ExperienceUtil {
    public static int getPlayerExperience(Player player) {
        int experience = Math.round(getExperienceAtLevel(player.getLevel()) * player.getExp());
        int level = player.getLevel();

        while (level > 0) {
            level--;
            experience += getExperienceAtLevel(level);
        }

        if (experience < 0) {
            return -1;
        }

        return experience;
    }

    public static int getExperienceAtLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        }

        return 9 * level - 158;
    }

    public static long getLevelFromExperience(long experience) {
        double result = 0;

        if (experience > 1395) {
            result = (Math.sqrt(72 * experience - 54215) + 325) / 18;
        } else if (experience > 315) {
            result = Math.sqrt(40 * experience - 7839) / 10 + 8.1;
        } else if (experience > 0) {
            result = Math.sqrt(experience + 9) - 3;
        }

        return (long) (Math.round(result * 100.0) / 100.0);
    }

    public static int getDropPercent(int experience, float percent) {
        return experience > 0 ? (int) (experience * percent) : 0;
    }

    public static int getPlayerDropExperience(Player player, float expStorePercent) {
        int experience = getPlayerExperience(player);

        return experience > 0 ? (int) (experience * expStorePercent) : 0;
    }
}
