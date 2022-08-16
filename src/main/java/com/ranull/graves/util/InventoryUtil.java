package com.ranull.graves.util;

import com.ranull.graves.Graves;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class InventoryUtil {
    public static int getInventorySize(int size) {
        if (size <= 9) {
            return 9;
        } else if (size <= 18) {
            return 18;
        } else if (size <= 27) {
            return 27;
        } else if (size <= 36) {
            return 36;
        } else if (size <= 45) {
            return 45;
        } else {
            return 54;
        }
    }

    public static void equipArmor(Inventory inventory, Player player) {
        List<ItemStack> itemList = Arrays.asList(inventory.getContents());
        Collections.reverse(itemList);

        for (ItemStack itemStack : itemList) {
            if (itemStack != null) {
                if (player.getInventory().getHelmet() == null && isHelmet(itemStack)) {
                    player.getInventory().setHelmet(itemStack);
                    playArmorEquipSound(player, itemStack);
                    inventory.removeItem(itemStack);
                }

                if (player.getInventory().getChestplate() == null && isChestplate(itemStack)) {
                    player.getInventory().setChestplate(itemStack);
                    playArmorEquipSound(player, itemStack);
                    inventory.removeItem(itemStack);
                }

                if (player.getInventory().getLeggings() == null && isLeggings(itemStack)) {
                    player.getInventory().setLeggings(itemStack);
                    playArmorEquipSound(player, itemStack);
                    inventory.removeItem(itemStack);
                }

                if (player.getInventory().getBoots() == null && isBoots(itemStack)) {
                    player.getInventory().setBoots(itemStack);
                    playArmorEquipSound(player, itemStack);
                    inventory.removeItem(itemStack);
                }
            }
        }
    }

    public static void equipItems(Inventory inventory, Player player) {
        List<ItemStack> itemStackList = new ArrayList<>();

        for (ItemStack itemStack : inventory.getContents().clone()) {
            if (itemStack != null && !MaterialUtil.isAir(itemStack.getType())) {
                itemStackList.add(itemStack);
            }
        }

        inventory.clear();

        for (ItemStack itemStack : itemStackList) {
            for (Map.Entry<Integer, ItemStack> itemStackEntry : player.getInventory().addItem(itemStack).entrySet()) {
                inventory.addItem(itemStackEntry.getValue())
                        .forEach((key, value) -> player.getWorld().dropItem(player.getLocation(), value));
            }
        }
    }

    public static void playArmorEquipSound(Player player, ItemStack itemStack) {
        try {
            if (itemStack.getType().name().startsWith("NETHERITE")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1, 1);
            } else if (itemStack.getType().name().startsWith("DIAMOND")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1, 1);
            } else if (itemStack.getType().name().startsWith("GOLD")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1, 1);
            } else if (itemStack.getType().name().startsWith("IRON")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1, 1);
            } else if (itemStack.getType().name().startsWith("LEATHER")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            } else if (itemStack.getType().name().startsWith("LEATHER")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            } else if (itemStack.getType().name().startsWith("ELYTRA")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1, 1);
            } else if (itemStack.getType().name().startsWith("TURTLE")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_TURTLE, 1, 1);
            } else {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1, 1);
            }
        } catch (NoSuchFieldError ignored) {
        }
    }

    public static boolean isArmor(ItemStack itemStack) {
        return isHelmet(itemStack) || isChestplate(itemStack) || isLeggings(itemStack) || isBoots(itemStack);
    }

    public static boolean isHelmet(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name()
                .matches("(?i)NETHERITE_HELMET|DIAMOND_HELMET|GOLDEN_HELMET|GOLD_HELMET|IRON_HELMET|LEATHER_HELMET|" +
                        "CHAINMAIL_HELMET|TURTLE_HELMET|CARVED_PUMPKIN|PUMPKIN");
    }

    public static boolean isChestplate(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name()
                .matches("(?i)NETHERITE_CHESTPLATE|DIAMOND_CHESTPLATE|GOLDEN_CHESTPLATE|GOLD_CHESTPLATE|" +
                        "IRON_CHESTPLATE|LEATHER_CHESTPLATE|CHAINMAIL_CHESTPLATE|ELYTRA");
    }

    public static boolean isLeggings(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name()
                .matches("(?i)NETHERITE_LEGGINGS|DIAMOND_LEGGINGS|GOLDEN_LEGGINGS|GOLD_LEGGINGS|IRON_LEGGINGS|" +
                        "LEATHER_LEGGINGS|CHAINMAIL_LEGGINGS");
    }

    public static boolean isBoots(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name()
                .matches("(?i)NETHERITE_BOOTS|DIAMOND_BOOTS|GOLDEN_BOOTS|GOLD_BOOTS|IRON_BOOTS|LEATHER_BOOTS|" +
                        "CHAINMAIL_BOOTS");
    }

    public static String inventoryToString(Inventory inventory) {
        List<String> stringList = new ArrayList<>();

        for (ItemStack itemStack : inventory.getContents()) {
            String base64 = Base64Util.objectToBase64(itemStack != null ? itemStack : new ItemStack(Material.AIR));

            if (base64 != null) {
                stringList.add(base64);
            }
        }

        return String.join("|", stringList);
    }

    public static Inventory stringToInventory(InventoryHolder inventoryHolder, String string, String title, Graves plugin) {
        String[] strings = string.split("\\|");

        if (strings.length > 0 && !strings[0].equals("")) {
            Inventory inventory = plugin.getServer().createInventory(inventoryHolder,
                    InventoryUtil.getInventorySize(strings.length), title);

            int counter = 0;
            for (String itemString : strings) {
                Object object = Base64Util.base64ToObject(itemString);

                if (object instanceof ItemStack) {
                    inventory.setItem(counter, (ItemStack) object);
                    counter++;
                }
            }

            return inventory;
        }

        return plugin.getServer().createInventory(inventoryHolder, 9, title);
    }
}
