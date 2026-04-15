package model;

public class Item {
    private String name;
    private String itemType;
    private String rarity;
    private String description;
    private String benefit;
    private String weakness;

    private int healAmount;
    private int attackBonus;
    private int defenseBonus;
    private int missChance;

    private boolean equippable;
    private boolean consumable;
    private String keyTarget;

    public Item() {
        this.name = "Item";
    }

    public Item(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getInfo() {
        return name;
    }
}
