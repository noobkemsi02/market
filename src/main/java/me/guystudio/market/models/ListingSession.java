package me.guystudio.market.models;

import org.bukkit.inventory.ItemStack;

public class ListingSession {
    private ItemStack tempItemToSell;
    private ItemStack tempPriceItem;

    public ListingSession() {
        this.tempItemToSell = null;
        this.tempPriceItem = null;
    }

    public void setTempItemToSell(ItemStack item) { this.tempItemToSell = item; }
    public ItemStack getTempItemToSell() { return tempItemToSell; }

    public void setTempPriceItem(ItemStack item) { this.tempPriceItem = item; }
    public ItemStack getTempPriceItem() { return tempPriceItem; }

    public boolean isReadyForQuantity() {
        return tempItemToSell != null && tempPriceItem != null;
    }
}