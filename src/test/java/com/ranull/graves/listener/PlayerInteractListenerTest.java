package com.ranull.graves.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.ranull.graves.Graves;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class PlayerInteractListenerTest {
    private static ServerMock server;
    private static Graves plugin;

    @BeforeAll
    public static void load() {
        System.out.println("load");
        server = MockBukkit.mock();
        plugin = new Graves();
        plugin = MockBukkit.load(Graves.class);
    }

    @AfterAll
    public static void unload()
    {
        System.out.println("unload");
        MockBukkit.unmock();
    }

    @Test
    public void testSomething() {

    }
//
//    @BeforeEach
//    public void beforeEach() {
//        System.out.println("beforeEach");
//        server.getPluginManager().clearEvents();
//    }
//
//    @Test
//    @DisplayName("When auto-looting a grave with a full inventory, don't overwrite the held item.")
//    public void testPlayerInteract() {
//        System.out.println("testPlayerInteract");
//        Player player = new PlayerMock(server, "SomePlayer");
//        World world = server.addSimpleWorld("my_world");
//        Block block = new BlockMock(Material.GREEN_TERRACOTTA, new Location(world, TestUtilities.randomInt(), 100, TestUtilities.randomInt()));
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
//    }
}
