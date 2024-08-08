package org.avarion.graves.integration;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.PasteBuilder;
import org.avarion.graves.Graves;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.BlockFaceUtil;
import org.avarion.graves.util.ResourceUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class WorldEdit {
    private final Graves plugin;
    private final Plugin worldEditPlugin;
    private final com.sk89q.worldedit.WorldEdit libInstance;
    private final Map<String, Clipboard> stringClipboardMap;

    public WorldEdit(Graves plugin, Plugin worldEditPlugin) {
        this.plugin = plugin;
        this.worldEditPlugin = worldEditPlugin;
        this.libInstance = com.sk89q.worldedit.WorldEdit.getInstance();
        this.stringClipboardMap = new HashMap<>();

        saveData();
        loadData();
    }

    public void saveData() {
        if (plugin.getConfig().getBoolean("settings.integration.worldedit.write")) {
            ResourceUtil.copyResources("data/plugin/"
                                       + worldEditPlugin.getName().toLowerCase()
                                       + "/schematics", plugin.getDataFolder() + "/schematics", plugin);
            plugin.debugMessage("Saving " + worldEditPlugin.getName() + " schematics.", 1);
        }
    }

    public void loadData() {
        stringClipboardMap.clear();

        File schematicsFile = new File(plugin.getDataFolder() + File.separator + "schematics");
        File[] listFiles = schematicsFile.listFiles();

        if (listFiles == null) {
            return;
        }

        for (File file : listFiles) {
            if (!file.isFile() || !file.getName().contains(".schem")) {
                continue;
            }

            String name = file.getName().toLowerCase().replace(".schematic", "").replace(".schem", "");
            ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(file);

            if (clipboardFormat == null) {
                plugin.warningMessage("Unable to load schematic " + name);
                continue;
            }

            try (ClipboardReader clipboardReader = clipboardFormat.getReader(new FileInputStream(file))) {
                stringClipboardMap.put(name, clipboardReader.read());
                plugin.debugMessage("Loading schematic " + name, 1);
            }
            catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void createSchematic(@NotNull Location location, Grave grave) {
        if (location.getWorld() != null && plugin.getConfigBool("schematic.enabled", grave)) {
            String schematicName = plugin.getConfigString("schematic.name", grave);

            if (schematicName != null && !schematicName.isEmpty() && hasSchematic(schematicName)) {
                int offsetX = plugin.getConfigInt("schematic.offset.x", grave);
                int offsetY = plugin.getConfigInt("schematic.offset.y", grave);
                int offsetZ = plugin.getConfigInt("schematic.offset.z", grave);


                pasteSchematic(location.clone().add(offsetX, offsetY, offsetZ), location.getYaw(), schematicName);
                //PasteBuilder test = getSchematic(location.clone().add(offsetX, offsetY, offsetZ), grave.getYaw(), schematicName);
                //buildSchematic(test);
                plugin.debugMessage("Placing schematic for "
                                    + grave.getUUID()
                                    + " at "
                                    + location.getWorld().getName()
                                    + ", "
                                    + (location.getBlockX() + 0.5)
                                    + "x, "
                                    + (location.getBlockY() + 0.5)
                                    + "y, "
                                    + (location.getBlockZ() + 0.5)
                                    + "z", 1);
            }
            else {
                plugin.debugMessage("Can't find schematic " + schematicName, 1);
            }
        }
    }

    @SuppressWarnings("SameReturnValue")
    public boolean canBuildSchematic(@NotNull Location location, BlockFace blockFace, String name) {
        if (location.getWorld() != null) {
            if (stringClipboardMap.containsKey(name)) {
                Clipboard clipboard = stringClipboardMap.get(name);
                BlockVector3 offset = clipboard.getOrigin();
                Region region = clipboard.getRegion();
                int width = region.getWidth();
                int height = region.getHeight();
                int length = region.getLength();
                Location corner = location.clone().add(offset.x(), offset.y(), offset.z());

                Location leftTopCorner = location;
                Location rightTopCorner = location;

                switch (blockFace) {
                    case NORTH -> leftTopCorner = location.clone()
                                                          .add(offset.x() - region.getWidth(), offset.y()
                                                                                               - region.getHeight(), 0);
                    case WEST -> leftTopCorner = location.clone()
                                                         .add(region.getWidth() - offset.x(), offset.y()
                                                                                              - region.getHeight(), 0);
                }

                corner.getBlock().setType(Material.BEDROCK);
                //plugin.getServer().broadcastMessage(leftTopCorner.toString());
                //leftTopCorner.getBlock().setType(Material.BEDROCK);
                //plugin.getServer().broadcastMessage(region.toString());
                //plugin.getServer().broadcastMessage(location.toString());
                //center.getBlock().setType(Material.BEDROCK);
            }
            else {
                plugin.debugMessage("Can't find schematic " + name, 1);
            }
        }

        return false;
    }

    public boolean hasSchematic(@NotNull String string) {
        return stringClipboardMap.containsKey(string.toLowerCase().replace(".schem", ""));
    }

    public void getAreaSchematic(Location location, float yaw, File file) {
        // Not implementing now
    }

    public Clipboard pasteSchematic(Location location, String name) {
        return pasteSchematic(location, 0, name);
    }

    public Clipboard pasteSchematic(Location location, float yaw, String name) {
        return pasteSchematic(location, yaw, name, true);
    }

    private @Nullable Clipboard pasteSchematic(@NotNull Location location, float yaw, String name, @SuppressWarnings("SameParameterValue") boolean ignoreAirBlocks) {
        if (location.getWorld() != null) {
            if (stringClipboardMap.containsKey(name)) {
                Clipboard clipboard = stringClipboardMap.get(name);

                try (EditSession editSession = getEditSession(location.getWorld())) {
                    ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);

                    clipboardHolder.setTransform(clipboardHolder.getTransform().combine(getYawTransform(yaw)));
                    Operations.complete(clipboardHolder.createPaste(editSession)
                                                       .to(locationToBlockVector3(location))
                                                       .ignoreAirBlocks(ignoreAirBlocks)
                                                       .build());

                    return clipboardHolder.getClipboard();
                }
                catch (WorldEditException exception) {
                    exception.printStackTrace();
                }
            }
            else {
                plugin.debugMessage("Can't find schematic " + name, 1);
            }
        }

        return null;
    }

    private PasteBuilder getSchematic(Location location, float yaw, String name) {
        return getSchematic(location, yaw, name, true);
    }

    private @Nullable PasteBuilder getSchematic(@NotNull Location location, float yaw, String name, @SuppressWarnings("SameParameterValue") boolean ignoreAirBlocks) {
        if (location.getWorld() != null) {
            if (stringClipboardMap.containsKey(name)) {
                Clipboard clipboard = stringClipboardMap.get(name);
                ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);

                clipboardHolder.setTransform(clipboardHolder.getTransform().combine(getYawTransform(yaw)));
                return clipboardHolder.createPaste(getEditSession(location.getWorld()))
                                      .to(locationToBlockVector3(location))
                                      .ignoreAirBlocks(ignoreAirBlocks);
            }
            else {
                plugin.debugMessage("Can't find schematic " + name, 1);
            }
        }

        return null;
    }

    public void buildSchematic(@NotNull PasteBuilder pasteBuilder) {
        try {
            Operations.complete(pasteBuilder.build());
        }
        catch (WorldEditException exception) {
            exception.printStackTrace();
        }
    }

    private @NotNull AffineTransform getYawTransform(float yaw) {
        AffineTransform affineTransform = new AffineTransform();

        return switch (BlockFaceUtil.getSimpleBlockFace(BlockFaceUtil.getYawBlockFace(yaw))) {
            case SOUTH -> affineTransform.rotateY(180);
            case EAST -> affineTransform.rotateY(270);
            case WEST -> affineTransform.rotateY(90);
            default -> affineTransform;
        };

    }

    public BlockVector3 locationToBlockVector3(@NotNull Location location) {
        return BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Contract("_, _ -> new")
    private @NotNull Location blockVector3ToLocation(World world, @NotNull BlockVector3 blockVector3) {
        return new Location(world, blockVector3.x(), blockVector3.y(), blockVector3.z());
    }

    @Contract("_ -> new")
    private @NotNull EditSession getEditSession(World world) {
        return libInstance.newEditSession(BukkitAdapter.adapt(world));
    }
}
