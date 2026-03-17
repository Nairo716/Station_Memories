package org.example;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private DataManager dataManager;
    private EkimemoListener ekimemoListener;

    @Override
    public void onEnable() {
// 1. フォルダ作成
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // 2. データマネージャー初期化
        this.dataManager = new DataManager(this);
        this.dataManager.setup();


        this.ekimemoListener = new EkimemoListener(dataManager);
        getServer().getPluginManager().registerEvents(this.ekimemoListener, this);
        EkimemoCommand cmdExecutor = new EkimemoCommand(this, dataManager, this.ekimemoListener);
        if (getCommand("ekimemo") != null) {
            getCommand("ekimemo").setExecutor(cmdExecutor);
            getCommand("gacha").setExecutor(cmdExecutor);
            getCommand("tablet").setExecutor(cmdExecutor);
            if (getCommand("checkin") != null) {
                getCommand("checkin").setExecutor(cmdExecutor);
            }
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
    public EkimemoListener getListener() {
        return this.ekimemoListener;
    }
}