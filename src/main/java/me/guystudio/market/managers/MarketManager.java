package me.guystudio.market.managers;

import me.guystudio.market.models.ListingSession;
import me.guystudio.market.models.MarketItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarketManager {
    // Danh sách toàn bộ mặt hàng đang bán trên chợ
    private final List<MarketItem> activeListings = new ArrayList<>();

    // Lưu trữ session tạm thời khi người chơi đang tạo đơn hàng
    private final Map<UUID, ListingSession> playerSessions = new HashMap<>();

    // Các hàm quản lý Listing
    public void addListing(MarketItem item) {
        activeListings.add(item);
    }

    public List<MarketItem> getActiveListings() {
        return activeListings;
    }

    // Các hàm quản lý Session (Tiến trình tạo đơn)
    public ListingSession getSession(UUID playerId) {
        // Trả về session hiện tại, nếu chưa có thì tạo mới
        return playerSessions.computeIfAbsent(playerId, k -> new ListingSession());
    }

    public void clearSession(UUID playerId) {
        playerSessions.remove(playerId);
    }
}