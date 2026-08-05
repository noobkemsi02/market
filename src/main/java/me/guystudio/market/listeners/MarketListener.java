package me.guystudio.market.listeners;

import me.guystudio.market.gui.MarketGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MarketListener implements Listener {

    public static List<UUID> isRefreshing = new ArrayList<>();
    public static HashMap<UUID, Integer> playerManagePages = new HashMap<>();
    public static HashMap<UUID, Integer> playerMainPages = new HashMap<>();

    // ==========================================
    // CÔNG CỤ ĐỌC LUCKPERMS
    // ==========================================
    public static int getPermissionInt(Player player, String prefix, int defaultVal) {
        int max = defaultVal;
        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String perm = permInfo.getPermission().toLowerCase();
            if (perm.startsWith(prefix.toLowerCase())) {
                try {
                    int val = Integer.parseInt(perm.substring(prefix.length()));
                    if (val > max) max = val;
                } catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }

    private int getValidVaultSize(int requested) {
        if (requested <= 9) return 9;
        if (requested <= 18) return 18;
        if (requested <= 27) return 27;
        if (requested <= 36) return 36;
        if (requested <= 45) return 45;
        return 54;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        // ==========================================
        // 1. GUI KHU CHỢ CHUNG
        // ==========================================
        if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8Khu Chợ Chung"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Material type = event.getCurrentItem().getType();
            int slot = event.getRawSlot();
            int currentPage = playerMainPages.getOrDefault(player.getUniqueId(), 1);

            if (slot == 45 && type == Material.ARROW) {
                playerMainPages.put(player.getUniqueId(), currentPage - 1);
                isRefreshing.add(player.getUniqueId());
                MarketGUI.openMainMenu(player, currentPage - 1);
                isRefreshing.remove(player.getUniqueId());
                return;
            } else if (slot == 53 && type == Material.ARROW) {
                playerMainPages.put(player.getUniqueId(), currentPage + 1);
                isRefreshing.add(player.getUniqueId());
                MarketGUI.openMainMenu(player, currentPage + 1);
                isRefreshing.remove(player.getUniqueId());
                return;
            }

            if (slot == 49 && type == Material.DIAMOND) {
                // ĐỌC GIỚI HẠN TỪ LUCKPERMS
                int limit = getPermissionInt(player, "guymarket.limit.listings.", 3); // Mặc định 3 mặt hàng

                int currentListings = 0;
                for (me.guystudio.market.manager.MarketManager.Listing l : me.guystudio.market.manager.MarketManager.marketItems) {
                    if (l.sellerId.equals(player.getUniqueId())) currentListings++;
                }

                if (currentListings >= limit) {
                    player.sendMessage(ChatColor.RED + "Bạn đã đạt giới hạn đăng tối đa " + limit + " mặt hàng (Hãy nâng cấp Rank để đăng thêm)!");
                    return;
                }

                MarketGUI.openCreateListing(player);
                return;
            } else if (slot == 48 && type == Material.CHEST) {
                playerManagePages.put(player.getUniqueId(), 1);
                isRefreshing.add(player.getUniqueId());
                MarketGUI.openManageMenu(player, 1);
                isRefreshing.remove(player.getUniqueId());
                return;
            } else if (slot == 50 && type == Material.ENDER_CHEST) {
                MarketGUI.openVaultMenu(player);
                return;
            }

            // ... (Phần code mua hàng giữ nguyên như cũ - không thay đổi)
            if (slot >= 0 && slot < 45) {
                int index = (currentPage - 1) * 45 + slot;
                if (index >= me.guystudio.market.manager.MarketManager.marketItems.size()) return;

                me.guystudio.market.manager.MarketManager.Listing listing = me.guystudio.market.manager.MarketManager.marketItems.get(index);
                if (listing.sellerId.equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Bạn không thể tự mua hàng của chính mình!"); return;
                }

                // KẾT NỐI VAULT
                net.milkbowl.vault.economy.Economy econ = me.guystudio.market.Market.getEconomy();
                if (listing.moneyPrice > 0) {
                    if (!econ.has(player, listing.moneyPrice)) {
                        player.sendMessage(ChatColor.RED + "Bạn không có đủ " + listing.moneyPrice + "$ để mua vật phẩm này!");
                        return;
                    }
                }

                int amountToBuy = 0;
                boolean isFree = listing.price == null || listing.price.getType() == Material.BARRIER;
                int bundleSize = getBundleSize(listing.item);

                if (event.getClick().isLeftClick()) { amountToBuy = 1; }
                else if (event.getClick().isRightClick()) {
                    if (isFree) { amountToBuy = listing.item.getAmount(); }
                    else {
                        int pricePerItem = listing.price.getAmount();
                        int playerCurrency = countItemsInInventory(player, new ItemStack(listing.price.getType()));
                        int affordable = playerCurrency / pricePerItem;
                        amountToBuy = Math.min(listing.item.getAmount(), affordable);
                        if (amountToBuy == 0) { player.sendMessage(ChatColor.RED + "Bạn không đủ tài nguyên để mua thêm vật phẩm này!"); return; }
                    }
                } else return;

                int totalCost = isFree ? 0 : listing.price.getAmount() * amountToBuy;
                int totalItemsToGive = amountToBuy * bundleSize;

                Inventory sim = Bukkit.createInventory(null, 36);
                sim.setContents(player.getInventory().getStorageContents());
                if (!isFree) {
                    if (countItemsInInventory(player, new ItemStack(listing.price.getType())) < totalCost) {
                        player.sendMessage(ChatColor.RED + "Bạn không có đủ " + totalCost + " " + listing.price.getType().name() + "!"); return;
                    }
                    int costLeft = totalCost;
                    for (int i = 0; i < sim.getSize(); i++) {
                        ItemStack item = sim.getItem(i);
                        if (item != null && item.getType() == listing.price.getType()) {
                            if (item.getAmount() > costLeft) { item.setAmount(item.getAmount() - costLeft); costLeft = 0; break; }
                            else { costLeft -= item.getAmount(); sim.setItem(i, null); }
                        }
                    }
                }

                ItemStack cleanItem = getCleanItem(listing.item);
                List<ItemStack> itemsToGive = new ArrayList<>();
                int amountLeft = totalItemsToGive;
                while (amountLeft > 0) {
                    ItemStack stack = cleanItem.clone();
                    int maxStack = stack.getMaxStackSize();
                    if (maxStack <= 0) maxStack = 64;
                    int give = Math.min(amountLeft, maxStack);
                    stack.setAmount(give);
                    itemsToGive.add(stack);
                    amountLeft -= give;
                }

                HashMap<Integer, ItemStack> leftoverSim = new HashMap<>();
                for (ItemStack item : itemsToGive) leftoverSim.putAll(sim.addItem(item.clone()));
                if (!leftoverSim.isEmpty()) { player.sendMessage(ChatColor.RED + "Túi đồ của bạn không đủ chỗ chứa!"); return; }

                // Trừ tiền và đồ của người mua
                if (listing.moneyPrice > 0) {
                    econ.withdrawPlayer(player, listing.moneyPrice);
                }
                if (listing.price != null && listing.price.getType() != Material.AIR) {
                    removeItemsFromInventory(player, new ItemStack(listing.price.getType()), totalCost);
                }

                // Chuyển đồ và tiền cho người bán (Cho vào Mailbox và Vault offline)
                if (listing.moneyPrice > 0) {
                    org.bukkit.OfflinePlayer offlineSeller = org.bukkit.Bukkit.getOfflinePlayer(listing.sellerId);
                    econ.depositPlayer(offlineSeller, listing.moneyPrice);
                }
                if (listing.price != null && listing.price.getType() != Material.AIR) {
                    ItemStack payment = new ItemStack(listing.price.getType(), totalCost);
                    me.guystudio.market.manager.MarketManager.addMailboxItem(listing.sellerId, payment);
                }

                // Giao hàng cho người mua
                for (ItemStack item : itemsToGive) player.getInventory().addItem(item);

                listing.item.setAmount(listing.item.getAmount() - amountToBuy);
                if (listing.item.getAmount() <= 0) {
                    me.guystudio.market.manager.MarketManager.marketItems.remove(listing);
                }

                player.sendMessage(ChatColor.GREEN + "Mua thành công!");
                isRefreshing.add(player.getUniqueId());
                MarketGUI.openMainMenu(player, currentPage);
                isRefreshing.remove(player.getUniqueId());
            }
        }

        // ==========================================
        // 1.5 GUI QUẢN LÝ MẶT HÀNG (MỚI)
        // ==========================================
        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8Quản Lý Mặt Hàng Của Bạn"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            int slot = event.getRawSlot();
            int currentPage = playerManagePages.getOrDefault(player.getUniqueId(), 1);

            if (slot == 45 && event.getCurrentItem().getType() == Material.ARROW) {
                playerManagePages.put(player.getUniqueId(), currentPage - 1);
                isRefreshing.add(player.getUniqueId());
                MarketGUI.openManageMenu(player, currentPage - 1);
                isRefreshing.remove(player.getUniqueId());
                return;
            } else if (slot == 53 && event.getCurrentItem().getType() == Material.ARROW) {
                playerManagePages.put(player.getUniqueId(), currentPage + 1);
                isRefreshing.add(player.getUniqueId());
                MarketGUI.openManageMenu(player, currentPage + 1);
                isRefreshing.remove(player.getUniqueId());
                return;
            } else if (slot == 49) {
                isRefreshing.add(player.getUniqueId());
                int mainPage = playerMainPages.getOrDefault(player.getUniqueId(), 1);
                MarketGUI.openMainMenu(player, mainPage);
                isRefreshing.remove(player.getUniqueId());
                return;
            }

            if (slot >= 0 && slot < 45) {
                List<me.guystudio.market.manager.MarketManager.Listing> playerListings = new ArrayList<>();
                for (me.guystudio.market.manager.MarketManager.Listing listing : me.guystudio.market.manager.MarketManager.marketItems) {
                    if (listing.sellerId.equals(player.getUniqueId())) playerListings.add(listing);
                }

                int index = (currentPage - 1) * 45 + slot;
                if (index < playerListings.size()) {
                    me.guystudio.market.manager.MarketManager.Listing targetListing = playerListings.get(index);

                    int bundleSize = getBundleSize(targetListing.item);
                    ItemStack cleanItem = getCleanItem(targetListing.item);
                    cleanItem.setAmount(1);

                    if (event.getClick().isLeftClick()) {
                        // Bổ sung 1 đơn
                        if (countItemsInInventory(player, cleanItem) >= bundleSize) {
                            removeItemsFromInventory(player, cleanItem, bundleSize);
                            targetListing.item.setAmount(targetListing.item.getAmount() + 1);
                            player.sendMessage(ChatColor.GREEN + "Đã bổ sung 1 đơn vào gian hàng!");
                        } else {
                            player.sendMessage(ChatColor.RED + "Bạn không đủ đồ trong túi để bổ sung!");
                        }
                    }
                    else if (event.getClick().isRightClick()) {
                        if (event.getClick().isShiftClick()) {
                            // Rút toàn bộ
                            int totalToRefund = targetListing.item.getAmount() * bundleSize;
                            me.guystudio.market.manager.MarketManager.marketItems.remove(targetListing);
                            giveItems(player, cleanItem, totalToRefund);
                            player.sendMessage(ChatColor.GREEN + "Đã thu hồi toàn bộ mặt hàng!");
                        } else {
                            // Rút 1 đơn
                            targetListing.item.setAmount(targetListing.item.getAmount() - 1);
                            giveItems(player, cleanItem, bundleSize);
                            player.sendMessage(ChatColor.YELLOW + "Đã rút 1 đơn về túi!");

                            if (targetListing.item.getAmount() <= 0) {
                                me.guystudio.market.manager.MarketManager.marketItems.remove(targetListing);
                            }
                        }
                    }

                    isRefreshing.add(player.getUniqueId());
                    MarketGUI.openManageMenu(player, currentPage);
                    isRefreshing.remove(player.getUniqueId());
                }
            }
        }

        // ==========================================
        // 2. HÒM ĐỒ & THƯ BÁO (Co giãn tự động)
        // ==========================================
        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8Kho Đồ & Thư Báo"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Material type = event.getCurrentItem().getType();
            int currentPage = MarketGUI.playerVaultPages.getOrDefault(player.getUniqueId(), 1);

            if (type == Material.HOPPER) {
                // Nhận đồ trong phễu thư báo
                List<ItemStack> mail = me.guystudio.market.manager.MarketManager.mailboxes.get(player.getUniqueId());
                if (mail == null || mail.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Hòm thư của bạn đang trống!");
                    return;
                }
                List<ItemStack> leftovers = new ArrayList<>();
                for (ItemStack mailItem : mail) {
                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(mailItem);
                    if (!left.isEmpty()) leftovers.addAll(left.values());
                }
                if (!leftovers.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "Túi đồ của bạn đã đầy! Đã nhận tối đa, phần còn lại vẫn ở trong thư.");
                    me.guystudio.market.manager.MarketManager.mailboxes.put(player.getUniqueId(), leftovers);
                } else {
                    player.sendMessage(ChatColor.GREEN + "Đã nhận toàn bộ đồ trong hòm thư!");
                    me.guystudio.market.manager.MarketManager.mailboxes.remove(player.getUniqueId());
                    me.guystudio.market.manager.MarketManager.savePlayerData(player.getUniqueId()); // Lưu File tức thì
                }
            } 
            else if (type == Material.BARRIER) {
                // Quay lại chợ
                isRefreshing.add(player.getUniqueId());
                int mainPage = playerMainPages.getOrDefault(player.getUniqueId(), 1);
                MarketGUI.openMainMenu(player, mainPage);
                isRefreshing.remove(player.getUniqueId());
            }
            else if (type == Material.ARROW) {
                // Điều hướng phân trang 
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                if (itemName.contains("Trang trước")) {
                    isRefreshing.add(player.getUniqueId());
                    MarketGUI.openVaultMenu(player, currentPage - 1);
                    isRefreshing.remove(player.getUniqueId());
                } else if (itemName.contains("Trang sau")) {
                    isRefreshing.add(player.getUniqueId());
                    MarketGUI.openVaultMenu(player, currentPage + 1);
                    isRefreshing.remove(player.getUniqueId());
                }
            }
            else if (type == Material.ENDER_CHEST) {
                // Mở hòm đồ thông minh dựa vào tên (ví dụ: "Hòm đồ 10" -> Tách số 10 ra)
                String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                try {
                    int vaultId = Integer.parseInt(name.replace("Hòm đồ ", "").trim());

                    // Kéo dữ liệu Rương Đồ ra. Nếu người đó mới xài lần đầu, tạo một hòm 54 Slot mới.
                    Inventory vault = me.guystudio.market.manager.MarketManager.playerVaults
                            .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                            .computeIfAbsent(vaultId, k -> Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Hòm đồ " + vaultId));
                    player.openInventory(vault);
                } catch (Exception ignored) {}
            }
        }

        // ==========================================
        // 3. GUI ĐẶT ĐƠN HÀNG (Giữ nguyên)
        // ==========================================
        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8Đặt Đơn Hàng"))) {
            // ... Không thay đổi so với phiên bản trước (Mình xin phép rút gọn block này để code ngắn gọn, bạn để y nguyên như cũ nhé)
            int slot = event.getRawSlot();
            if (slot >= 0 && slot <= 8) {
                if (slot != 2 && slot != 4) event.setCancelled(true);
                if (slot == 8) {
                    Inventory inv = event.getClickedInventory();
                    if (inv != null) {
                        ItemStack item = inv.getItem(2);
                        ItemStack price = inv.getItem(4);
                        if (item != null) giveItems(player, item, item.getAmount());
                        if (price != null) giveItems(player, price, price.getAmount());
                        inv.setItem(2, null);
                        inv.setItem(4, null);
                    }
                    isRefreshing.add(player.getUniqueId());
                    int mainPage = playerMainPages.getOrDefault(player.getUniqueId(), 1);
                    MarketGUI.openMainMenu(player, mainPage);
                    isRefreshing.remove(player.getUniqueId());
                    return;
                }
                if (slot == 6) {
                    Inventory inv = event.getClickedInventory();
                    if (inv == null) return;
                    ItemStack itemToSell = inv.getItem(2);
                    ItemStack priceItem = inv.getItem(4);
                    if (itemToSell == null || itemToSell.getType() == Material.AIR) {
                        player.sendMessage(ChatColor.RED + "Bạn phải đặt mặt hàng muốn bán vào ô bên trái!");
                        return;
                    }
                    if (priceItem == null || priceItem.getType() == Material.AIR) { priceItem = new ItemStack(Material.BARRIER); }
                    else { giveItems(player, priceItem, priceItem.getAmount()); inv.setItem(4, null); }

                    int bundleSize = itemToSell.getAmount();
                    ItemStack marketItem = itemToSell.clone();
                    marketItem.setAmount(1);
                    ItemMeta meta = marketItem.getItemMeta();
                    List<String> lore = meta != null && meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add(ChatColor.DARK_GRAY + "Bundle: " + bundleSize);
                    meta.setLore(lore);
                    marketItem.setItemMeta(meta);
                    inv.setItem(2, null);

                    isRefreshing.add(player.getUniqueId());
                    MarketGUI.openQuantitySetup(player, marketItem, priceItem, 1);
                    isRefreshing.remove(player.getUniqueId());
                }
            }
        }

        // ==========================================
        // 4. GUI CHỈNH SỐ LƯỢNG (Giữ nguyên)
        // ==========================================
        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8Chỉnh Số Lượng"))) {
            // ... Không thay đổi so với phiên bản trước
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            int slot = event.getRawSlot();
            if (slot > 26) return;

            MarketGUI.SessionData session = MarketGUI.activeSessions.get(player.getUniqueId());
            if (session == null) { player.closeInventory(); return; }

            int bundleSize = getBundleSize(session.itemToSell);
            ItemStack cleanUnit = getCleanItem(session.itemToSell);
            cleanUnit.setAmount(1);

            if (slot == 11) {
                if (event.getClick().isLeftClick() && session.quantity > 1) {
                    session.quantity--;
                    giveItems(player, cleanUnit, bundleSize);
                    isRefreshing.add(player.getUniqueId());
                    MarketGUI.openQuantitySetup(player, session.itemToSell, session.priceItem, session.quantity);
                    isRefreshing.remove(player.getUniqueId());
                }
            }
            else if (slot == 15) {
                int maxAvailableUnits = countItemsInInventory(player, cleanUnit) / bundleSize;
                if (event.getClick().isLeftClick()) {
                    if (maxAvailableUnits >= 1) {
                        session.quantity++;
                        removeItemsFromInventory(player, cleanUnit, bundleSize);
                        isRefreshing.add(player.getUniqueId());
                        MarketGUI.openQuantitySetup(player, session.itemToSell, session.priceItem, session.quantity);
                        isRefreshing.remove(player.getUniqueId());
                    } else { player.sendMessage(ChatColor.RED + "Bạn không đủ vật phẩm trong túi để bổ sung thêm đơn này!"); }
                }
                else if (event.getClick().isRightClick()) {
                    if (maxAvailableUnits > 0) {
                        session.quantity += maxAvailableUnits;
                        removeItemsFromInventory(player, cleanUnit, maxAvailableUnits * bundleSize);
                        isRefreshing.add(player.getUniqueId());
                        MarketGUI.openQuantitySetup(player, session.itemToSell, session.priceItem, session.quantity);
                        isRefreshing.remove(player.getUniqueId());
                    } else { player.sendMessage(ChatColor.RED + "Bạn không đủ vật phẩm trong túi để bổ sung thêm đơn này!"); }
                }
            }
            else if (slot == 22) {
                // Lấy số tiền đã nhập từ lệnh /msell (mặc định 0 nếu không nhập)
                double money = me.guystudio.market.manager.MarketManager.pendingMoney.getOrDefault(player.getUniqueId(), 0.0);

                me.guystudio.market.manager.MarketManager.addListing(player.getUniqueId(), session.itemToSell, session.priceItem, session.quantity, money);
                // Xóa bộ nhớ tạm sau khi đăng thành công
                me.guystudio.market.manager.MarketManager.pendingMoney.remove(player.getUniqueId());

                player.sendMessage(ChatColor.GREEN + "Bạn đã đăng bán thành công " + session.quantity + " đơn vật phẩm!");
                MarketGUI.activeSessions.remove(player.getUniqueId());
                isRefreshing.add(player.getUniqueId());
                int mainPage = playerMainPages.getOrDefault(player.getUniqueId(), 1);
                MarketGUI.openMainMenu(player, mainPage);
                isRefreshing.remove(player.getUniqueId());
            }
            else if (slot == 26) {
                giveItems(player, cleanUnit, session.quantity * bundleSize);
                MarketGUI.activeSessions.remove(player.getUniqueId());
                isRefreshing.add(player.getUniqueId());
                int mainPage = playerMainPages.getOrDefault(player.getUniqueId(), 1);
                MarketGUI.openMainMenu(player, mainPage);
                isRefreshing.remove(player.getUniqueId());
            }
        }
    }

    // ==========================================
    // 5. CÁC EVENT & TOOLS (Giữ nguyên như cũ)
    // ==========================================
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        // [MỚI] Tự động Lưu File vào Data ngay khi người chơi vừa cất đồ xong
        if (title.startsWith(ChatColor.DARK_GRAY + "Hòm đồ ")) {
            me.guystudio.market.manager.MarketManager.savePlayerData(player.getUniqueId());
        }

        if (isRefreshing.contains(player.getUniqueId())) return;

        if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8Chỉnh Số Lượng"))) {
            MarketGUI.SessionData session = MarketGUI.activeSessions.get(player.getUniqueId());
            if (session != null) {
                int bundleSize = getBundleSize(session.itemToSell);
                ItemStack cleanUnit = getCleanItem(session.itemToSell);
                giveItems(player, cleanUnit, session.quantity * bundleSize);
                MarketGUI.activeSessions.remove(player.getUniqueId());
                player.sendMessage(ChatColor.YELLOW + "Hủy giao dịch, đã hoàn trả lại đồ vào túi!");
            }
        }
        else if (title.equals(ChatColor.translateAlternateColorCodes('&', "&8Đặt Đơn Hàng"))) {
            Inventory inv = event.getInventory();
            ItemStack itemToSell = inv.getItem(2);
            ItemStack priceItem = inv.getItem(4);
            if (itemToSell != null && itemToSell.getType() != Material.AIR) { giveItems(player, itemToSell, itemToSell.getAmount()); inv.setItem(2, null); }
            if (priceItem != null && priceItem.getType() != Material.AIR) { giveItems(player, priceItem, priceItem.getAmount()); inv.setItem(4, null); }
        }
        }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        me.guystudio.market.manager.MarketManager.loadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        me.guystudio.market.manager.MarketManager.savePlayerData(event.getPlayer().getUniqueId());
    }

    private void giveItems(Player player, ItemStack baseItem, int totalAmount) {
        if (baseItem == null || baseItem.getType() == Material.AIR || totalAmount <= 0) return;
        ItemStack item = baseItem.clone();
        int maxStack = item.getMaxStackSize();
        if (maxStack <= 0) maxStack = 64;
        List<ItemStack> list = new ArrayList<>();
        while (totalAmount > 0) {
            ItemStack stack = item.clone();
            int give = Math.min(totalAmount, maxStack);
            stack.setAmount(give);
            list.add(stack);
            totalAmount -= give;
        }
        for (ItemStack st : list) {
            HashMap<Integer, ItemStack> left = player.getInventory().addItem(st);
            for (ItemStack drop : left.values()) player.getWorld().dropItem(player.getLocation(), drop);
        }
    }

    private int getBundleSize(ItemStack marketItem) {
        if (marketItem != null && marketItem.hasItemMeta() && marketItem.getItemMeta().hasLore()) {
            for (String line : marketItem.getItemMeta().getLore()) {
                if (line.startsWith(ChatColor.DARK_GRAY + "Bundle: ")) {
                    try { return Integer.parseInt(line.substring((ChatColor.DARK_GRAY + "Bundle: ").length())); }
                    catch (Exception ignored) {}
                }
            }
        }
        return 1;
    }

    private ItemStack getCleanItem(ItemStack marketItem) {
        ItemStack clean = marketItem.clone();
        if (clean.hasItemMeta() && clean.getItemMeta().hasLore()) {
            ItemMeta meta = clean.getItemMeta();
            List<String> lore = meta.getLore();
            lore.removeIf(line -> line.startsWith(ChatColor.DARK_GRAY + "Bundle: ") || line.startsWith(ChatColor.AQUA + "▶ Đóng gói: "));
            meta.setLore(lore);
            clean.setItemMeta(meta);
        }
        return clean;
    }

    private int countItemsInInventory(Player player, ItemStack matchItem) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(matchItem)) count += item.getAmount();
        }
        return count;
    }

    private void removeItemsFromInventory(Player player, ItemStack matchItem, int amountToRemove) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(matchItem)) {
                if (item.getAmount() > amountToRemove) {
                    item.setAmount(item.getAmount() - amountToRemove);
                    break;
                } else {
                    amountToRemove -= item.getAmount();
                    item.setAmount(0);
                }
            }
        }
    }
}
