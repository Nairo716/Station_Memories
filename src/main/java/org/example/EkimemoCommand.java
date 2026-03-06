package org.example;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EkimemoCommand implements CommandExecutor {
    private final Main plugin;
    private final DataManager dataManager;

    public EkimemoCommand(Main plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        plugin.getLogger().info("Ekimemoコマンドが実行されました: /" + label + " " + String.join(" ", args));

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cプレイヤーのみ実行可能です。");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            EkimemoGUI.openMainMenu(player, dataManager);
            return true;
        }
        if (args[0].equalsIgnoreCase("setting")) {
            if (!player.isOp()) {
                player.sendMessage("§c権限（OP）がありません。");
                return true;
            }

            // --- ヘルプ表示 ---
            if (args.length == 1) {
                player.sendMessage("§e--- Ekimemo 設定ヘルプ ---");
                player.sendMessage("§f 路線追加 /ekimemo setting addline <路線名>");
                player.sendMessage("§f 路線削除 /ekimemo setting delline <路線名>");
                player.sendMessage("§f 路線名変更 /ekimemo setting renline <旧名> <新名>");
                player.sendMessage("§f 駅追加 /ekimemo setting addstation <路線名> <駅名>");
                player.sendMessage("§f 駅削除 /ekimemo setting delstation <路線名> <駅名>");
                player.sendMessage("§f 駅名変更 /ekimemo setting renstation <路線名> <旧駅名> <新駅名>");
                return true;
            }

            String action = args[1];

            // 1. 路線追加 (addline)
            if (action.equalsIgnoreCase("addline") && args.length >= 3) {
                dataManager.addLine(args[2]);
                player.sendMessage("§a路線「" + args[2] + "」を追加しました。");
                return true;
            }

            // 2. 路線削除 (delline)
            if (action.equalsIgnoreCase("delline") && args.length >= 3) {
                dataManager.deleteLine(args[2]);
                player.sendMessage("§a路線「" + args[2] + "」を削除しました。");
                return true;
            }

            // 3. 路線名変更 (renline)
            if (action.equalsIgnoreCase("renline") && args.length >= 4) {
                dataManager.renameLine(args[2], args[3]);
                player.sendMessage("§a路線名を「" + args[2] + "」から「" + args[3] + "」に変更しました。");
                return true;
            }

            // 4. 駅追加 (addstation)
            if (action.equalsIgnoreCase("addstation") && args.length >= 4) {
                dataManager.addStation(args[2], args[3], player.getLocation());
                player.sendMessage("§a現在地に駅「" + args[3] + "」を追加しました。");
                return true;
            }

            // 5. 駅削除 (delstation)
            if (action.equalsIgnoreCase("delstation") && args.length >= 4) {
                dataManager.deleteStation(args[2], args[3]);
                player.sendMessage("§a路線「" + args[2] + "」から駅「" + args[3] + "」を削除しました。");
                return true;
            }

            // 6. 駅名変更 (renstation)
            if (action.equalsIgnoreCase("renstation") && args.length >= 5) {
                dataManager.renameStation(args[2], args[3], args[4]);
                player.sendMessage("§a駅「" + args[3] + "」の名前を「" + args[4] + "」に変更しました。");
                return true;
            }
        }

        player.sendMessage("§c不明なコマンドです。");
        return true;
    }
}