package org.avarion.graves.util;

import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class BlockFaceUtil {

    @Contract(pure = true)
    public static BlockFace getSimpleBlockFace(@NotNull BlockFace blockFace) {
        return switch (blockFace) {
            case EAST, NORTH_EAST -> BlockFace.EAST;
            case SOUTH, SOUTH_EAST -> BlockFace.SOUTH;
            case WEST, SOUTH_WEST -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    public static BlockFace getEntityYawBlockFace(@NotNull LivingEntity livingEntity) {
        return getYawBlockFace(livingEntity.getLocation().getYaw());
    }

    @Contract(pure = true)
    public static Rotation getBlockFaceRotation(@NotNull BlockFace blockFace) {
        return switch (blockFace) {
            case SOUTH -> Rotation.NONE;
            case SOUTH_WEST -> Rotation.CLOCKWISE;
            case WEST -> Rotation.CLOCKWISE_45;
            case NORTH_WEST -> Rotation.CLOCKWISE_135;
            case EAST -> Rotation.COUNTER_CLOCKWISE;
            case NORTH_EAST -> Rotation.FLIPPED_45;
            case SOUTH_EAST -> Rotation.COUNTER_CLOCKWISE_45;
            default -> Rotation.FLIPPED;
        };
    }

    public static BlockFace getYawBlockFace(float yaw) {
        float direction = yaw % 360;

        if (direction < 0) {
            direction += 360;
        }

        return switch (Math.round(direction / 45)) {
            case 1 -> BlockFace.SOUTH_WEST;
            case 2 -> BlockFace.WEST;
            case 3 -> BlockFace.NORTH_WEST;
            case 4 -> BlockFace.NORTH;
            case 5 -> BlockFace.NORTH_EAST;
            case 6 -> BlockFace.EAST;
            case 7 -> BlockFace.SOUTH_EAST;
            default -> BlockFace.SOUTH;
        };
    }

    @Contract(pure = true)
    public static int getBlockFaceYaw(@NotNull BlockFace blockFace) {
        return switch (blockFace) {
            case SOUTH -> 0;
            case SOUTH_WEST -> 45;
            case WEST -> 90;
            case NORTH_WEST -> 135;
            case EAST -> -90;
            case NORTH_EAST -> -135;
            case SOUTH_EAST -> -45;
            default -> 180;
        };
    }

}
