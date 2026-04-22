package model;

import java.util.Random;

public class Weapon extends Item {
    private static final Random RNG = new Random();

    private int damage;
    private String weaponType;
    private int missChance;
    private String specialEffect;

    public Weapon(String name, int damage) {
        this(name, damage, "Melee", 0, "NONE");
    }

    public Weapon(String name, int damage, String weaponType, int missChance, String specialEffect) {
        super(name);
        this.damage = damage;
        this.weaponType = weaponType == null ? "Melee" : weaponType;
        this.missChance = Math.max(0, missChance);
        this.specialEffect = specialEffect == null ? "NONE" : specialEffect;
    }

    public int getDamage() {
        return damage;
    }

    public int getAtkBonus() {
        return damage;
    }

    public String getWeaponType() {
        return weaponType.toLowerCase();
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
}
