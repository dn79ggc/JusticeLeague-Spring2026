package model;

import java.util.List;
import java.util.ArrayList;

public class Player {

    // Navigation & Tracking
    private int location;
    private int roomsVisited;
    private int totalMoves;

    private Room currentRoom;
    private Room previousRoom;

    private List<String> visitedRooms; // Visited room IDs (for Map & SaveFile)

    // Combat Stats
    private int currentHP;
    private int maxHP;
    private int baseAttack;
    private int baseDefense;

    private boolean isDefending;
    private boolean itemUsedThisTurn;

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
        this.baseDefense = 5;

        // Inventory & states
        this.inventory = new ArrayList<>();
        this.statusEffects = new ArrayList<>();
        this.visitedRooms = new ArrayList<>();

        this.equippedWeapon = null;
        this.equippedArmor = null;

        this.isDefending = false;
        this.itemUsedThisTurn = false;
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

    public boolean addToInventory(Item item) {
        if (inventory.size() >= 7) {
            return false;
        }
        inventory.add(item);
        return true;
    }

    public boolean removeItem(Item item) {
        if (item instanceof KeyItem) {
            return false;
        }
        return inventory.remove(item);
    }

    public int getInventorySlots() {
        return inventory.size();
    }

    // Equipment
    public void equipWeapon(Weapon weapon) {
        this.equippedWeapon = weapon;
    }

    public void equipArmor(Armor armor) {
        this.equippedArmor = armor;
    }

    public boolean unequipWeapon() {
        if (equippedWeapon == null) {
            return false;
        }

        if (inventory.size() >= 7) {
            return false;
        }

        inventory.add(equippedWeapon);
        equippedWeapon = null;
        return true;
    }

    public boolean unequipArmor() {
        if (equippedArmor == null) {
            return false;
        }

        if (inventory.size() >= 7) {
            return false;
        }

        inventory.add(equippedArmor);
        equippedArmor = null;
        return true;
    }

    // Combat Helpers
    public int attack() {
        return getAttackValue();
    }

    public void defend() {
        isDefending = true;
    }

    public boolean flee() {
        return false;
    }

    public void resetTurnFlags() {
        isDefending = false;
        itemUsedThisTurn = false;
    }

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
        if (equippedWeapon != null) {
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

    public boolean hasStatusEffect(EffectType type) {
        for (StatusEffect effect : statusEffects) {
            if (effect.getEffectType() == type) {
                return true;
            }
        }
        return false;
    }

    public void applyStatusEffects() {

    }

    public void clearStatusEffects() {
        statusEffects.clear();
    }

    // Puzzle Interaction
    public boolean solvePuzzle(String answer) {
        return false;
    }

    public String getHint() {
        if (currentRoom == null) {
            return "No hint available";
        }

        if (!currentRoom.hasPuzzle()) {
            return "No hint available";
        }

        Puzzle puzzle = currentRoom.getPuzzle();

        if (puzzle.hasHint()) {
            return puzzle.getHint();
        } else {
            return "No hint available";
        }
    }

    public boolean useKeyItem(KeyItem keyItem) {
        return false;
    }

    // Info for Save / UI
    public String getName() {
        return "Player";
    }

    public String getStatus() {
        return "HP: " + currentHP + "/" + maxHP +
                " | ATK: " + getAttackValue() +
                " | DEF: " + getDefenseValue();
    }

    // Command methods
    public String inspectItem(Item item) {
        if (!inventory.contains(item)) {
            return "You must pick up this item before inspecting it.";

        }
        return item.getInfo();
    }

    public String exploreRoom() {
        if (currentRoom == null) {
            return "There is nothing to explore here";
        }
        return currentRoom.getFullDescription();
    }

    public boolean pickupItem(Item item) {
        if (currentRoom == null || item == null) {
            return false;
        }

        if (!currentRoom.getItems().contains(item)) {
            return false;
        }

        if (!addToInventory(item) && !(item instanceof KeyItem)) {
            return false; // inventory full
        }

        currentRoom.removeItem(item);
        return true;
    }

    public boolean dropItem(Item item) {
        if (item == null || !inventory.contains(item)) {
            return false;
        }

        if (item instanceof KeyItem) {
            return false;
        }

        inventory.remove(item);
        currentRoom.addItem(item);
        return true;
    }

    public List<String> showInventory() {
        List<String> result = new ArrayList<>();

        if (inventory.isEmpty()) {
            result.add("Your inventory is empty.");
            return result;
        }

        for (Item item : inventory) {
            result.add(item.getName());
        }

        return result;
    }
}
