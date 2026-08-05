package me.guystudio.market.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class MarketGUI {

    public static Map<UUID, SessionData> activeSessions = new HashMap<>();
    public static HashMap<UUID, Integer> playerVaultPages = new HashMap<>();

    public static class SessionData {
        public ItemStack itemToSell; public ItemStack priceItem; public int quantity;
        public SessionData(ItemStack itemToSell, ItemStack priceItem, int quantity) {
            this.itemToSell = itemToSell; this.priceItem = priceItem; this.quantity = quantity;
        }
    }

    // Gọi hàm này khi mở chợ mặc định (Trang 1)
    public static void openMainMenu(Player player) {
        openMainMenu(player, 1);
    }

    public static void openMainMenu(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', "&8Khu Chợ Chung"));
                List<me.guystudio.market.manager.MarketManager.Listing> items = me.guystudio.market.manager.MarketManager.marketItems;        int maxItems = 45;        int totalPages = (int) Math.ceil((double) items.size() / maxItems);        if (totalPages == 0) totalPages = 1;        if (page < 1) page = 1;        if (page > totalPages) page = totalPages;                int start = (page - 1) * maxItems;        int end = Math.min(start + maxItems, items.size());                for (int i = start; i < end; i++) {            me.guystudio.market.manager.MarketManager.Listing listing = items.get(i);            ItemStack display = listing.item.clone();                        // 1. Đọc Quy cách đóng gói (Bundle Size)            int bundleSize = 1;            if (display.hasItemMeta() && display.getItemMeta().hasLore()) {                for (String line : display.getItemMeta().getLore()) {                    if (line.startsWith(ChatColor.DARK_GRAY + "Bundle: ")) {                        try { bundleSize = Integer.parseInt(line.substring((ChatColor.DARK_GRAY + "Bundle: ").length())); }                         catch (Exception ignored) {}                    }                }            }                        int stock = listing.item.getAmount(); // Tồn kho hệ thống            display.setAmount(Math.min(bundleSize, 64)); // SET AMOUNT CHO ICON ĐỂ HIỂN THỊ ĐÚNG STACK!                        ItemMeta meta = display.getItemMeta();            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();                        // Dọn dẹp lore cũ cho sạch sẽ            lore.removeIf(line -> line.contains("Người bán:") || line.contains("Quy cách:") || line.contains("Tồn kho:") || line.contains("Giá") || line.contains("▶"));                        String sellerName = "Unknown";            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(listing.sellerId);            if (op != null && op.getName() != null) sellerName = op.getName();                        // Render Lore mới            lore.add("");            lore.add(ChatColor.GRAY + "Người bán: " + ChatColor.AQUA + sellerName);            lore.add(ChatColor.GRAY + "Quy cách: " + ChatColor.YELLOW + bundleSize + " vật phẩm/đơn");            lore.add(ChatColor.GRAY + "Tồn kho: " + ChatColor.GREEN + stock + " đơn");            if (listing.moneyPrice > 0) {
                lore.add(ChatColor.GREEN + "Giá tiền: " + ChatColor.YELLOW + listing.moneyPrice + "$");
            }
            if (listing.price != null && listing.price.getType() != Material.AIR) {
                lore.add(ChatColor.GREEN + "Yêu cầu: " + ChatColor.AQUA + listing.price.getAmount() + "x " + listing.price.getType().name());
            }
            if (lore.isEmpty()) {
                lore.add(ChatColor.GREEN + "MIỄN PHÍ!");
            }            lore.add("");            lore.add(ChatColor.GREEN + "▶ Chuột trái: Mua 1 đơn");            lore.add(ChatColor.GREEN + "▶ Chuột phải: Mua tối đa");                        meta.setLore(lore);            display.setItemMeta(meta);            inv.setItem(i - start, display);        }                // 2. THÊM 2 NÚT SANG/LÙI TRANG        if (page > 1) {            ItemStack prev = new ItemStack(Material.ARROW);            ItemMeta m = prev.getItemMeta();            m.setDisplayName(ChatColor.YELLOW + "Trang trước");            prev.setItemMeta(m);            inv.setItem(45, prev);        }        if (page < totalPages) {            ItemStack next = new ItemStack(Material.ARROW);            ItemMeta m = next.getItemMeta();            m.setDisplayName(ChatColor.YELLOW + "Trang sau");            next.setItemMeta(m);            inv.setItem(53, next);        }                ItemStack btnManage = new ItemStack(Material.CHEST);        ItemMeta m1 = btnManage.getItemMeta();        m1.setDisplayName(ChatColor.GREEN + "Quản lý mặt hàng");        btnManage.setItemMeta(m1);        inv.setItem(48, btnManage);                ItemStack btnSell = new ItemStack(Material.DIAMOND);        ItemMeta m2 = btnSell.getItemMeta();        m2.setDisplayName(ChatColor.AQUA + "Đăng bán vật phẩm");        btnSell.setItemMeta(m2);        inv.setItem(49, btnSell);                ItemStack btnVault = new ItemStack(Material.ENDER_CHEST);        ItemMeta m3 = btnVault.getItemMeta();        m3.setDisplayName(ChatColor.LIGHT_PURPLE + "Hòm đồ & Thư báo");        btnVault.setItemMeta(m3);        inv.setItem(50, btnVault);                player.openInventory(inv);    }

    // Giao diện Hòm đồ
        public static void openVaultMenu(Player player) {
        openVaultMenu(player, 1);
    }

    public static void openVaultMenu(Player player, int page) {
        int vaultCount = me.guystudio.market.manager.MarketManager.getPlayerVaultLimit(player);
        boolean isPaginated = vaultCount > 51; // Quá 51 hòm mới xé trang
        int guiSize;

        // Tính toán độ co dãn GUI
        if (isPaginated) {
            guiSize = 54;
        } else {
            guiSize = (int) Math.ceil((vaultCount + 2) / 9.0) * 9; 
            if (guiSize > 54) guiSize = 54; // Chốt chặn an toàn
        }

        Inventory inv = Bukkit.createInventory(null, guiSize, ChatColor.translateAlternateColorCodes('&', "&8Kho Đồ & Thư Báo"));

        // 1. Phễu nhận đồ (Luôn ở Slot 0 đầu tiên)
        ItemStack hopper = new ItemStack(Material.HOPPER);
        ItemMeta hMeta = hopper.getItemMeta();
        hMeta.setDisplayName(ChatColor.YELLOW + "Nhận tất cả thư (Tiền/Đồ)");
        hopper.setItemMeta(hMeta);
        inv.setItem(0, hopper);

        if (!isPaginated) {
            // Trường hợp < 51 Hòm (Không có nút chuyển trang)
            for (int i = 1; i <= vaultCount; i++) {
                ItemStack vault = new ItemStack(Material.ENDER_CHEST);
                ItemMeta vMeta = vault.getItemMeta();
                vMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Hòm đồ " + i);
                vMeta.setLore(Arrays.asList(ChatColor.GRAY + "Nhấn để quản lý vật phẩm của bạn"));
                vault.setItemMeta(vMeta);
                inv.setItem(i, vault); 
            }

            // Nút Back luôn nằm ở góc cuối bên phải
            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.setDisplayName(ChatColor.RED + "Trở về khu chợ");
            back.setItemMeta(backMeta);
            inv.setItem(guiSize - 1, back);

        } else {
            // Trường hợp > 51 Hòm (Bật phân trang thông minh)
            int maxPerPage = 44; // Số lượng hòm tối đa 1 trang chứa được
            int totalPages = (int) Math.ceil((double) vaultCount / maxPerPage);
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;

            int startVault = (page - 1) * maxPerPage + 1;
            int endVault = Math.min(startVault + maxPerPage - 1, vaultCount);

            int slot = 1; // Xếp hòm bắt đầu từ Slot 1
            for (int i = startVault; i <= endVault; i++) {
                ItemStack vault = new ItemStack(Material.ENDER_CHEST);
                ItemMeta vMeta = vault.getItemMeta();
                vMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Hòm đồ " + i);
                vMeta.setLore(Arrays.asList(ChatColor.GRAY + "Nhấn để quản lý vật phẩm của bạn"));
                vault.setItemMeta(vMeta);
                inv.setItem(slot++, vault);
            }

            // Nút điều hướng
            if (page > 1) {
                ItemStack prev = new ItemStack(Material.ARROW);
                ItemMeta m = prev.getItemMeta();
                m.setDisplayName(ChatColor.YELLOW + "Trang trước");
                prev.setItemMeta(m);
                inv.setItem(45, prev);
            }

            if (page < totalPages) {
                ItemStack next = new ItemStack(Material.ARROW);
                ItemMeta m = next.getItemMeta();
                m.setDisplayName(ChatColor.YELLOW + "Trang sau");
                next.setItemMeta(m);
                inv.setItem(53, next);
            }

            // Nút back được đặt ở giữa, dưới cùng
            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.setDisplayName(ChatColor.RED + "Trở về khu chợ");
            back.setItemMeta(backMeta);
            inv.setItem(49, back); 
        }

        playerVaultPages.put(player.getUniqueId(), page);
        player.openInventory(inv);
    }

    // THÊM ĐOẠN CODE NÀY VÀO TRONG FILE MarketGUI.java
    public static void openManageMenu(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', "&8Quản Lý Mặt Hàng Của Bạn"));
        
        // Lọc danh sách mặt hàng của riêng người chơi này
        List<me.guystudio.market.manager.MarketManager.Listing> playerListings = new ArrayList<>();
        for (me.guystudio.market.manager.MarketManager.Listing listing : me.guystudio.market.manager.MarketManager.marketItems) {
            if (listing.sellerId.equals(player.getUniqueId())) {
                playerListings.add(listing);
            }
        }

        // Tính toán phân trang
        int maxItemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) playerListings.size() / maxItemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, playerListings.size());

        // Hiển thị vật phẩm
        for (int i = startIndex; i < endIndex; i++) {
            me.guystudio.market.manager.MarketManager.Listing listing = playerListings.get(i);
            ItemStack displayItem = listing.item.clone();
            org.bukkit.inventory.meta.ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(ChatColor.GRAY + "Giá: " + ChatColor.GOLD + listing.price.getAmount() + " " + listing.price.getType().name());
                lore.add("");
                lore.add(ChatColor.RED + "▶ Click để thu hồi vật phẩm này");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            inv.setItem(i - startIndex, displayItem);
        }

        // Các nút điều hướng
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(ChatColor.YELLOW + "Trang trước");
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Trở về khu chợ");
        back.setItemMeta(backMeta);
        inv.setItem(49, back);
        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(ChatColor.YELLOW + "Trang sau");
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }
        player.openInventory(inv);
    }

    // Giữ nguyên các GUI khác (openCreateListing, openQuantitySetup) - lấy từ file hiện tại
    public static void openSellMenu(Player player) {
        // Wrapper for backward compatibility with MSellCommand
        // Opens the standard create-listing GUI where player places item and price.
        openCreateListing(player);
    }

    public static void openCreateListing(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&', "&8Đặt Đơn Hàng"));
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) { bgMeta.setDisplayName(" "); background.setItemMeta(bgMeta); }
        for (int i = 0; i < 9; i++) { if (i != 2 && i != 4 && i != 6) { inv.setItem(i, background); } }
        ItemStack confirmBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmBtn.getItemMeta();
        if (confirmMeta != null) {            confirmMeta.setDisplayName(ChatColor.GREEN + "Xác Nhận Đơn Hàng");            confirmBtn.setItemMeta(confirmMeta);        }        inv.setItem(6, confirmBtn);
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) { backMeta.setDisplayName(ChatColor.RED + "Trở Về Khu Chợ"); backBtn.setItemMeta(backMeta); }
        inv.setItem(8, backBtn); // Đặt ở ô số 8 (góc trên cùng bên phải)        player.openInventory(inv);    }
    
    public static void openQuantitySetup(Player player, ItemStack itemToSell, ItemStack priceItem, int quantity) {        activeSessions.put(player.getUniqueId(), new SessionData(itemToSell, priceItem, quantity));        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&8Chỉnh Số Lượng"));        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);        ItemMeta bgMeta = bg.getItemMeta();        if (bgMeta != null) { bgMeta.setDisplayName(" "); bg.setItemMeta(bgMeta); }        for (int i = 0; i < 27; i++) inv.setItem(i, bg);        ItemStack minusBtn = new ItemStack(Material.RED_STAINED_GLASS_PANE);        ItemMeta minusMeta = minusBtn.getItemMeta();        minusMeta.setDisplayName(ChatColor.RED + "Bớt Số Lượng");        minusMeta.setLore(Arrays.asList(ChatColor.GRAY + "Chuột Trái: Bớt 1"));        minusBtn.setItemMeta(minusMeta);        inv.setItem(11, minusBtn);        ItemStack displayItem = itemToSell.clone();        displayItem.setAmount(1);        ItemMeta displayMeta = displayItem.getItemMeta();        List<String> lore = displayMeta.hasLore() ? displayMeta.getLore() : new ArrayList<>();        lore.add("");        lore.add(ChatColor.GRAY + "Số lượng sẽ bán: " + ChatColor.GREEN + quantity);        if (priceItem.getType() == Material.BARRIER) {            lore.add(ChatColor.GRAY + "Mức giá: " + ChatColor.AQUA + "Miễn Phí");        } else {            lore.add(ChatColor.GRAY + "Mức giá: " + ChatColor.YELLOW + priceItem.getType().name());        }        displayMeta.setLore(lore);        displayItem.setItemMeta(displayMeta);        inv.setItem(13, displayItem);        ItemStack addBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);        ItemMeta addMeta = addBtn.getItemMeta();        addMeta.setDisplayName(ChatColor.GREEN + "Thêm Số Lượng");        addMeta.setLore(Arrays.asList(            ChatColor.GRAY + "Chuột Trái: Thêm 1",            ChatColor.GRAY + "Chuột Phải: Lấy tối đa trong kho"        ));        addBtn.setItemMeta(addMeta);        inv.setItem(15, addBtn);        ItemStack finalConfirm = new ItemStack(Material.SUNFLOWER);        ItemMeta finalMeta = finalConfirm.getItemMeta();        finalMeta.setDisplayName(ChatColor.GOLD + "ĐĂNG BÁN MẶT HÀNG");        finalConfirm.setItemMeta(finalMeta);        inv.setItem(22, finalConfirm);
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) { backMeta.setDisplayName(ChatColor.RED + "Trở Về Khu Chợ"); backBtn.setItemMeta(backMeta); }
        inv.setItem(26, backBtn); // Đặt ở ô số 26 (góc dưới cùng bên phải)        player.openInventory(inv);    }

    // Hàm tạo nút nhanh
    private static ItemStack createBtn(Material mat, String name, String lore) {        ItemStack item = new ItemStack(mat);        ItemMeta meta = item.getItemMeta();        if (meta != null) {            meta.setDisplayName(name);            meta.setLore(Arrays.asList(lore));            item.setItemMeta(meta);        }        return item;    }
}

