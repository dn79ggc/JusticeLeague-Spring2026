package model;

public class Armor extends Item {
    private int defense;

    public Armor(String name, int defense) {
        super(name);
        this.defense = defense;
    }

    public int getDefense() {
        return defense;
    }

    public int getDefenseBonus() {
        return defense;
    }
}
