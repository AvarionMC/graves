package com.ranull.graves.manager;

import org.bukkit.Bukkit;

public final class VersionManager {
    private final String version;
    private final boolean hasConfigContains;
    private final boolean hasAPIVersion;
    private final boolean hasBlockData;
    private final boolean hasPersistentData;
    private final boolean hasScoreboardTags;
    private final boolean hasHexColors;
    private final boolean hasCompassMeta;
    private final boolean hasSwingHand;
    private final boolean hasWorldHeight;
    private final boolean hasSecondHand;
    private final boolean hasEnchantmentCurse;
    private final boolean hasParticle;
    private boolean isBukkit;
    private boolean isMohist;

    public VersionManager() {
        this.version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        this.hasConfigContains = !is_v1_7() && !is_v1_8() && !is_v1_9();
        this.hasAPIVersion = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12();
        this.hasBlockData = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12();
        this.hasPersistentData = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13();
        this.hasScoreboardTags = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10();
        this.hasHexColors = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15() && !isBukkit();
        this.hasCompassMeta = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11()
                && !is_v1_12() && !is_v1_13() && !is_v1_14() && !is_v1_15()
                && !version.matches("(?i)v1_16_R1|");
        this.hasSwingHand = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15();
        this.hasWorldHeight = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15() && !is_v1_16();
        this.hasSecondHand = !is_v1_7() && !is_v1_8();
        this.hasEnchantmentCurse = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10();
        this.hasParticle = !is_v1_7() && !is_v1_8();

        try {
            Class.forName("org.spigotmc.SpigotConfig", false, getClass().getClassLoader());

            this.isBukkit = false;
        } catch (ClassNotFoundException ignored) {
            this.isBukkit = true;
        }

        try {
            Class.forName("com.mohistmc.config.MohistConfigUtil", false, getClass().getClassLoader());

            this.isMohist = true;
        } catch (ClassNotFoundException ignored) {
            this.isBukkit = false;
        }
    }

    public boolean isBukkit() {
        return isBukkit;
    }

    public boolean isMohist() {
        return isMohist;
    }

    public boolean hasConfigContains() {
        return hasConfigContains;
    }

    public boolean hasAPIVersion() {
        return hasAPIVersion;
    }

    public boolean hasBlockData() {
        return hasBlockData;
    }

    public boolean hasPersistentData() {
        return hasPersistentData;
    }

    public boolean hasScoreboardTags() {
        return hasScoreboardTags;
    }

    public boolean hasHexColors() {
        return hasHexColors;
    }

    public boolean hasCompassMeta() {
        return hasCompassMeta;
    }

    public boolean hasSwingHand() {
        return hasSwingHand;
    }

    public boolean hasMinHeight() {
        return hasWorldHeight;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasSecondHand() {
        return hasSecondHand;
    }

    public boolean hasEnchantmentCurse() {
        return hasEnchantmentCurse;
    }

    public boolean hasParticle() {
        return hasParticle;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_7() {
        return version.matches("(?i)v1_7_R1|v1_7_R2|v1_7_R3|v1_7_R4");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_8() {
        return version.matches("(?i)v1_8_R1|v1_8_R2|v1_8_R3");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_9() {
        return version.matches("(?i)v1_9_R1|v1_9_R2");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_10() {
        return version.matches("(?i)v1_10_R1");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_11() {
        return version.matches("(?i)v1_11_R1");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_12() {
        return version.matches("(?i)v1_12_R1");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_13() {
        return version.matches("(?i)v1_13_R1|v1_13_R2");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_14() {
        return version.matches("(?i)v1_14_R1");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_15() {
        return version.matches("(?i)v1_15_R1");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_16() {
        return version.matches("(?i)v1_16_R1|v1_16_R2|v1_16_R3");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_17() {
        return version.matches("(?i)v1_17_R1");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean is_v1_18() {
        return version.matches("(?i)v1_18_R1|v1_18_R2");
    }
}
