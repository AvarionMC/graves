package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class SchematicManager {
    private final Graves plugin;
    private final Map<String, Clipboard> stringClipboardMap;

    public SchematicManager(Graves plugin) {
        this.plugin = plugin;
        this.stringClipboardMap = new HashMap<>();
        File schematicsFile = new File(plugin.getDataFolder() + File.separator + "schematics");
        String defaultSchematic = "grave_default";
        String defaultSchematicPath = schematicsFile + File.separator + defaultSchematic + ".schem";

        if ((!schematicsFile.exists() && schematicsFile.mkdirs())
                || (schematicsFile.exists() && !(new File(defaultSchematicPath).exists()))) {
            plugin.saveResource("schematics" + File.separator + defaultSchematic + ".schem", false);
            plugin.saveResource("schematics" + File.separator + "grave_wither.schem", false);
            plugin.infoMessage("Saved schematic " + defaultSchematic);
        }

        reload();
    }

    public void reload() {
        stringClipboardMap.clear();

        File schematicsFile = new File(plugin.getDataFolder() + File.separator + "schematics");
        File[] listFiles = schematicsFile.listFiles();

        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isFile() && file.getName().contains(".schem")) {
                    String name = file.getName().toLowerCase().replace(".schematic", "")
                            .replace(".schem", "");
                    ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(file);

                    if (clipboardFormat != null) {
                        try (ClipboardReader clipboardReader = clipboardFormat.getReader(new FileInputStream(file))) {
                            stringClipboardMap.put(name, clipboardReader.read());
                            plugin.debugMessage("Loading schematic " + name);
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    } else {
                        plugin.warningMessage("Unable to load schematic " + name);
                    }
                }
            }
        }
    }

    public void createSchematic(Location location, Grave grave) {
        if (location.getWorld() != null && plugin.getConfig("schematic.enabled", grave).getBoolean("schematic.enabled")) {
            String schematicName = plugin.getConfig("schematic.name", grave).getString("schematic.name");

            if (schematicName != null && !schematicName.equals("")
                    && plugin.getSchematicManager().hasSchematic(schematicName)) {
                int offsetX = plugin.getConfig("schematic.offset.x", grave).getInt("schematic.offset.x");
                int offsetY = plugin.getConfig("schematic.offset.y", grave).getInt("schematic.offset.y");
                int offsetZ = plugin.getConfig("schematic.offset.z", grave).getInt("schematic.offset.z");

                pasteSchematic(location.clone().add(offsetX, offsetY, offsetZ), grave.getYaw(), schematicName);
                plugin.debugMessage("Placing schematic for " + grave.getUUID() + " at "
                        + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                        + (location.getBlockY() + 0.5) + "Y, " + (location.getBlockZ() + 0.5) + "z");

            } else {
                plugin.debugMessage("Can't find schematic " + schematicName);
            }
        }
    }

    public boolean canBuildSchematic(Location location, BlockFace blockFace, String name) {
        if (location.getWorld() != null) {
            if (stringClipboardMap.containsKey(name)) {
                Clipboard clipboard = stringClipboardMap.get(name);
                BlockVector3 offset = clipboard.getOrigin();
                ;
                Region region = clipboard.getRegion();
                int width = region.getWidth();
                int height = region.getHeight();
                int length = region.getLength();
                //Location center = new Location(location.getWorld(), region.getWidth() + offset.getBlockX(), region.getHeight() + offset.getBlockY(), region.getLength() + offset.getBlockZ());
                Location corner = location.clone().add(offset.getBlockX(), offset.getBlockY(), offset.getBlockZ());

                Location leftTopCorner = location;
                Location rightTopCorner = location;

                switch (blockFace) {
                    case NORTH:
                        leftTopCorner = location.clone().add(offset.getBlockX() - region.getWidth(), offset.getBlockY() - region.getHeight(), 0);
                    case WEST:
                        leftTopCorner = location.clone().add(region.getWidth() - offset.getBlockX(), offset.getBlockY() - region.getHeight(), 0);
                }

                corner.getBlock().setType(Material.BEDROCK);
                //plugin.getServer().broadcastMessage(leftTopCorner.toString());
                //leftTopCorner.getBlock().setType(Material.BEDROCK);
                //plugin.getServer().broadcastMessage(region.toString());
                //plugin.getServer().broadcastMessage(location.toString());
                //center.getBlock().setType(Material.BEDROCK);
            } else {
                plugin.debugMessage("Can't find schematic " + name);
            }
        }

        return false;
    }

    public boolean hasSchematic(String string) {
        return stringClipboardMap.containsKey(string.toLowerCase().replace(".schem", ""));
    }

    public void getAreaSchematic(Location location, float yaw, File file) {

    }

    public Clipboard pasteSchematic(Location location, String name) {
        return pasteSchematic(location, 0, name);
    }

    public Clipboard pasteSchematic(Location location, float yaw, String name) {
        return pasteSchematic(location, yaw, name, true);
    }

    private Clipboard pasteSchematic(Location location, float yaw, String name, boolean ignoreAirBlocks) {
        if (location.getWorld() != null) {
            if (stringClipboardMap.containsKey(name)) {
                Clipboard clipboard = stringClipboardMap.get(name);

                try (EditSession editSession = getEditSession(location.getWorld())) {
                    ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);

                    clipboardHolder.setTransform(clipboardHolder.getTransform().combine(getYawTransform(yaw)));
                    Operations.complete(clipboardHolder.createPaste(editSession).to(locationToBlockVector3(location))
                            .ignoreAirBlocks(ignoreAirBlocks).build());

                    return clipboardHolder.getClipboard();
                } catch (WorldEditException exception) {
                    exception.printStackTrace();
                }
            } else {
                plugin.debugMessage("Can't find schematic " + name);
            }
        }

        return null;
    }

    private AffineTransform getYawTransform(float yaw) {
        AffineTransform affineTransform = new AffineTransform();

        switch (BlockFaceUtil.getSimpleBlockFace(BlockFaceUtil.getYawBlockFace(yaw))) {
            case SOUTH:
                return affineTransform.rotateY(180);
            case EAST:
                return affineTransform.rotateY(270);
            case WEST:
                return affineTransform.rotateY(90);
        }

        return affineTransform;
    }

    private BlockVector3 locationToBlockVector3(Location location) {
        return BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private Location blockVector3ToLocation(World world, BlockVector3 blockVector3) {
        return new Location(world, blockVector3.getBlockX(), blockVector3.getBlockY(), blockVector3.getBlockZ());
    }

    private EditSession getEditSession(World world) {
        return WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world));
    }
}
