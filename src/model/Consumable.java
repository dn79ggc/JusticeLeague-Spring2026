package model;

public class Consumable extends Item {
    private final int hpEffect;
    private final int defEffect;
    private final int defDuration;
    private final int hpPenalty;

    public Consumable(String name, int hpEffect, int defEffect, int defDuration, int hpPenalty) {
        super(name, "Consumable", "Common", "A consumable item that can be used once.", "Provides effects like healing or buffs", "May have negative effects");
        this.hpEffect = hpEffect;
        this.defEffect = defEffect;
        this.defDuration = defDuration;
        this.hpPenalty = hpPenalty;
    }

    public int getHpEffect() {
        return hpEffect;
    }

    public int getDefEffect() {
        return defEffect;
    }

    public int getDefDuration() {
        return defDuration;
    }

    public int getHpPenalty() {
        return hpPenalty;
    }

    @Override
    public void use(Player player) {
        if (player == null) {
            return;
        }
        if (hpEffect > 0) {
            player.heal(hpEffect);
        }
        if (hpPenalty > 0) {
            player.takeDamage(hpPenalty);
        }
        if (defEffect > 0 && defDuration > 0) {
            player.addStatusEffect(new StatusEffect(EffectType.TEMP_DEF, defEffect, defDuration));
        }
    }
}
