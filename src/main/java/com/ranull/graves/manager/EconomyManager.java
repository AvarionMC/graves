package com.ranull.graves.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

public final class EconomyManager {
    private final Economy economy;

    public EconomyManager(Economy economy) {
        this.economy = economy;
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
}
