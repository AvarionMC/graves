package com.ranull.graves.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.ranull.graves.Graves;
import com.ranull.graves.manager.BukkitVersion;
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

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();

        Whitebox.setInternalState(Graves.class, "IS_TEST", true);

        try (MockedStatic<BukkitVersion> mockedVersion = mockStatic(BukkitVersion.class)) {
            mockedVersion.when(BukkitVersion::getVersion).thenReturn("v_1_20_R3");
            plugin = MockBukkit.load(Graves.class);
        }
    }

    @AfterAll
    public static void unload()
    {
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
