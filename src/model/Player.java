package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

/**
 * Tracks player location, HP, inventory, equipped gear, and room history.
 * Supports CSV load/save functionality.
 * 
 * @author Thang Pham
 */
public class Player {
    private static final Random RNG = new Random();

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

    public Room getCurrentRoom() {
        return currentRoom;
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

    public void setCurrentRoom(Room room) {
        this.previousRoom = this.currentRoom;
        this.currentRoom = room;
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
            java.util.List<java.util.Map<String, String>> rows = SaveManager.readCsvAsStringMaps(filePath);
            if (rows == null || rows.isEmpty()) {
                return player;
            }
            java.util.Map<String, String> row = rows.get(0);
            String playerName = row.getOrDefault("Name", "Explorer");
            String roomId = row.getOrDefault("CurrentRoomID", "");
            int currentHP = parseInt(row.getOrDefault("CurrentHP", "100"), 100);
            int maxHP = parseInt(row.getOrDefault("MaxHP", "100"), 100);
            int baseAttack = parseInt(row.getOrDefault("BaseAttack", "10"), 10);
            int baseDefense = parseInt(row.getOrDefault("BaseDefense", "5"), 5);

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
        if (item == null) {
            return false;
        }

        // Determine if adding this item would consume a new inventory slot
        boolean alreadyPresent = false;
        for (Item i : inventory) {
            if (i.getName().equalsIgnoreCase(item.getName())) {
                alreadyPresent = true;
                break;
            }
        }

        int slotsUsed = getInventorySlots();
        if (!alreadyPresent && slotsUsed >= 7 && !(item instanceof KeyItem)) {
            return false; // would require a new slot
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
        // Count unique item names as inventory slots so stackable items
        // (e.g., health potions) occupy a single slot even if multiple
        // instances exist in the list.
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Item item : inventory) {
            if (item == null)
                continue;
            names.add(item.getName().toLowerCase());
        }
        return names.size();
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

    /**
     * Auto-equips the strongest weapon and armor from inventory.
     * Sorts by damage/defense (descending), prioritizing higher tiers.
     */
    public void autoEquipStrongest() {
        // Find strongest weapon
        Weapon strongestWeapon = null;
        int maxDamage = -1;
        for (Item item : inventory) {
            if (item instanceof Weapon) {
                Weapon w = (Weapon) item;
                if (w.getDamage() > maxDamage) {
                    maxDamage = w.getDamage();
                    strongestWeapon = w;
                }
            }
        }
        if (strongestWeapon != null) {
            equipWeapon(strongestWeapon);
        }

        // Find strongest armor
        Armor strongestArmor = null;
        int maxDefense = -1;
        for (Item item : inventory) {
            if (item instanceof Armor) {
                Armor a = (Armor) item;
                if (a.getDefense() > maxDefense) {
                    maxDefense = a.getDefense();
                    strongestArmor = a;
                }
            }
        }
        if (strongestArmor != null) {
            equipArmor(strongestArmor);
        }
    }

    public boolean unequipWeapon() {
        if (equippedWeapon == null) {
            return false;
        }

        int slotsUsed = getInventorySlots();
        boolean wouldIntroduceNew = true;
        for (Item i : inventory) {
            if (i.getName().equalsIgnoreCase(equippedWeapon.getName())) {
                wouldIntroduceNew = false;
                break;
            }
        }
        if (wouldIntroduceNew && slotsUsed >= 7) {
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

        int slotsUsed = getInventorySlots();
        boolean wouldIntroduceNew = true;
        for (Item i : inventory) {
            if (i.getName().equalsIgnoreCase(equippedArmor.getName())) {
                wouldIntroduceNew = false;
                break;
            }
        }
        if (wouldIntroduceNew && slotsUsed >= 7) {
            return false;
        }
        inventory.add(equippedArmor);
        equippedArmor = null;
        return true;
    }

    // Combat Helpers
    public int attack() {
        return getEffectiveATK();
    }

    public void defend() {
        isDefending = true;
    }

    public boolean flee() {
        return RNG.nextDouble() < 0.5;
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

    public int getMaxHP() {
        return maxHP;
    }

    public int getCurrentHp() {
        return getCurrentHP();
    }

    public int getEffectiveATK() {
        int attack = baseAttack;
        if (equippedWeapon != null) {
            attack += equippedWeapon.getDamage();
        }
        return attack;
    }

    public int getEffectiveDEF() {
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
        for (StatusEffect effect : statusEffects) {
            effect.tick();
        }
        statusEffects.removeIf(StatusEffect::isExpired);
    }

    public int getAttackValue() {
        return getEffectiveATK();
    }

    public int getDefenseValue() {
        int tempDefense = 0;
        for (StatusEffect effect : statusEffects) {
            if (effect.getEffectType() == EffectType.TEMP_DEF) {
                tempDefense += (int) effect.getModifier();
            }
        }
        return getEffectiveDEF() + tempDefense;
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

    public List<StatusEffect> getStatusEffects() {
        return statusEffects;
    }

    public StatusEffect getStatusEffect(EffectType type) {
        for (StatusEffect effect : statusEffects) {
            if (effect.getEffectType() == type) {
                return effect;
            }
        }
        return null;
    }

    public void removeStatusEffect(EffectType type) {
        statusEffects.removeIf(effect -> effect.getEffectType() == type);
    }

    public boolean isDefending() {
        return isDefending;
    }

    public void setDefending(boolean defending) {
        isDefending = defending;
    }

    public boolean isItemUsedThisTurn() {
        return itemUsedThisTurn;
    }

    public void setItemUsedThisTurn(boolean itemUsedThisTurn) {
        this.itemUsedThisTurn = itemUsedThisTurn;
    }

    public List<Weapon> getWeapons() {
        List<Weapon> weapons = new ArrayList<>();
        for (Item item : inventory) {
            if (item instanceof Weapon weapon) {
                weapons.add(weapon);
            }
        }
        return weapons;
    }

    public List<Weapon> getRangedWeapons() {
        List<Weapon> weapons = new ArrayList<>();
        for (Weapon weapon : getWeapons()) {
            if (weapon.isRanged()) {
                weapons.add(weapon);
            }
        }
        return weapons;
    }

    public List<Consumable> getConsumables() {
        List<Consumable> consumables = new ArrayList<>();
        for (Item item : inventory) {
            if (item instanceof Consumable consumable) {
                consumables.add(consumable);
            }
        }
        return consumables;
    }

    public boolean hasKeyItem(String keyName) {
        for (Item item : inventory) {
            if (item instanceof KeyItem && item.getName().equalsIgnoreCase(keyName)) {
                return true;
            }
        }
        return false;
    }

    // Puzzle Interaction
    public boolean solvePuzzle(String answer) {
        if (currentRoom == null || !currentRoom.hasPuzzle()) {
            return false;
        }

        Puzzle puzzle = currentRoom.getPuzzle();
        Puzzle.PuzzleResult result = puzzle.checkSolution(answer);

        if (result == Puzzle.PuzzleResult.CORRECT ||
                result == Puzzle.PuzzleResult.WRONG_FINAL) {
            currentRoom.removePuzzle();
        }

        return result == Puzzle.PuzzleResult.CORRECT;

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
        if (keyItem == null || !inventory.contains(keyItem)) {
            return false;
        }

        if (currentRoom == null) {
            return false;
        }

        String target = keyItem.getUnlockTarget();

        // Case 1: Key unlocks a puzzle
        if (currentRoom.hasPuzzle()) {
            Puzzle puzzle = currentRoom.getPuzzle();

            if (currentRoom.hasPuzzle()) {
                currentRoom.removePuzzle();
                inventory.remove(keyItem);
                keyItem.consume();
                return true;
            }
        }

        // Case 2: Key unlocks a barricaded exit
        if (currentRoom.isBarricaded()) {
            currentRoom.setBarricadedTo("NONE");
            inventory.remove(keyItem);
            keyItem.consume();
            return true;
        }

        return false;
    }

    // Info for Save / UI
    public String getStatus() {
        return "HP: " + currentHP + "/" + maxHP +
                " | ATK: " + getEffectiveATK() +
                " | DEF: " + getEffectiveDEF();
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

        // Auto-equip strongest weapon and armor on pickup
        autoEquipStrongest();

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

        // Aggregate items by name so stackable items display as "Name xN"
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (Item item : inventory) {
            String name = item.getName();
            counts.put(name, counts.getOrDefault(name, 0) + 1);
        }

        for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
            String name = e.getKey();
            int cnt = e.getValue();
            result.add(cnt > 1 ? name + " x" + cnt : name);
        }

        return result;
    }

    public boolean useItem(Item item) {
        if (item == null || !inventory.contains(item)) {
            return false;
        }

        if (itemUsedThisTurn) {
            return false;
        }

        item.use(this);
        itemUsedThisTurn = true;

        if (item instanceof Consumable || item instanceof KeyItem) {
            inventory.remove(item);
        }

        return true;
    }

    public String inspectEnemy(Monster monster) {
        if (monster == null || !monster.isAlive()) {
            return "There is no enemy to inspect.";
        }
        return monster.getInfo();
    }

    public boolean breach(String direction) {
        if (currentRoom == null) {
            return false;
        }

        if (!currentRoom.isBarricaded()) {
            return false;
        }

        int damage = (equippedWeapon != null && !equippedWeapon.isRanged())
                ? 5
                : 15;

        takeDamage(damage);

        // clear the barricade
        currentRoom.setBarricadedTo("NONE");
        return true;
    }

}
