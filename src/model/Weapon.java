package model;

import java.util.Random;

public class Weapon extends Item {
    private static final Random RNG = new Random();

    private final int damage;
    private final String weaponType;
    private final int missChance;
    private final String specialEffect;

    public Weapon(String name, int damage) {
        this(name, damage, "Melee", 0, "None");
    }

    public Weapon(String name, int damage, String weaponType, int missChance, String specialEffect) {
        super(name, "Weapon", "Common", "A weapon used in combat.", "+" + damage + " attack", "May miss");
        this.damage = damage;
        this.weaponType = weaponType == null ? "melee" : weaponType.toLowerCase();
        this.missChance = Math.max(0, missChance);
        this.specialEffect = specialEffect == null ? "None" : specialEffect;
    }

    public int getDamage() {
        return damage;
    }

    public int getAtkBonus() {
        return damage;
    }

    public String getWeaponType() {
        return weaponType;
    }

    public boolean isRanged() {
        return "ranged".equalsIgnoreCase(weaponType);
    }

    public boolean didMiss() {
        return RNG.nextInt(100) < missChance;
    }

    public int getMissChance() {
        return missChance;
    }

    public String getSpecialEffect() {
        return specialEffect;
    }

    @Override
    public String getInfo() {
        return super.getInfo()
                + "\nDamage: " + damage
                + "\nWeapon Type: " + weaponType
                + "\nMiss Chance: " + missChance + "%"
                + "\nSpecial Effect: " + specialEffect;
    }
}