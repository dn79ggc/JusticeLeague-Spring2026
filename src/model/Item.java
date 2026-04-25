package model;

/**
 * Base class for all item types (weapons, armor, consumables, key items).
 * 
 * @author Jerry Kabwende
 */
public class Item {
    private String name;
    private String itemType;
    private String rarity;
    private String description;
    private String benefit;
    private String weakness;
    // Optional runtime marker for the base ItemID this instance was created from
    private String originalItemId;

    public Item() {
        this.name = "Item";
        this.itemType = "Unknown";
        this.rarity = "Common";
        this.description = "No description.";
        this.benefit = "None";
        this.weakness = "None";
    }

    public Item(String name) {
        this.name = name;
        this.itemType = "Unknown";
        this.rarity = "Common";
        this.description = "No description.";
        this.benefit = "None";
        this.weakness = "None";
    }

    public Item(String name, String itemType) {
        this(name, itemType, "Common", "No description.", "None", "None");
    }

    public Item(String name, String itemType, String rarity, String description, String benefit, String weakness) {
        this.name = name;
        this.itemType = itemType;
        this.rarity = rarity;
        this.description = description;
        this.benefit = benefit;
        this.weakness = weakness;
    }

    public String getName() {
        return name;
    }

    public String getItemType() {
        return itemType;
    }

    public String getRarity() {
        return rarity;
    }

    public String getDescription() {
        return description;
    }

    public String getBenefit() {
        return benefit;
    }

    public String getWeakness() {
        return weakness;
    }

    public String getInfo() {
        return "Name: " + name
                + "\nType: " + itemType
                + "\nRarity: " + rarity
                + "\nDescription: " + description
                + "\nBenefit: " + benefit
                + "\nWeakness: " + weakness;
    }

    public void use(Player player) {
        // Default item does nothing
    }

    public String getOriginalItemId() {
        return originalItemId;
    }

    public void setOriginalItemId(String id) {
        this.originalItemId = id;
    }
}