package me.guystudio.market.commands;

import me.guystudio.market.gui.MarketGUI; // Nhớ import class MarketGUI vừa tạo
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MarketCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Kiểm tra xem người gõ lệnh có phải là Player (người chơi) không
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ có thể sử dụng trong game bởi người chơi!");
            return true;
        }

        Player player = (Player) sender;

        // Tạm thời gửi tin nhắn để test, sau này chúng ta sẽ thay bằng code mở GUI
        player.sendMessage("§a[Market] Đang mở giao diện khu chợ chung...");

        // Gọi hàm mở GUI ngay tại đây!
        MarketGUI.openMainMenu(player);

        return true;
    }
}