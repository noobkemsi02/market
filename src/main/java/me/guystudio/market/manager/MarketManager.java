package me.guystudio.market.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MarketManager {
    public static List<Listing> marketItems = new ArrayList<>();
    public static Map<UUID, List<ItemStack>> mailboxes = new HashMap<>();
    public static Map<UUID, Map<Integer, Inventory>> playerVaults = new HashMap<>();
    public static Map<UUID, Double> pendingMoney = new HashMap<>();

    public static class Listing {
        public UUID sellerId;
        public ItemStack item;
        public ItemStack price;
        public double moneyPrice; // [MỚI] Giá tiền tệ

        public Listing(UUID sellerId, ItemStack item, ItemStack price, double moneyPrice) {
            this.sellerId = sellerId; 
            this.item = item; 
            this.price = price;
            this.moneyPrice = moneyPrice;
        }
    }

    public static void addListing(UUID sellerId, ItemStack item, ItemStack price, int amount, double moneyPrice) {
        ItemStack finalItem = item.clone();
        finalItem.setAmount(amount);
        marketItems.add(new Listing(sellerId, finalItem, price, moneyPrice));
    }

    public static void addMailboxItem(UUID uuid, ItemStack item) {
        mailboxes.computeIfAbsent(uuid, k -> new ArrayList<>()).add(item.clone());
        savePlayerData(uuid); // THÊM DÒNG NÀY ĐỂ LƯU NGAY LẬP TỨC, CHỐNG MẤT TIỀN!
    }

    public static int getPlayerVaultLimit(Player player) {
        int maxVaults = -1;
        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String perm = permInfo.getPermission().toLowerCase();
            if (perm.startsWith("guymarket.limit.vaults.")) {
                try {
                    int amount = Integer.parseInt(perm.replace("guymarket.limit.vaults.", ""));
                    if (amount > maxVaults) maxVaults = amount;
                } catch (NumberFormatException ignored) {}
            }
        }
        return maxVaults == -1 ? 1 : maxVaults; // Mặc định mỗi người 1 hòm
    }

    // ==========================================
    // HỆ THỐNG LƯU TRỮ VẬT PHẨM (MỚI)
    // ==========================================
    private static File getPlayerFile(UUID uuid) {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(MarketManager.class);
        File folder = new File(plugin.getDataFolder(), "userdata");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, uuid.toString() + ".yml");
    }

    public static void savePlayerData(UUID uuid) {
        File file = getPlayerFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Lưu hộp thư
        if (mailboxes.containsKey(uuid)) {
            config.set("mailbox", mailboxes.get(uuid));
        }

        // Lưu kho đồ (Vault) - Giữ nguyên toàn bộ kể cả khi bị hạ quyền
        if (playerVaults.containsKey(uuid)) {
            Map<Integer, Inventory> vaults = playerVaults.get(uuid);
            for (Map.Entry<Integer, Inventory> entry : vaults.entrySet()) {
                config.set("vaults." + entry.getKey(), entry.getValue().getContents());
            }
        }
        try { config.save(file); } catch (IOException ignored) {}
    }

    public static void loadPlayerData(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Tải hộp thư
        if (config.contains("mailbox")) {
            List<ItemStack> mail = (List<ItemStack>) config.getList("mailbox");
            if (mail != null) mailboxes.put(uuid, mail);
        }

        // Tải kho đồ
        if (config.contains("vaults")) {
            Map<Integer, Inventory> vaults = new HashMap<>();
            for (String key : config.getConfigurationSection("vaults").getKeys(false)) {
                int vaultId = Integer.parseInt(key);
                List<ItemStack> items = (List<ItemStack>) config.getList("vaults." + key);

                // Mặc định tạo một Rương khổng lồ 54 Slot để nhét đồ vào, không bao giờ lo mất đồ
                Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Hòm đồ " + vaultId);
                if (items != null) inv.setContents(items.toArray(new ItemStack[0]));
                vaults.put(vaultId, inv);
            }
            playerVaults.put(uuid, vaults);
        }
    }

    // ==========================================
    // HỆ THỐNG LƯU TRỮ CHỢ CHUNG (MARKET DATA)
    // ==========================================
    public static void saveMarketData() {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(MarketManager.class);
        File file = new File(plugin.getDataFolder(), "market_items.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        config.set("listings", null); // Xóa dữ liệu cũ để ghi đè dữ liệu mới
        
        int index = 0;
        for (Listing listing : marketItems) {
            config.set("listings." + index + ".seller", listing.sellerId.toString());
            config.set("listings." + index + ".item", listing.item);
            config.set("listings." + index + ".price", listing.price);
            config.set("listings." + index + ".money", listing.moneyPrice);
            index++;
        }
        try { config.save(file); } catch (IOException ignored) {}
    }

    public static void loadMarketData() {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(MarketManager.class);
        File file = new File(plugin.getDataFolder(), "market_items.yml");
        if (!file.exists()) return;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        marketItems.clear(); // Dọn rác bộ nhớ trước khi tải
        
        if (config.contains("listings")) {
            for (String key : config.getConfigurationSection("listings").getKeys(false)) {
                try {
                    UUID seller = UUID.fromString(config.getString("listings." + key + ".seller"));
                    ItemStack item = config.getItemStack("listings." + key + ".item");
                    ItemStack price = config.getItemStack("listings." + key + ".price");
                    double money = config.getDouble("listings." + key + ".money", 0.0);
                    marketItems.add(new Listing(seller, item, price, money));
                } catch (Exception ignored) {
                    // Bỏ qua nếu có item lỗi
                }
            }
        }
    }

    // Hàm an toàn để chạy khi tắt server (Lưu toàn bộ người chơi đang kẹt trong RAM)
    public static void saveAllData() {
        Set<UUID> allActiveUsers = new HashSet<>();
        allActiveUsers.addAll(mailboxes.keySet());
        allActiveUsers.addAll(playerVaults.keySet());
        for (UUID uuid : allActiveUsers) {
            savePlayerData(uuid);
        }
    }
}