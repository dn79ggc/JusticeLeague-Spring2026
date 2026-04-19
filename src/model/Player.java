package model;

import java.util.List;
import java.util.ArrayList;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

public class Player {

    // Navigation & Tracking
    private String name;
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
        this("Explorer", startingLocation);
    }

    public Player(String name, int startingLocation) {
        this.name = name == null || name.isBlank() ? "Explorer" : name;
        this.location = startingLocation;
        this.roomsVisited = 1; // Player starts in a room, which counts
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

    public void setLocationNoMove(int newLocation) {
        this.location = newLocation;
    }

    public Room getPreviousRoom() {
        return previousRoom;
    }

    public boolean move(Room nextRoom) {
        if (nextRoom == null) {
            return false;
        }

        previousRoom = currentRoom;
        currentRoom = nextRoom;
        totalMoves++;

        if (!visitedRooms.contains(nextRoom.getRoomId())) {
            visitedRooms.add(nextRoom.getRoomId());
            roomsVisited++;
        }

        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCurrentHP(int currentHP) {
        this.currentHP = Math.max(0, Math.min(currentHP, this.maxHP));
    }

    public void setMaxHP(int maxHP) {
        this.maxHP = Math.max(1, maxHP);
        if (this.currentHP > this.maxHP) {
            this.currentHP = this.maxHP;
        }
    }

    public void setBaseAttack(int baseAttack) {
        this.baseAttack = baseAttack;
    }

    public void setBaseDefense(int baseDefense) {
        this.baseDefense = baseDefense;
    }

    public static Player loadFromCsv(String filePath, Game game) {
        Player player = new Player("Explorer", 1);
        if (game == null) {
            return player;
        }
        try {
            Table table = Table.read().csv(filePath);
            if (table.rowCount() == 0) {
                return player;
            }
            Row row = table.row(0);
            String playerName = row.getString("Name");
            String roomId = row.getString("CurrentRoomID");
            int currentHP = parseInt(row.getString("CurrentHP"), 100);
            int maxHP = parseInt(row.getString("MaxHP"), 100);
            int baseAttack = parseInt(row.getString("BaseAttack"), 10);
            int baseDefense = parseInt(row.getString("BaseDefense"), 5);

            int roomNumber = game.getRoomNumberById(roomId);
            if (roomNumber == 0) {
                roomNumber = 1;
            }
            Player loaded = new Player(playerName, roomNumber);
            loaded.setMaxHP(maxHP);
            loaded.setCurrentHP(currentHP);
            loaded.setBaseAttack(baseAttack);
            loaded.setBaseDefense(baseDefense);
            return loaded;
        } catch (Exception e) {
            return player;
        }
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
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

    public Item getItemByName(String name) {
        for (Item item : inventory) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
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

    public int getCurrentHP() {
        return currentHP;
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

    public int getMaxHP() {
        return maxHP;
    }

    public int getBaseAttack() {
        return baseAttack;
    }

    public int getBaseDefense() {
        return baseDefense;
    }

    public List<String> getVisitedRooms() {
        return visitedRooms;
    }

    public void addVisitedRoom(String roomId) {
        if (roomId != null && !visitedRooms.contains(roomId)) {
            visitedRooms.add(roomId);
        }
    }

    public Weapon getEquippedWeapon() {
        return equippedWeapon;
    }

    public Armor getEquippedArmor() {
        return equippedArmor;
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
