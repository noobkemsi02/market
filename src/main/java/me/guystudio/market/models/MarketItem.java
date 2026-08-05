package me.guystudio.market.models;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class MarketItem {
    private final UUID id; // ID duy nhất của đơn hàng
    private final UUID ownerId; // UUID của người bán
    private ItemStack itemToSell; // Mặt hàng bán
    private ItemStack priceItem; // Mặt hàng dùng để làm giá (vd: 32 kim cương)
    private int amount; // Số lượng còn lại

    public MarketItem(UUID ownerId, ItemStack itemToSell, ItemStack priceItem, int amount) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.itemToSell = itemToSell;
        this.priceItem = priceItem;
        this.amount = amount;
    }

    // Các Getter và Setter
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public ItemStack getItemToSell() { return itemToSell; }
    public ItemStack getPriceItem() { return priceItem; }
    public int getAmount() { return amount; }

    public void setAmount(int amount) { this.amount = amount; }
}