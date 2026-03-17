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

        // 判定対象のGUI名リスト
        if (!title.equals("ekimemo") && !title.equals("§1路線一覧") &&
                !title.startsWith("§1駅一覧:") && !title.equals("§1キャラ一覧") &&
                !title.startsWith("§1詳細: ") &&  !title.equals("§1ガチャ（スカウト）") &&
                !title.startsWith("§cBattle: ")){
            return;
        }

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String displayName = clickedItem.getItemMeta().getDisplayName();

        // ★★★ 最優先：戻るボタンの判定を追加 ★★★
        if (displayName.equals("§7[ 戻る ]")) {
            if (title.equals("§1路線一覧") || title.equals("§1キャラ一覧") || title.equals("§1ガチャ（スカウト）")) {
                EkimemoGUI.openMainMenu(player, dataManager);
            } else if (title.startsWith("§1詳細: ")) {
                EkimemoGUI.openDenkoList(player, dataManager);
            } else if (title.startsWith("§1駅一覧:")) {
                EkimemoGUI.openLineMenu(player, dataManager);
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.CLICK, 1, 1);
            return; // ここで処理を終わらせることで、下のキャラ編成や路線判定に行かせない
        }

        // --- 1. メインメニュー (ekimemo) の判定 ---
        if (title.equals("ekimemo")) {
            if (displayName.contains("駅にチェックイン")) {
                handleCheckin(player);
            } else if (displayName.contains("駅を見る")) {
                EkimemoGUI.openLineMenu(player, dataManager);
            } else if (displayName.contains("キャラ編成") || displayName.contains("キャラ一覧")) {
                EkimemoGUI.openDenkoList(player, dataManager);
            }
        }
        // --- 2. 路線一覧の判定 ---
        else if (title.equals("§1路線一覧")) {
            // ここに辿り着くのは「戻るボタン以外」が押された時だけ
            String lineName = displayName.substring(2);
            EkimemoGUI.openStationMenu(player, lineName, dataManager);
        }
