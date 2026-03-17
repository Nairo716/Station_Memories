package org.example;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EkimemoGUI {

    // メインメニュー (GUI名: ekimemo)
    public static void openMainMenu(Player player, DataManager dataManager) {
        Inventory gui = Bukkit.createInventory(null, 9, "ekimemo");

        // 1. 駅にチェックイン (コンパス)
        ItemStack checkin = new ItemStack(Material.COMPASS);
        ItemMeta m1 = checkin.getItemMeta();
        m1.setDisplayName("§a§l駅にチェックイン");
        checkin.setItemMeta(m1);

        // 2. 駅を見る (地図)
        ItemStack view = new ItemStack(Material.EMPTY_MAP);
        ItemMeta m2 = view.getItemMeta();
        m2.setDisplayName("§b§l駅を見る");
        view.setItemMeta(m2);

        // 3. キャラ編成 (チェスト) - 現在のパートナーを表示
        ItemStack formation = new ItemStack(Material.CHEST);
        ItemMeta m3 = formation.getItemMeta();
        String active = dataManager.getActiveDenko(player.getUniqueId());
        m3.setDisplayName("§d§lキャラ編成");
        List<String> lore3 = new ArrayList<String>();
        lore3.add("§7現在のパートナー: §e" + active);
        m3.setLore(lore3);
        formation.setItemMeta(m3);

        // 4. キャラ一覧 (本)
        ItemStack denkoList = new ItemStack(Material.BOOK);
        ItemMeta m4 = denkoList.getItemMeta();
        m4.setDisplayName("§6§lキャラ一覧");
        denkoList.setItemMeta(m4);

        // スロット配置 (0~8)
        gui.setItem(1, checkin);
        gui.setItem(3, view);
        gui.setItem(5, formation);
        gui.setItem(7, denkoList);

        player.openInventory(gui);
    }

    // 路線一覧 (旧openLineMenuのタイトル変更)
    public static void openLineMenu(Player player, DataManager dataManager) {
        Inventory gui = Bukkit.createInventory(null, 54, "§1路線一覧");
        Set<String> lines = dataManager.getLineNames();

        int slot = 0;
        // 1. ループの中では「路線の紙」を並べるだけにする
        for (String lineName : lines) {
            if (slot >= 53) break; // 53スロット目は「戻るボタン」用に空けておく
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + lineName);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }

        // 2. ループが終わった後に「戻るボタン」を1回だけ置く
        gui.setItem(53, getBackBtn());

        // 3. 最後に1回だけインベントリを開く
        player.openInventory(gui);
    }

    public static void openStationMenu(Player player, String lineName, DataManager dataManager) {
        Inventory gui = Bukkit.createInventory(null, 54, "§1駅一覧: " + lineName);
        List<String> stations = dataManager.getStations(lineName);
        int slot = 0;

        for (String stationName : stations) {
            // ★ 53番スロットを「戻るボタン」用に空けるため、53以上の時はループを抜ける
            if (slot >= 53) break;

            int count = dataManager.getVisitCount(player.getUniqueId(), stationName);
            ItemStack item;
            if (count > 0) {
                item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 13);
            } else {
                item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
            }

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((count > 0 ? "§a" : "§c") + stationName);
            List<String> lore = new java.util.ArrayList<String>();
            lore.add("§7アクセス回数: §e" + count + "回");
            meta.setLore(lore);
            item.setItemMeta(meta);

            gui.setItem(slot, item);
            slot++;
        }

        // ★ ループの外で、一番右下のスロット(53)に戻るボタンを配置
        gui.setItem(53, getBackBtn());

        player.openInventory(gui);
    }

    public static void openDenkoList(Player player, DataManager dataManager) {
        Inventory gui = Bukkit.createInventory(null, 27, "§1キャラ一覧");
        List<String> ownedDenkos = dataManager.getOwnedDenkos(player.getUniqueId());

        for (String denkoName : ownedDenkos) {
            if (gui.firstEmpty() >= 26) break;
            DenkoType type = DenkoType.getByName(denkoName);
            if (type == null) continue;

            ItemStack item = new ItemStack(type.getMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§6" + denkoName);
            List<String> lore = new ArrayList<String>();
            lore.add("§eクリックで詳細を確認");
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.addItem(item);
        }
        gui.setItem(26, getBackBtn());
        player.openInventory(gui);
    }

    //キャラ詳細画面
    public static void openDenkoDetail(Player player, String denkoName, DataManager dataManager) {
        Inventory gui = Bukkit.createInventory(null, 27, "§1詳細: " + denkoName);
        DenkoType type = DenkoType.getByName(denkoName);
        if (type == null) return;

        int lv = dataManager.getDenkoLevel(player.getUniqueId(), denkoName);

        // キャラ情報アイコン (中央)
        ItemStack info = new ItemStack(type.getMaterial());
        ItemMeta m = info.getItemMeta();
        m.setDisplayName("§6§l" + denkoName);
        List<String> lore = new ArrayList<String>();
        lore.add("§eLv: " + lv + " §8/ 50");
        lore.add("§aHP: " + type.getMaxHp(lv));
        lore.add("§cATK: " + type.getAtk(lv));

        if (type.isSkillUnlocked(lv)) {
            lore.add("§bスキル: " + type.getSkillName());
        } else {
            lore.add("§bスキル: §7§kXXXXX §r§8(Lv.5で解禁)");
        }
        m.setLore(lore);
        info.setItemMeta(m);
        gui.setItem(13, info);

        // 編成ボタン (下中央)
        ItemStack select = new ItemStack(Material.BEACON);
        ItemMeta sm = select.getItemMeta();
        sm.setDisplayName("§d§lこのキャラをパートナーにする");
        select.setItemMeta(sm);
        gui.setItem(22, select);

        gui.setItem(18, getBackBtn());
        player.openInventory(gui);
    }

    // --- ユーティリティ ---
    private static ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getBackBtn() {
        // 1.7.10/1.8系では Material.SKULL_ITEM, データ値 3 がプレイヤーの頭です
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // SkullOwner を設定
        meta.setOwner("MHF_ArrowLeft");
        meta.setDisplayName("§7[ 戻る ]");

        item.setItemMeta(meta);
        return item;
    }

    public static void openGachaMenu(Player player, DataManager dataManager) {
        Inventory gui = Bukkit.createInventory(null, 9, "§1ガチャ（スカウト）");

        int tickets = dataManager.getGachaTickets(player.getUniqueId());

        ItemStack gachaItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = gachaItem.getItemMeta();
        meta.setDisplayName("§e§lガチャを回す");
        List<String> lore = new ArrayList<String>();
        lore.add("§7所持チケット: §b" + tickets + "枚");
        lore.add("");
        if (tickets > 0) {
            lore.add("§aクリックで1回まわす！");
        } else {
            lore.add("§cチケットが足りません");
        }
        meta.setLore(lore);
        gachaItem.setItemMeta(meta);

        gui.setItem(4, gachaItem);
        player.openInventory(gui);
    }

    public static void openBattleGui(Player player, String stationName, String ownerName, String ownerUuid,
                                     DataManager dataManager, int enemyCurrentHp){        Inventory gui = Bukkit.createInventory(null, 27, "§cBattle: " + stationName);

        // 自分のデータ
        String myActive = dataManager.getActiveDenko(player.getUniqueId());
        DenkoType myType = DenkoType.getByName(myActive);
        int myLv = dataManager.getDenkoLevel(player.getUniqueId(), myActive);

        // 敵のデータ
        String enemyActive = dataManager.getActiveDenko(UUID.fromString(ownerUuid));
        DenkoType enemyType = DenkoType.getByName(enemyActive);
        int enemyLv = dataManager.getDenkoLevel(UUID.fromString(ownerUuid), enemyActive);

        //現在のHPを取得
        int enemyMaxHp = enemyType.getMaxHp(enemyLv);

        // --- 右側：自分 ---
        gui.setItem(16, getPlayerHead(player.getName(), "§bあなた: " + player.getName()));
        ItemStack myItem = new ItemStack(myType.getMaterial());
        ItemMeta myMeta = myItem.getItemMeta();
        myMeta.setDisplayName("§b味方: " + myActive);
        myMeta.setLore(java.util.Arrays.asList("§7Lv: " + myLv, "§cATK: " + myType.getAtk(myLv)));
        myItem.setItemMeta(myMeta);
        gui.setItem(15, myItem);

        // --- 左側：敵 ---
        gui.setItem(10, getPlayerHead(ownerName, "§c防衛者: " + ownerName));
        ItemStack enemyItem = new ItemStack(enemyType.getMaterial());
        ItemMeta enemyMeta = enemyItem.getItemMeta();
        enemyMeta.setDisplayName("§c敵: " + enemyActive);

        //HP表示を「計算後」の状態にする
        String hpStatus = (enemyCurrentHp <= 0) ? "§7§m" + enemyCurrentHp : "§a" + enemyCurrentHp;
        enemyMeta.setLore(java.util.Arrays.asList("§7Lv: " + enemyLv, "§a残りHP: " + hpStatus + " / " + enemyMaxHp));
        enemyItem.setItemMeta(enemyMeta);
        gui.setItem(11, enemyItem);

        // --- 中央：リザルト表示 (1.7.10対応) ---
        // 剣ではなく、バトルの結果（成功/失敗）を表示するアイテムに変更
        if (enemyCurrentHp <= 0) {
            // 5番は緑色 (Lime)
            gui.setItem(13, createItem(Material.STAINED_GLASS_PANE, "§a§l制圧完了！", (short) 5));
        } else {
            // 14番は赤色 (Red)
            gui.setItem(13, createItem(Material.STAINED_GLASS_PANE, "§c§l制圧失敗...", (short) 14));
        }

        player.openInventory(gui);
    }

    // 補助メソッド：1.7.10用のアイテム作成
    private static ItemStack createItem(Material m, String name, short data) {
        ItemStack item = new ItemStack(m, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    // 頭を取得する補助メソッド
    private static ItemStack getPlayerHead(String name, String displayName) {
        // 1.7.10/1.8系: Material.SKULL_ITEM, 数量1, データ値3 (Player Head)
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta m = (SkullMeta) head.getItemMeta();
        m.setOwner(name);
        m.setDisplayName(displayName);
        head.setItemMeta(m);
        return head;
    }
}