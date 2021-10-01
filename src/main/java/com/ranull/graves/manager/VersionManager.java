package com.ranull.graves.manager;

import com.ranull.graves.Graves;

public final class VersionManager {
    private final String version;
    private final boolean hasBlockData;
    private final boolean hasPersistentData;
    private final boolean hasHexColors;
    private final boolean hasLodestone;
    private final boolean hasSwingHand;
    private final boolean hasWorldHeight;
    private final boolean hasSecondHand;
    private boolean isBukkit;

    public VersionManager(Graves plugin) {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
        } catch (ClassNotFoundException ignored) {
            isBukkit = true;
        }

        this.version = plugin.getServer().getClass().getPackage().getName().split("\\.")[3];
        this.hasBlockData = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12();
        this.hasPersistentData = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13();
        this.hasHexColors = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15() && !isBukkit();
        this.hasLodestone = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11()
                && !is_v1_12() && !is_v1_13() && !is_v1_14() && !is_v1_15()
                && !version.matches("(?i)v1_16_R1|");
        this.hasSwingHand = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15();
        this.hasWorldHeight = !is_v1_7() && !is_v1_8() && !is_v1_9() && !is_v1_10() && !is_v1_11() && !is_v1_12()
                && !is_v1_13() && !is_v1_14() && !is_v1_15() && !is_v1_16();
        this.hasSecondHand = !is_v1_7() && !is_v1_8();
    }

    public boolean isBukkit() {
        return isBukkit;
    }

    public boolean hasBlockData() {
        return hasBlockData;
    }

    public boolean hasPersistentData() {
        return hasPersistentData;
    }

    public boolean hasHexColors() {
        return hasHexColors;
    }

    public boolean hasLodestone() {
        return hasLodestone;
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
}
