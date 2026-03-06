package org.example;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        for (String lineName : lines) {
            if (slot >= 54) break;
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + lineName);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }
        player.openInventory(gui);
    }

    // 駅一覧
    public static void openStationMenu(Player player, String lineName, DataManager dataManager) {
        Inventory gui = Bukkit.createInventory(null, 54, "§1駅一覧: " + lineName);
        List<String> stations = dataManager.getStations(lineName);
        int slot = 0;

        for (String stationName : stations) {
            if (slot >= 54) break;

            // 訪問回数を取得
            int count = dataManager.getVisitCount(player.getUniqueId(), stationName);

            ItemStack item;
            // 1回でも行ってれば緑、行ってなければ赤
            if (count > 0) {
                item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 13); // 緑
            } else {
                item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14); // 赤
            }

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((count > 0 ? "§a" : "§c") + stationName);

            // アイテムの説明文（Lore）に回数を表示
            List<String> lore = new java.util.ArrayList<String>();
            lore.add("§7アクセス回数: §e" + count + "回");
            meta.setLore(lore);

            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }
        player.openInventory(gui);
    }
    // キャラ一覧画面を開く
    public static void openDenkoList(Player player, DataManager dataManager) {
        Inventory gui = Bukkit.createInventory(null, 27, "§1キャラ一覧");

        List<String> ownedDenkos = dataManager.getOwnedDenkos(player.getUniqueId());
        String activeDenko = dataManager.getActiveDenko(player.getUniqueId());

        for (String denkoName : ownedDenkos) {
            // 仮のアイテム設定（本来はDenkoType等から取得）
            ItemStack item;
            if (denkoName.equals("ダイヤ剣士")) {
                item = new ItemStack(Material.DIAMOND_SWORD);
            } else if (denkoName.equals("鉄の防衛者")) {
                item = new ItemStack(Material.IRON_CHESTPLATE);
            } else {
                item = new ItemStack(Material.PAPER);
            }

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§6" + denkoName);

            List<String> lore = new ArrayList<String>();
            // ステータス表示（例）
            lore.add("§7ATK: §c20");
            lore.add("§7HP: §a100");

            if (denkoName.equals(activeDenko)) {
                lore.add("");
                lore.add("§b§l[編成中]");
            } else {
                lore.add("");
                lore.add("§eクリックで編成する");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.addItem(item);
        }
        player.openInventory(gui);
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
}