package com.ranull.graves.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.ranull.graves.Graves;
import com.ranull.graves.manager.BukkitVersion;
import com.ranull.graves.util.TestUtilities;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.internal.matchers.Any;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest(Graves.class)
class PlayerInteractListenerTest {
    private static ServerMock server;
    private static Graves plugin;

    @NotNull
    private Player createPlayer(@NotNull String name) {
        Player player = server.addPlayer("ExamplePlayer");
        player.giveExp(1000);
        PlayerInventory inventory = player.getInventory();

        // Equip armor
        inventory.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        inventory.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        inventory.setItemInOffHand(new ItemStack(Material.DIAMOND_AXE));
        // First hotbar slot
        inventory.setItem(0, new ItemStack(Material.DIAMOND_SWORD));
        // Fill the inventory with beef
        for (int i = 1; i < inventory.getSize() - 5; i++) { // -5 because of the helmet & stuff...
            inventory.setItem(i, new ItemStack(Material.COOKED_BEEF, i + 1));
        }

        World world = server.getWorld("world");

//        player.setRespawnLocation(new Location(world, 1, 2, 3));
        player.setLastDeathLocation(new Location(world, 4, 5, 6));
        player.teleport(new Location(world, 7, 8, 9));

        // Add default permissions (PR hanging that would load them from the plugin.yml file: https://github.com/MockBukkit/MockBukkit/pull/961)
        player.addAttachment(plugin, "graves.place", true);
        player.addAttachment(plugin, "graves.open", true);
        player.addAttachment(plugin, "graves.autoloot", true);
        player.addAttachment(plugin, "graves.break", true);
        player.addAttachment(plugin, "graves.gui", true);
        player.addAttachment(plugin, "graves.experience", true);
        player.addAttachment(plugin, "graves.protection", true);
        player.addAttachment(plugin, "graves.teleport", true);
        player.addAttachment(plugin, "graves.graveyard", true);

        return player;
    }

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");

        Whitebox.setInternalState(Graves.class, "IS_TEST", true);

        try (MockedStatic<BukkitVersion> mockedVersion = mockStatic(BukkitVersion.class)) {
            mockedVersion.when(BukkitVersion::getVersion).thenReturn("v_1_20_R3");
            plugin = MockBukkit.load(Graves.class);
        }

        server.getScheduler().performOneTick();
    }

    @AfterAll
    public static void unload()
    {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @Test
    @DisplayName("When auto-looting a grave with a full inventory, don't overwrite the held item.")
    public void testPlayerInteract() {

        Player player = createPlayer("Bumba");
        player.damage(1000000);  // Should be dead, right?

//
//        // Create the event with mock parameters
//        PlayerInteractEvent event = new PlayerInteractEvent(
//                player,
//                Action.RIGHT_CLICK_BLOCK,
//                null,
//                block,
//                BlockFace.WEST,
//                EquipmentSlot.HAND
//        );
//
//        server.getPluginManager().callEvent(event);
    }
}
