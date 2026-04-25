package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an enemy with HP, attack, defense, abilities, and status effects.
 * 
 * @author Joseph Nguyen
 */
public class Monster {
    private String monsterId;
    private String monsterClass;
    private String name;
    private String description;
    private String combatStartText;
    private String combatDeathText;
    private int level;
    private int speed;
    private int maxHp;
    private int currentHp;
    private String roomId;
    private String resistType;
    private double resistModifier;
    private String weakType;
    private double weakModifier;

    private boolean alive;
    private boolean inBuilding;
    private boolean darkenActive;
    private boolean fortifyActive;
    private boolean hasBarricaded;
    private boolean hasFled;
    private boolean summoning;
    private boolean summonActive;
    private boolean hasSummoned;
    private boolean guaranteedHit;
    private int summonTurnCount;

    private Monster summonedGhost;

    private List<MonsterAbility> abilities;
    private Map<String, Double> specialFlags;

    public Monster() {
        this("M00", "Monster", "Monster", 1, 25, 1, "NONE", "NONE", 0.0, "NONE", 0.0, "A monster.");
    }

    public Monster(String name) {
        this("M00", "Monster", name, 1, 25, 1, "NONE", "NONE", 0.0, "NONE", 0.0, "A monster.");
    }

    public Monster(String monsterId, String monsterClass, String name, int level, int maxHp,
            int speed, String roomId, String resistType, double resistModifier,
            String weakType, double weakModifier, String description) {
        this.monsterId = monsterId;
        this.monsterClass = monsterClass;
        this.name = name;
        this.level = level;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.speed = speed;
        this.roomId = roomId;
        this.resistType = resistType == null ? "NONE" : resistType;
        this.resistModifier = resistModifier;
        this.weakType = weakType == null ? "NONE" : weakType;
        this.weakModifier = weakModifier;
        this.description = description == null ? "" : description;
        this.combatStartText = "";
        this.combatDeathText = "";
        this.alive = true;
        this.abilities = new ArrayList<>();
        this.specialFlags = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public int getSpeed() {
        return speed;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public String getDescription() {
        return description;
    }

    public String getMonsterClass() {
        return monsterClass;
    }

    public String getMonsterId() {
        return monsterId;
    }

    public String getCombatStartText() {
        return combatStartText;
    }

    public void setCombatStartText(String combatStartText) {
        this.combatStartText = combatStartText == null ? "" : combatStartText;
    }

    public String getCombatDeathText() {
        return combatDeathText;
    }

    public void setCombatDeathText(String combatDeathText) {
        this.combatDeathText = combatDeathText == null ? "" : combatDeathText;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        this.roomId = roomId;
    }

    public String getResistType() {
        return resistType;
    }

    public double getResistModifier() {
        return resistModifier;
    }

    public String getWeakType() {
        return weakType;
    }

    public double getWeakModifier() {
        return weakModifier;
    }

    public void addAbility(MonsterAbility ability) {
        abilities.add(ability);
    }

    public List<MonsterAbility> getAbilities() {
        return new ArrayList<>(abilities);
    }

    public Monster createSummonedVariant() {
        Monster clone = new Monster(monsterId + "-SUM", monsterClass, name, 1, 25, speed, roomId,
                resistType, resistModifier, weakType, weakModifier, description);
        for (MonsterAbility ability : abilities) {
            clone.addAbility(new MonsterAbility(ability.getName(), ability.getDamagePercent(), ability.getHitChance(),
                    ability.getStatusEffect(), ability.getEffectChance()));
        }
        for (Map.Entry<String, Double> entry : specialFlags.entrySet()) {
            clone.setSpecialFlag(entry.getKey(), entry.getValue());
        }
        clone.setCombatStartText(combatStartText);
        clone.setCombatDeathText(combatDeathText);
        return clone;
    }

    public Monster createCombatTestCopy() {
        Monster clone = new Monster(monsterId + "-TEST", monsterClass, name, level, maxHp, speed, roomId,
                resistType, resistModifier, weakType, weakModifier, description);
        for (MonsterAbility ability : abilities) {
            clone.addAbility(new MonsterAbility(ability.getName(), ability.getDamagePercent(), ability.getHitChance(),
                    ability.getStatusEffect(), ability.getEffectChance()));
        }
        for (Map.Entry<String, Double> entry : specialFlags.entrySet()) {
            clone.setSpecialFlag(entry.getKey(), entry.getValue());
        }
        clone.setCombatStartText(combatStartText);
        clone.setCombatDeathText(combatDeathText);
        clone.reset();
        return clone;
    }

    public MonsterAbility getAbilityByName(String abilityName) {
        for (MonsterAbility ability : abilities) {
            if (ability.getName().equalsIgnoreCase(abilityName)) {
                return ability;
            }
        }
        return null;
    }

    public void setSpecialFlag(String key, double value) {
        specialFlags.put(key, value);
    }

    public double getSpecialFlag(String key) {
        return specialFlags.getOrDefault(key, 0.0);
    }

    public boolean isType(String expectedClass) {
        return monsterClass != null && monsterClass.equalsIgnoreCase(expectedClass);
    }

    public double getDamageModifier(String weaponType) {
        if (weaponType == null) {
            return 1.0;
        }
        String normalized = weaponType.toUpperCase();
        if (normalized.equalsIgnoreCase(resistType)) {
            return 1.0 + resistModifier;
        }
        if (normalized.equalsIgnoreCase(weakType)) {
            return 1.0 + weakModifier;
        }
        return 1.0;
    }

    public void takeDamage(int damage) {
        currentHp = Math.max(0, currentHp - Math.max(0, damage));
        if (currentHp == 0) {
            alive = false;
        }
    }

    public void heal(int amount) {
        currentHp = Math.min(maxHp, currentHp + Math.max(0, amount));
        if (currentHp > 0) {
            alive = true;
        }
    }

    public boolean isDefeated() {
        return currentHp <= 0 || !alive;
    }

    public void reset() {
        alive = true;
        currentHp = maxHp;
        darkenActive = false;
        fortifyActive = false;
        guaranteedHit = false;
        summonActive = false;
        summoning = false;
        summonTurnCount = 0;
        summonedGhost = null;
    }

    public boolean isAlive() {
        return alive && currentHp > 0;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
        if (!alive) {
            currentHp = 0;
        }
    }

    public String getInfo() {
        return name + " (Lv " + level + ")\n"
                + "HP: " + currentHp + " / " + maxHp + "\n"
                + description;
    }

    public boolean isInBuilding() {
        return inBuilding;
    }

    public void setIsInBuilding(boolean inBuilding) {
        this.inBuilding = inBuilding;
    }

    public boolean isDarkenActive() {
        return darkenActive;
    }

    public void setDarkenActive(boolean darkenActive) {
        this.darkenActive = darkenActive;
    }

    public boolean hasFortifyActive() {
        return fortifyActive;
    }

    public void setFortifyActive(boolean fortifyActive) {
        this.fortifyActive = fortifyActive;
    }

    public boolean hasBarricaded() {
        return hasBarricaded;
    }

    public void setHasBarricaded(boolean hasBarricaded) {
        this.hasBarricaded = hasBarricaded;
    }

    public boolean hasFled() {
        return hasFled;
    }

    public void setHasFled(boolean hasFled) {
        this.hasFled = hasFled;
    }

    public boolean isSummoning() {
        return summoning;
    }

    public void startSummonCast() {
        this.summoning = true;
        this.summonTurnCount = 0;
    }

    public void tickSummonCast() {
        this.summonTurnCount++;
    }

    public boolean isSummonReady() {
        int turnsToCast = (int) getSpecialFlag("SUMMON_TURNS_TO_CAST");
        if (turnsToCast <= 0) {
            turnsToCast = 3;
        }
        return summonTurnCount >= turnsToCast;
    }

    public boolean isSummonActive() {
        return summonActive;
    }

    public void setSummonActive(boolean summonActive) {
        this.summonActive = summonActive;
    }

    public boolean hasSummoned() {
        return hasSummoned;
    }

    public void setHasSummoned(boolean hasSummoned) {
        this.hasSummoned = hasSummoned;
    }

    public Monster getSummonedGhost() {
        return summonedGhost;
    }

    public void setSummonedGhost(Monster summonedGhost) {
        this.summonedGhost = summonedGhost;
    }

    public int getSummonTurnCount() {
        return summonTurnCount;
    }

    public void incrementSummonTurnCount() {
        this.summonTurnCount++;
    }

    public void resetSummonTurnCount() {
        this.summonTurnCount = 0;
    }

    public boolean isGuaranteedHit() {
        return guaranteedHit;
    }

    public void setGuaranteedHit(boolean guaranteedHit) {
        this.guaranteedHit = guaranteedHit;
    }
}
