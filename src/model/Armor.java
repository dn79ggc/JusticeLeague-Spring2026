package model;

/**
 * Equippable armor item with defense bonus.
 * 
 * @author Jerry Kabwende
 */
public class Armor extends Item {
    private final int defense;

    public Armor(String name, int defense) {
        super(name, "Armor", "Common", "Protective gear.", "+" + defense + " defense", "None");
        this.defense = defense;
    }

    public int getDefense() {
        return defense;
    }

    public int getDefenseBonus() {
        return defense;
    }

    @Override
    public String getInfo() {
        return super.getInfo() + "\nDefense: " + defense;
    }
}