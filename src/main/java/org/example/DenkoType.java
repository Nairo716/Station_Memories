package org.example;

import org.bukkit.Material;

public enum DenkoType {
    // 名前, 表示アイテム, 初期HP, 初期ATK
    DIAMOND_WARRIOR("ダイヤ剣士", Material.DIAMOND_SWORD, 100, 20),
    IRON_DEFENDER("鉄の防衛者", Material.IRON_CHESTPLATE, 150, 10),
    GOLDEN_SPEEDER("金のマッハ", Material.GOLD_BOOTS, 80, 15);

    private final String name;
    private final Material material;
    private final int baseHp;
    private final int baseAtk;

    DenkoType(String name, Material material, int baseHp, int baseAtk) {
        this.name = name;
        this.material = material;
        this.baseHp = baseHp;
        this.baseAtk = baseAtk;
    }

    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public int getBaseHp() { return baseHp; }
    public int getBaseAtk() { return baseAtk; }
}