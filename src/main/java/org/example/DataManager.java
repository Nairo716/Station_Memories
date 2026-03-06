package org.example;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DataManager {
    private final Main plugin;
    private File file;
    private FileConfiguration config;

    // プレイヤーデータ用の変数を追加
    private File playerFile;
    private FileConfiguration playerConfig;

    public DataManager(Main plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        // --- lines.yml の設定 ---
        file = new File(plugin.getDataFolder(), "lines.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        // --- players.yml の設定 (追加分) ---
        playerFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playerFile.exists()) {
            try { playerFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        playerConfig = YamlConfiguration.loadConfiguration(playerFile);
    }

    public Set<String> getLineNames() { return config.getKeys(false); }

    public void addLine(String lineName) {
        config.set(lineName + ".stations", new ArrayList<String>());
        saveLines();
    }

    public void addStation(String lineName, String stationName, Location loc) {
        List<String> stations = config.getStringList(lineName + ".stations");
        if (!stations.contains(stationName)) {
            stations.add(stationName);
            config.set(lineName + ".stations", stations);
        }
        String path = "station_data." + stationName;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        saveLines();
    }

    public List<String> getStations(String lineName) {
        return config.getStringList(lineName + ".stations");
    }

    public Location getStationLocation(String stationName) {
        String path = "station_data." + stationName;
        if (!config.contains(path)) return null;
        return new Location(
                Bukkit.getWorld(config.getString(path + ".world")),
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z")
        );
    }

    // --- プレイヤー履歴管理用メソッド (追加分) ---

    /**
     * チェックイン回数を加算する
     */
    public void addVisitHistory(UUID uuid, String stationName) {
        String path = uuid.toString() + "." + stationName;
        // 現在の回数を取得し、1足して保存
        int count = playerConfig.getInt(path, 0);
        playerConfig.set(path, count + 1);
        savePlayers();
    }

    /**
     * 指定した駅の訪問回数を取得する
     */
    public int getVisitCount(UUID uuid, String stationName) {
        String path = uuid.toString() + "." + stationName;
        return playerConfig.getInt(path, 0);
    }
    /**
     * 訪問済みかどうかを確認する
     */
    public boolean hasVisited(UUID uuid, String stationName) {
        List<String> visited = playerConfig.getStringList(uuid.toString());
        if (visited == null) return false;
        return visited.contains(stationName);
    }

    public void saveLines() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * プレイヤーデータをファイルに保存する
     */
    public void savePlayers() {
        try { playerConfig.save(playerFile); } catch (IOException e) { e.printStackTrace(); }
    }
    /**
     * 路線を削除する
     */
    public void deleteLine(String lineName) {
        config.set(lineName, null);
        saveLines();
    }

    /**
     * 路線名を変更する
     */
    public void renameLine(String oldName, String newName) {
        if (config.contains(oldName)) {
            Object data = config.get(oldName);
            config.set(newName, data); // 新しい名前にデータをコピー
            config.set(oldName, null); // 古い名前を削除
            saveLines();
        }
    }

    /**
     * 駅を削除する
     */
    public void deleteStation(String lineName, String stationName) {
        List<String> stations = config.getStringList(lineName + ".stations");
        if (stations != null && stations.remove(stationName)) {
            config.set(lineName + ".stations", stations);
            // 駅の座標データも削除したい場合は以下も追加
            config.set("station_data." + stationName, null);
            saveLines();
        }
    }

    /**
     * 駅名を変更する
     */
    public void renameStation(String lineName, String oldName, String newName) {
        List<String> stations = config.getStringList(lineName + ".stations");
        if (stations != null && stations.contains(oldName)) {
            // リスト内の名前を更新
            stations.remove(oldName);
            stations.add(newName);
            config.set(lineName + ".stations", stations);

            // 座標データを移動
            if (config.contains("station_data." + oldName)) {
                Object locData = config.get("station_data." + oldName);
                config.set("station_data." + newName, locData);
                config.set("station_data." + oldName, null);
            }
            saveLines();
        }
    }
    /**
     * プレイヤーの現在編成中のキャラ名を取得
     */
    public String getActiveDenko(UUID uuid) {
        return playerConfig.getString(uuid.toString() + ".active_denko", "なし");
    }

    /**
     * 編成キャラを設定
     */
    public void setActiveDenko(UUID uuid, String denkoName) {
        playerConfig.set(uuid.toString() + ".active_denko", denkoName);
        savePlayers();
    }

    /**
     * 所持キャラリストを取得
     */
    public List<String> getOwnedDenkos(UUID uuid) {
        List<String> denkos = playerConfig.getStringList(uuid.toString() + ".owned_denkos");
        if (denkos == null) denkos = new ArrayList<String>();
        return denkos;
    }
    // 駅にリンクしているプレイヤーのUUIDを保存
    public void setStationOwner(String stationName, java.util.UUID uuid) {
        config.set("station_data." + stationName + ".owner", uuid.toString());
        saveLines();
    }

    // 現在のオーナーを取得
    public String getStationOwner(String stationName) {
        return config.getString("station_data." + stationName + ".owner", null);
    }
    /**
     * プレイヤーに初期キャラを付与し、編成する (救済措置)
     */
    public void initDefaultDenko(UUID uuid) {
        String current = getActiveDenko(uuid);
        // もし現在「なし」なら、初期キャラを設定
        if (current == null || current.equals("なし")) {
            setActiveDenko(uuid, "ダイヤ剣士");

            // 所持リストにも追加
            List<String> owned = getOwnedDenkos(uuid);
            if (!owned.contains("ダイヤ剣士")) {
                owned.add("ダイヤ剣士");
                playerConfig.set(uuid.toString() + ".owned_denkos", owned);
                savePlayers();
            }
        }
    }
    /**
     * ガチャチケットの所持数を取得
     */
    public int getGachaTickets(UUID uuid) {
        return playerConfig.getInt(uuid.toString() + ".gacha_tickets", 0);
    }

    /**
     * ガチャチケットを増減させる
     */
    public void addGachaTickets(UUID uuid, int amount) {
        int current = getGachaTickets(uuid);
        playerConfig.set(uuid.toString() + ".gacha_tickets", current + amount);
        savePlayers();
    }

    /**
     * 累計チェックイン回数を取得
     */
    public int getTotalCheckinCount(UUID uuid) {
        return playerConfig.getInt(uuid.toString() + ".total_checkins", 0);
    }

    /**
     * 累計チェックイン回数を加算し、報酬の判定を行う
     */
    public void incrementTotalCheckin(Player player) {
        UUID uuid = player.getUniqueId();
        int currentTotal = getTotalCheckinCount(uuid) + 1;
        playerConfig.set(uuid.toString() + ".total_checkins", currentTotal);

        // 初回・累計報酬の判定
        if (currentTotal == 1) {
            addGachaTickets(uuid, 3);
            player.sendMessage("§b§l[報酬] §f初チェックイン特典！ガチャ券を §e3枚 §f獲得しました！");
        } else if (currentTotal == 5) {
            addGachaTickets(uuid, 1);
            player.sendMessage("§b§l[報酬] §f累計5回チェックイン！ガチャ券を §e1枚 §f獲得しました！");
        } else if (currentTotal == 10) {
            addGachaTickets(uuid, 1);
            player.sendMessage("§b§l[報酬] §f累計10回チェックイン！ガチャ券を §e1枚 §f獲得しました！");
        }

        savePlayers();
    }
}