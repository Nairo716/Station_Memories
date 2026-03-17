package org.example;

import org.bukkit.Material;

public enum DenkoType {
    DIAMOND_WARRIOR("ダイヤ剣士", Material.DIAMOND_SWORD, 100, 20, 5, 2, "こんじょう"),
    IRON_DEFENDER("鉄の防衛者", Material.IRON_CHESTPLATE, 150, 10, 8, 1, "かちこち"),
    GOLDEN_SPEEDER("金のマッハ", Material.GOLD_BOOTS, 80, 15, 4, 3, "しゅんそく");

    private final String name;
    private final Material material;
    private final int baseHp;
    private final int baseAtk;
    private final int hpPerLv;
    private final int atkPerLv;
    private final String skillName;

    DenkoType(String name, Material material, int baseHp, int baseAtk, int hpPerLv, int atkPerLv, String skillName) {
        this.name = name;
        this.material = material;
        this.baseHp = baseHp;
        this.baseAtk = baseAtk;
        this.hpPerLv = hpPerLv;
        this.atkPerLv = atkPerLv;
        this.skillName = skillName;
    }

    public int getMaxHp(int level) { return baseHp + (hpPerLv * (level - 1)); }
    public int getAtk(int level) { return baseAtk + (atkPerLv * (level - 1)); }
    public boolean isSkillUnlocked(int level) { return level >= 5; }

    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public String getSkillName() { return skillName; }

    public static DenkoType getByName(String name) {
        for (DenkoType type : values()) {
            if (type.getName().equals(name)) return type;
        }
        return null;
    }
}