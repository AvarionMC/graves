package org.avarion.graves.integration;

import com.jojodmo.itembridge.ItemBridgeListener;
import com.jojodmo.itembridge.ItemBridgeListenerPriority;
import org.avarion.graves.Graves;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemBridge implements ItemBridgeListener {

    private final Graves plugin;
    private com.jojodmo.itembridge.ItemBridge itemBridge;

    public ItemBridge(Graves plugin) {
        this.plugin = plugin;

        unregister();
        register();
    }

    private void unregister() {
        if (itemBridge != null) {
            itemBridge.removeListener(this);
        }
    }

    private void register() {
        itemBridge = new com.jojodmo.itembridge.ItemBridge(plugin, "graves", "grave");

        itemBridge.registerListener(this);
    }

    @Override
    public ItemBridgeListenerPriority getPriority() {
        return ItemBridgeListenerPriority.MEDIUM;
    }

    @Override
    public @Nullable ItemStack fetchItemStack(@NotNull String string) {
        string = string.toLowerCase();

        if (string.startsWith("token_")) {
            string = string.replaceFirst("token_", "");

            return plugin.getConfig().isSet("settings.token." + string)
                   ? plugin.getRecipeManager().getToken(string)
                   : null;
        }

        return null;
    }

    @Override
    public @Nullable String getItemName(@NotNull ItemStack itemStack) {
        if (plugin.getRecipeManager().isToken(itemStack)) {
            return plugin.getRecipeManager().getTokenName(itemStack);
        }

        return null;
    }

    @Override
    public boolean isItem(@NotNull ItemStack itemStack, @NotNull String string) {
        return string.equals(getItemName(itemStack));
    }

}