//3. キャラ一覧の判定
        else if (title.equals("§1キャラ一覧")) {
            // キャラをクリックしたら「詳細画面」を開く
            String denkoName = org.bukkit.ChatColor.stripColor(displayName);
            EkimemoGUI.openDenkoDetail(player, denkoName, dataManager);
        }
        //4. 詳細画面の判定
        else if (title.startsWith("§1詳細: ")) {
            if (displayName.equals("§d§lこのキャラをパートナーにする")) {
                // タイトルから名前を抜き取る
                String denkoName = org.bukkit.ChatColor.stripColor(title).replace("詳細: ", "");
                dataManager.setActiveDenko(player.getUniqueId(), denkoName);
                player.sendMessage("§d§l[編成] §e" + denkoName + " §fをパートナーに設定しました！");
                player.playSound(player.getLocation(), org.bukkit.Sound.ORB_PICKUP, 1, 1);
                player.closeInventory();
            }
        }
        // --- 5. ガチャ画面の判定 ---
        else if (title.equals("§1ガチャ（スカウト）")) {
            if (displayName.contains("ガチャを回す")) {
                // ... (既存のガチャ処理) ...
                int tickets = dataManager.getGachaTickets(player.getUniqueId());
                if (tickets <= 0) {
                    player.sendMessage("§cガチャチケットが足りません！");
                    player.playSound(player.getLocation(), org.bukkit.Sound.VILLAGER_NO, 1, 1);
                    return;
                }
                dataManager.addGachaTickets(player.getUniqueId(), -1);
                DenkoType[] types = DenkoType.values();
                DenkoType result = types[new java.util.Random().nextInt(types.length)];
                dataManager.addOwnedDenko(player.getUniqueId(), result.getName());
                player.sendMessage("§e§l[ガチャ] §a§l[当選!] §f「§b" + result.getName() + "§f」を仲間にしました！");
                player.playSound(player.getLocation(), org.bukkit.Sound.LEVEL_UP, 1, 1.2f);
                EkimemoGUI.openGachaMenu(player, dataManager);
            }
        }
    }
    public void handleCheckin(Player player) {
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
        // 2. クールタイムチェック
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

        // 3. 最寄り駅確定「後」にオーナー情報とアクティブキャラを取得
        String ownerUUIDStr = dataManager.getStationOwner(nearestStation);
        String activeDenko = dataManager.getActiveDenko(player.getUniqueId());
        //EXP付与
        if (!activeDenko.equals("なし")) {
            // 1回のチェックインで 50 EXP 獲得させる例
            dataManager.addDenkoExp(player.getUniqueId(), activeDenko, 50);
            player.sendMessage("§a" + activeDenko + " が 50 EXP 獲得しました！");
        }
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

        // 4. 戦闘・リンク判定ロジック
        if (ownerUUIDStr == null) {
            // 無人駅
            dataManager.setStationOwner(nearestStation, player.getUniqueId());
            player.sendMessage("§a§l[リンク] §e" + nearestStation + " §fを占有しました！");
            // 無人駅占有時は経験値を与える（必要なければ削除してください）
            dataManager.addDenkoExp(player.getUniqueId(), activeDenko, 50);
        } else if (ownerUUIDStr.equals(player.getUniqueId().toString())) {
            // 自分の駅 (リンク継続)
            player.sendMessage("§a§l[リンク継続] §e" + nearestStation + " §fの占有を維持しました。");
            // ここで addDenkoExp を呼ばないことで、経験値が入らないようにします
        } else {
            // 他人の駅判定
            String ownerUuidStr = dataManager.getStationOwner(nearestStation);
            String ownerName = org.bukkit.Bukkit.getOfflinePlayer(UUID.fromString(ownerUuidStr)).getName();

            // ダメージ計算
            String myActive = dataManager.getActiveDenko(player.getUniqueId());
            int myLv = dataManager.getDenkoLevel(player.getUniqueId(), myActive);
            int myAtk = DenkoType.getByName(myActive).getAtk(myLv);

            int currentHp = dataManager.getStationCurrentHp(nearestStation, ownerUuidStr);
            int newHp = currentHp - myAtk;

            // --- 共通の履歴保存（ここで先にやっておくと楽です） ---
            playerCooldowns.put(nearestStation, now);
            dataManager.addVisitHistory(player.getUniqueId(), nearestStation);
            dataManager.incrementTotalCheckin(player);

            if (newHp <= 0) {
                // 【パターンA：撃破成功】

                // ★先にGUI表示（まだ敵の状態が残ってるうちに！）
                EkimemoGUI.openBattleGui(player, nearestStation, ownerName, ownerUuidStr, dataManager, newHp);
                UUID oldOwnerUuid = UUID.fromString(ownerUuidStr);

                dataManager.clearAllStationLinks(oldOwnerUuid); // 相手の全駅解除
                dataManager.setStationOwner(nearestStation, player.getUniqueId());
                dataManager.setStationCurrentHp(nearestStation, 0); // ← -1やめる
                dataManager.addDenkoExp(player.getUniqueId(), myActive, 100);

                player.sendMessage("§a§l撃破！ §e" + nearestStation + " §fを制圧しました！");

                Player target = org.bukkit.Bukkit.getPlayer(oldOwnerUuid);
                if (target != null) {
                    target.sendMessage("§c§l[リンク全解除] §fでんこが撃破され、全駅のリンクが解除されました！");
                }

                return;

            } else {
                // パターンB：削っただけ（奪取失敗）
                dataManager.setStationCurrentHp(nearestStation, newHp);
                player.sendMessage("§e" + myAtk + " §7ダメージ与えましたが、守備を突破できませんでした。");

                Player target = org.bukkit.Bukkit.getPlayer(UUID.fromString(ownerUuidStr));
                if (target != null) {
                    target.sendMessage("§e§l[防衛警告] §f管理中の §e" + nearestStation + " §fが攻撃を受けました！");
                }

                // 相手の情報でGUIを開く
                EkimemoGUI.openBattleGui(player, nearestStation, ownerName, ownerUuidStr, dataManager, newHp);
                return;
            }
        }
        // --- 無人駅や自分の駅の場合のみここに来る ---
        playerCooldowns.put(nearestStation, now);
        dataManager.addVisitHistory(player.getUniqueId(), nearestStation);
        dataManager.incrementTotalCheckin(player);

        player.closeInventory(); // ここは通常メニューを閉じるため
    }
    private void executeAttack(Player player, String stationName) {
        String ownerUuidStr = dataManager.getStationOwner(stationName);
        if (ownerUuidStr == null) return;

        // 自分のパートナー情報とATK
        String myActive = dataManager.getActiveDenko(player.getUniqueId());
        int myLv = dataManager.getDenkoLevel(player.getUniqueId(), myActive);
        int myAtk = DenkoType.getByName(myActive).getAtk(myLv);

        // 相手の現在の残りHP
        int currentHp = dataManager.getStationCurrentHp(stationName, ownerUuidStr);
        int newHp = currentHp - myAtk;

        if (newHp <= 0) {
            // --- 奪還成功時の処理 ---
            dataManager.setStationOwner(stationName, player.getUniqueId());
            dataManager.setStationCurrentHp(stationName, -1); // HPをリセット（次回アクセス時にフル回復させるため）

            player.sendMessage("§a§l撃破！ §e" + stationName + " §fのリンクを解除し、占有しました！");
            player.playSound(player.getLocation(), org.bukkit.Sound.LEVEL_UP, 1, 1);

            // 経験値付与
            dataManager.addDenkoExp(player.getUniqueId(), myActive, 100);

            // 前のオーナー（敵）へ通知
            Player target = org.bukkit.Bukkit.getPlayer(UUID.fromString(ownerUuidStr));
            if (target != null) {
                target.sendMessage("§c§l[通知] §e" + stationName + " §fのリンクが解除されました。");
            }
            player.closeInventory();

        } else {
            // --- ダメージのみの処理 ---
            dataManager.setStationCurrentHp(stationName, newHp);
            player.sendMessage("§c" + myAtk + " §fのダメージを与えた！ (残りHP: " + newHp + ")");

            // 1.7.10ではGUIを更新するために再描画
            String ownerName = org.bukkit.Bukkit.getOfflinePlayer(UUID.fromString(ownerUuidStr)).getName();
            EkimemoGUI.openBattleGui(player, stationName, ownerName, ownerUuidStr, dataManager, newHp);
            // 攻撃音
            player.playSound(player.getLocation(), org.bukkit.Sound.IRONGOLEM_HIT, 1, 1);
        }
    }
    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        // アイテムを持っていない場合は無視
        if (item == null || item.getTypeId() != 265) return;

        // 右クリック（空気クリックまたはブロッククリック）の判定
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
                event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {

            // --- ここで実行 ---
            // 1. 直接メニューを開く場合（おすすめ）
            EkimemoGUI.openMainMenu(player, dataManager);

            // 2. もしコマンドとして実行させたい場合（既存の権限等を通したい時）
            // player.performCommand("ekimemo");
            // 1.7.10特有の「ブロックを置いてしまう」などの挙動を防ぐ
            event.setCancelled(true);
        }
    }
}