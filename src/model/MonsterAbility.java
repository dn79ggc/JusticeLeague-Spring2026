package model;

/**
 * Defines special actions a monster can perform during combat.
 * 
 * @author Joseph Nguyen
 */
public class MonsterAbility {
    private final String name;
    private final double damagePercent;
    private final double hitChance;
    private final EffectType statusEffect;
    private final double effectChance;

    public MonsterAbility(String name, double damagePercent, double hitChance,
            EffectType statusEffect, double effectChance) {
        this.name = name;
        this.damagePercent = damagePercent;
        this.hitChance = hitChance;
        this.statusEffect = statusEffect;
        this.effectChance = effectChance;
    }

    public String getName() {
        return name;
    }

    public double getDamagePercent() {
        return damagePercent;
    }

    public double getHitChance() {
        return hitChance;
    }

    public EffectType getStatusEffect() {
        return statusEffect;
    }

    public double getEffectChance() {
        return effectChance;
    }
}
