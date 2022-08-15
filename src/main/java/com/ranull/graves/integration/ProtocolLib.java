package com.ranull.graves.integration;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.ranull.graves.Graves;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public final class ProtocolLib {
    private final Graves plugin;
    private final ProtocolManager protocolManager;

    public ProtocolLib(Graves plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void setBlock(Block block, Material material, Player player) {
        WrappedBlockData wrappedBlockData = WrappedBlockData.createData(material);

        sendServerPacket(player, createBlockChangePacket(block, wrappedBlockData));
    }

    public void refreshBlock(Block block, Player player) {
        sendServerPacket(player, createBlockChangePacket(block, WrappedBlockData.createData(block.getBlockData())));
    }

    private PacketContainer createBlockChangePacket(Block block, WrappedBlockData wrappedBlockData) {
        Location location = block.getLocation();
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        PacketContainer packetContainer = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);

        packetContainer.getBlockPositionModifier().write(0, blockPosition);
        packetContainer.getBlockData().write(0, wrappedBlockData);

        return packetContainer;
    }

    private void sendServerPacket(Player player, PacketContainer packetContainer) {
        try {
            protocolManager.sendServerPacket(player, packetContainer);
        } catch (InvocationTargetException ignored) {
        }
    }
}
