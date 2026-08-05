package me.guystudio.market;

import me.guystudio.market.commands.MarketCommand;
import me.guystudio.market.commands.MSellCommand;
import me.guystudio.market.listeners.MarketListener;
import me.guystudio.market.manager.MarketManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Market extends JavaPlugin {

    private static Economy econ = null;
    private static Market instance;

    @Override
    public void onEnable() {
        instance = this;
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        // KẾT NỐI VAULT
        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Vô hiệu hóa plugin do không tìm thấy Vault!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        MarketManager.loadMarketData();
        getServer().getPluginManager().registerEvents(new MarketListener(), this);
        
        getCommand("market").setExecutor(new MarketCommand());
        getCommand("msell").setExecutor(new MSellCommand()); // Đăng ký lệnh mới

        getLogger().info("Market Plugin đã kích hoạt thành công!");
    }

    @Override
    public void onDisable() {
        MarketManager.saveMarketData();
        MarketManager.saveAllData();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }
    
    public static Market getInstance() {
        return instance;
    }
}