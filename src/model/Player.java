package model;

import java.util.List;

public class Player {
    private int location;
    private int roomsVisited;
    private int totalMoves;

    // Combat Stats
    private int currentHP;
    private int maxHP;
    private int baseAttack;
    private int baseDefense;

    // Inventory & Equipment
    private List<Item> inventory;
    private Weapon equippedWeapon;
    private Armor equippedArmor;

    // Status Effects
    private List<StatusEffect> statusEffects;

    // Constructor
    public Player(int startingLocation) {
        this.location = startingLocation;
        this.roomsVisited = 1; // Player starts in Room 1, which counts
        this.totalMoves = 0;

        // Combat defaults
        this.maxHP = 100;
        this.currentHP = maxHP;
        this.baseAttack = 10;
        this.baseDefense = 10;

        // Inventory & status effects
        this.inventory = new ArrayList<>();
        this.statusEffects = new ArrayList<>();
        this.equippedWeapon = null;
        this.equippedArmor = null;
    }

    // Movement Logic
    public int getLocation() {
        return location;
    }

    public void setLocation(int newLocation) {
        this.location = newLocation;
        this.totalMoves++;
    }

    public void incrementRoomsVisited() {
        this.roomsVisited++;
    }

    public int getRoomsVisited() {
        return roomsVisited;
    }

    public int getTotalMoves() {
        return totalMoves;
    }

    // Tries to move in the given direction.
    // Returns the destination room number on success, or -1 if no exit exists.
    public int attemptMove(Room currentRoom, int directionIndex) {
        int destination = currentRoom.getExit(directionIndex);
        if (destination > 0) {
            setLocation(destination);
            return destination;
        }
        return -1;
    }

    // Inventory
    public List<Item> getInventory() {
        return inventory;
    }

    public void addItem(Item item) {
        inventory.add(item);
    }

    public void removeItem(Item item) {
        inventory.remove(item);
    }

    // Equipment
    public void equipWeapon(Weapon weapon) {
        this.equippedWeapon = weapon;
    }

    public void equipArmor(Armor armor) {
        this.equippedArmor = armor;
    }

    // Combat Helpers
    public void takeDamage(int amount) {
        currentHP -= amount;
        if (currentHP < 0) {
            currentHP = 0;
        }
    }

    public void heal(int amount) {
        currentHP += amount;
        if (currentHP > maxHP) {
            currentHP = maxHP;
        }
    }

    public boolean isAlive() {
        return currentHP > 0;
    }

    public int getAttackValue() {
        int attack = baseAttack;
        if (equippedWWeapon != null) {
            attack += equippedWeapon.getDamage();
        }
        return attack;
    }

    public int getDefenseValue() {
        int defense = baseDefense;
        if (equippedArmor != null) {
            defense += equippedArmor.getDefense();
        }
        return defense;
    }

    // Status Effects
    public void addStatusEffect(StatusEffect effect) {
        statusEffects.add(effect);
    }

    public void clearStatusEffects() {
        statusEffects.clear();
    }

}
