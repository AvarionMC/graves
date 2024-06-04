package org.avarion.graves.integration;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ProtocolLib {

    private final ProtocolManager protocolManager;

    public ProtocolLib() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void setBlock(Block block, Material material, Player player) {
        WrappedBlockData wrappedBlockData = WrappedBlockData.createData(material);

        sendServerPacket(player, createBlockChangePacket(block, wrappedBlockData));
    }

    public void refreshBlock(Block block, Player player) {
        sendServerPacket(player, createBlockChangePacket(block, WrappedBlockData.createData(block.getBlockData())));
    }

    private @NotNull PacketContainer createBlockChangePacket(@NotNull Block block, WrappedBlockData wrappedBlockData) {
        Location location = block.getLocation();
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        PacketContainer packetContainer = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);

        packetContainer.getBlockPositionModifier().write(0, blockPosition);
        packetContainer.getBlockData().write(0, wrappedBlockData);

        return packetContainer;
    }

    private void sendServerPacket(Player player, PacketContainer packetContainer) {
        protocolManager.sendServerPacket(player, packetContainer);
    }

}
