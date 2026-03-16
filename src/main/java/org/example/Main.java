package org.example;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private DataManager dataManager;

    @Override
    public void onEnable() {
// 1. フォルダ作成
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // 2. データマネージャー初期化
        this.dataManager = new DataManager(this);
        this.dataManager.setup();

        getServer().getPluginManager().registerEvents(new EkimemoListener(dataManager), this);
        EkimemoCommand cmdExecutor = new EkimemoCommand(this, dataManager);
        // コマンドの登録
        if (getCommand("ekimemo") != null) {
            getCommand("ekimemo").setExecutor(new EkimemoCommand(this, dataManager));
            getCommand("gacha").setExecutor(cmdExecutor);
        } else {

            getLogger().severe("plugin.ymlにekimemoコマンドが登録されていません！");
        }


        getLogger().info("駅メモプラグインが導入されたよ！ !");
    }
    // プレイヤーが参加したときに実行
    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        dataManager.initDefaultDenko(event.getPlayer().getUniqueId());
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveLines();
        }
    }
}