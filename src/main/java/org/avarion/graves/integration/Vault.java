package org.avarion.graves.integration;

import net.milkbowl.vault.economy.Economy;
import org.avarion.graves.Graves;
import org.bukkit.OfflinePlayer;

public final class Vault {

    private final Economy economy;
    private final Graves plugin;

    public Vault(Graves plugin) {
        this.plugin = plugin;

        var tmp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        this.economy = tmp == null ? null : tmp.getProvider();
    }

    public boolean hasBalance(OfflinePlayer player, double balance) {
        return economy.getBalance(player) >= balance;
    }

    public double getBalance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    public boolean withdrawBalance(OfflinePlayer player, double balance) {
        return balance <= 0 || economy.withdrawPlayer(player, balance).transactionSuccess();
    }

    public boolean hasServiceProvider() {
        return economy != null;
    }
}
