package model;

/**
 * Represents a timed buff or debuff effect applied to a combatant.
 * 
 * @author Joseph Nguyen
 */
public class StatusEffect {
    private EffectType effectType;
    private double modifier;
    private int duration;

    public StatusEffect(EffectType effectType) {
        this(effectType, 0.0, 1);
    }

    public StatusEffect(EffectType effectType, double modifier, int duration) {
        this.effectType = effectType;
        this.modifier = modifier;
        this.duration = duration;
    }

    public EffectType getEffectType() {
        return effectType;
    }

    public double getModifier() {
        return modifier;
    }

    public int getDuration() {
        return duration;
    }

    public void tick() {
        if (duration > 0) {
            duration--;
        }
    }

    public void consumeCharge() {
        tick();
    }

    public boolean isExpired() {
        return duration == 0;
    }

    public String getName() {
        return effectType.name();
    }
}
