package org.example;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.UUID;

public class EkimemoListener implements Listener {
    private final DataManager dataManager;
    private final HashMap<UUID, HashMap<String, Long>> cooldowns = new HashMap<>();

    public EkimemoListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;
        String title = event.getInventory().getTitle();

        // 判定対象のGUI名リストに「§1キャラ一覧」を追加
        if (!title.equals("ekimemo") && !title.equals("§1路線一覧") &&
                !title.startsWith("§1駅一覧:") && !title.equals("§1キャラ一覧")) {
            return;
        }

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String displayName = clickedItem.getItemMeta().getDisplayName();

        // --- 1. メインメニュー (ekimemo) の判定 ---
        if (title.equals("ekimemo")) {
            if (displayName.contains("駅にチェックイン")) {
                handleCheckin(player);
            } else if (displayName.contains("駅を見る")) {
                EkimemoGUI.openLineMenu(player, dataManager);
            } else if (displayName.contains("キャラ編成") || displayName.contains("キャラ一覧")) {
                // キャラ一覧GUIを開く（編成もここで行う仕様）
                EkimemoGUI.openDenkoList(player, dataManager);
            }
        }
        // --- 2. 路線一覧の判定 ---
        else if (title.equals("§1路線一覧")) {
            String lineName = displayName.substring(2);
            EkimemoGUI.openStationMenu(player, lineName, dataManager);
        }
        // --- 3. キャラ一覧（編成画面）の判定 ---
        else if (title.equals("§1キャラ一覧")) {
            // 表示名 "§6ダイヤ剣士" から "ダイヤ剣士" を抽出
            String denkoName = displayName.substring(2);

            // パートナーに設定
            dataManager.setActiveDenko(player.getUniqueId(), denkoName);
            player.sendMessage("§d§l[編成] §e" + denkoName + " §fをパートナーに設定しました！");

            // 演出: 音を鳴らしてGUIを再描画（[編成中]表示を更新するため）
            player.playSound(player.getLocation(), org.bukkit.Sound.ORB_PICKUP, 1, 1);
            EkimemoGUI.openDenkoList(player, dataManager);
        }
    }
    private void handleCheckin(Player player) {
        org.bukkit.Location playerLoc = player.getLocation();
        String nearestStation = null;
        double minDistance = Double.MAX_VALUE;

        // 1. 最寄り駅の検索ループ
        for (String lineName : dataManager.getLineNames()) {
            for (String stationName : dataManager.getStations(lineName)) {
                org.bukkit.Location stationLoc = dataManager.getStationLocation(stationName);
                if (stationLoc == null || !stationLoc.getWorld().equals(playerLoc.getWorld())) continue;

                double distance = playerLoc.distance(stationLoc);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestStation = stationName;
                }
            }
        } // ここでループが終了

        // 駅がない場合のチェック
        if (nearestStation == null) {
            player.sendMessage("§c登録されている駅が一つもありません。");
            player.closeInventory();
            return;
        }

        // 2. 最寄り駅確定「後」にオーナー情報とアクティブキャラを取得
        String ownerUUIDStr = dataManager.getStationOwner(nearestStation);
        String activeDenko = dataManager.getActiveDenko(player.getUniqueId());

        // --- 修正：キャラがいなければその場で付与する ---
        if (dataManager.getActiveDenko(player.getUniqueId()).equals("なし")) {
            dataManager.initDefaultDenko(player.getUniqueId());
            player.sendMessage("§b[システム] 初期パートナー「ダイヤ剣士」を編成しました。");
        }

        // キャラを編成していないとチェックイン不可
        if (activeDenko == null || activeDenko.equals("なし")) {
            player.sendMessage("§cキャラを編成していないため、チェックインできません！");
            player.closeInventory();
            return;
        }


        // 3. クールタイムチェック
        long now = System.currentTimeMillis();
        cooldowns.putIfAbsent(player.getUniqueId(), new HashMap<String, Long>());
        HashMap<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());

        if (playerCooldowns.containsKey(nearestStation)) {
            long lastTime = playerCooldowns.get(nearestStation);
            long diff = (now - lastTime) / 1000;
            if (diff < 300) {
                player.sendMessage("§c" + nearestStation + " にはまだチェックインできません！（残り " + (300 - diff) + "秒）");
                return;
            }
        }

        // 4. 戦闘・リンク判定ロジック
        if (ownerUUIDStr == null) {
            // 無人駅
            dataManager.setStationOwner(nearestStation, player.getUniqueId());
            player.sendMessage("§a§l[リンク] §e" + nearestStation + " §fを占有しました！");
            giveExp(player, 50);
        } else if (ownerUUIDStr.equals(player.getUniqueId().toString())) {
            // 自分の駅
            player.sendMessage("§a§l[リンク継続] §e" + nearestStation + " §fの占有を維持しました。");
            giveExp(player, 20);
        } else {
            // 他人の駅
            player.sendMessage("§c§l[攻撃] §e" + nearestStation + " §fを攻撃して奪取しました！");

            Player target = org.bukkit.Bukkit.getPlayer(UUID.fromString(ownerUUIDStr));
            if (target != null) {
                target.sendMessage("§c§l[通知] §e" + nearestStation + " §fのリンクが解除されました。");
            }
            dataManager.setStationOwner(nearestStation, player.getUniqueId());
            giveExp(player, 100);
        }

        // 履歴保存
        playerCooldowns.put(nearestStation, now);
        dataManager.addVisitHistory(player.getUniqueId(), nearestStation);
        dataManager.incrementTotalCheckin(player);

        player.closeInventory();
    }
//すぐ消す
    private void giveExp(Player player, int amount) {
        player.sendMessage("§d§l[EXP] §fパートナーが §b" + amount + " exp §f獲得しました。");
    }
}