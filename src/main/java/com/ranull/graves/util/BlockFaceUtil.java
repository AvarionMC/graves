package com.ranull.graves.util;

import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

public final class BlockFaceUtil {
    public static BlockFace getSimpleBlockFace(BlockFace blockFace) {
        switch (blockFace) {
            case EAST:
            case NORTH_EAST:
                return BlockFace.EAST;
            case SOUTH:
            case SOUTH_EAST:
                return BlockFace.SOUTH;
            case WEST:
            case SOUTH_WEST:
                return BlockFace.WEST;
            default:
                return BlockFace.NORTH;
        }
    }

    public static BlockFace getEntityYawBlockFace(LivingEntity livingEntity) {
        return getYawBlockFace(livingEntity.getLocation().getYaw());
    }

    public static Rotation getBlockFaceRotation(BlockFace blockFace) {
        switch (blockFace) {
            case SOUTH:
                return Rotation.NONE;
            case SOUTH_WEST:
                return Rotation.CLOCKWISE;
            case WEST:
                return Rotation.CLOCKWISE_45;
            case NORTH_WEST:
                return Rotation.CLOCKWISE_135;
            case EAST:
                return Rotation.COUNTER_CLOCKWISE;
            case NORTH_EAST:
                return Rotation.FLIPPED_45;
            case SOUTH_EAST:
                return Rotation.COUNTER_CLOCKWISE_45;
            default:
                return Rotation.FLIPPED;
        }
    }

    public static BlockFace getYawBlockFace(float yaw) {
        float direction = yaw % 360;

        if (direction < 0) {
            direction += 360;
        }

        switch (Math.round(direction / 45)) {
            case 1:
                return BlockFace.SOUTH_WEST;
            case 2:
                return BlockFace.WEST;
            case 3:
                return BlockFace.NORTH_WEST;
            case 4:
                return BlockFace.NORTH;
            case 5:
                return BlockFace.NORTH_EAST;
            case 6:
                return BlockFace.EAST;
            case 7:
                return BlockFace.SOUTH_EAST;
            default:
                return BlockFace.SOUTH;
        }
    }

    public static int getBlockFaceYaw(BlockFace blockFace) {
        switch (blockFace) {
            case SOUTH:
                return 0;
            case SOUTH_WEST:
                return 45;
            case WEST:
                return 90;
            case NORTH_WEST:
                return 135;
            case EAST:
                return -90;
            case NORTH_EAST:
                return -135;
            case SOUTH_EAST:
                return -45;
            default:
                return 180;
        }
    }
}