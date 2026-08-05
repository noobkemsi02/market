package me.guystudio.market.commands;

import me.guystudio.market.gui.MarketGUI;
import me.guystudio.market.manager.MarketManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MSellCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        double money = 0.0;
        if (args.length > 0) {
            try {
                money = Double.parseDouble(args[0]);
                if (money < 0) money = 0.0;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Số tiền không hợp lệ! Vui lòng nhập một số.");
                return true;
            }
        }

        // Lưu tạm số tiền muốn bán vào bộ nhớ
        MarketManager.pendingMoney.put(player.getUniqueId(), money);
        
        // Mở GUI bán đồ (Giống như cũ)
        MarketGUI.openSellMenu(player);
        return true;
    }
}