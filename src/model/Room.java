package model;

import java.util.*;

/**
 * Represents a room in the game world with directional exits, description,
 * items, and puzzle.
 * Core component of the room graph and navigation system.
 * Manages room-to-room connectivity and coordinates for the interactive map.
 * 
 * @author Sebastian Ochoa Cabrera
 */
public class Room {
    // Properties strictly from the System Design PDF
    private String roomId; // Needed for your HashMap
    private String name;
    private String description;
    private Map<String, String> exits;
    private Map<String, Integer> exitNumbers;
    private boolean isIndoor;
    private List<Item> items;
    private Monster monster;
    private Puzzle puzzle;
    private boolean visited;
    private Set<String> barricadedExits;
    private String barricadedTo;

    public Room(String roomId, String name, String description, boolean isIndoor) {
        this.roomId = roomId;
        this.name = name;
        this.description = description;
        this.isIndoor = isIndoor;

        this.exits = new HashMap<>();
        this.exitNumbers = new HashMap<>();
        this.items = new ArrayList<>();
        this.barricadedExits = new HashSet<>();

        this.monster = null;
        this.puzzle = null;
        this.visited = false;
        this.barricadedTo = "NONE";
    }

    public Room(String roomId, String name, String description) {
        this(roomId, name, description, false);
    }

    // --- Navigation & Exit Methods ---
    public void addExit(String direction, String targetRoomId) {
        if (targetRoomId != null && !targetRoomId.equals("null")) {
            exits.put(direction.toUpperCase(), targetRoomId);
        }
    }

    public String getExit(String direction) {
        return exits.get(direction.toUpperCase());
    }

    public int getExit(int directionIndex) {
        String dir = switch (directionIndex) {
            case 0 -> "N";
            case 1 -> "E";
            case 2 -> "S";
            case 3 -> "W";
            default -> null;
        };
        if (dir == null) {
            return -1;
        }
        return exitNumbers.getOrDefault(dir, -1);
    }

    public void setExitNumber(String direction, Integer roomNumber) {
        if (direction == null) {
            return;
        }
        String key = direction.toUpperCase();
        if (roomNumber == null || roomNumber <= 0) {
            exitNumbers.remove(key);
            return;
        }
        exitNumbers.put(key, roomNumber);
    }

    public Map<String, String> getExits() {
        return exits;
    }

    public void enter() {
        this.visited = true;
    }

    public void setVisited() {
        this.visited = true;
    }

    // --- Barricade Methods ---
    public boolean isBarricaded(String direction) {
        return barricadedExits.contains(direction.toUpperCase());
    }

    public boolean isBarricaded() {
        return !"NONE".equalsIgnoreCase(barricadedTo) || !barricadedExits.isEmpty();
    }

    public void barricadeExit(String direction) {
        String dir = direction.toUpperCase();
        barricadedExits.add(dir);
        String target = exits.get(dir);
        if (target != null) {
            barricadedTo = target;
        }
    }

    public void unbarricadeExit(String direction) {
        String dir = direction.toUpperCase();
        barricadedExits.remove(dir);
        if (barricadedExits.isEmpty()) {
            barricadedTo = "NONE";
        }
    }

    public String getBarricadedTo() {
        return barricadedTo;
    }

    public void setBarricadedTo(String roomId) {
        if (roomId == null || roomId.isBlank() || "NONE".equalsIgnoreCase(roomId)) {
            barricadedTo = "NONE";
            barricadedExits.clear();
            return;
        }

        barricadedTo = roomId;
        barricadedExits.clear();
        for (Map.Entry<String, String> exit : exits.entrySet()) {
            if (roomId.equalsIgnoreCase(exit.getValue())) {
                barricadedExits.add(exit.getKey().toUpperCase());
                break;
            }
        }
    }

    public String getRandomUnbarricadedExit() {
        List<String> available = new ArrayList<>();
        for (String dir : exits.keySet()) {
            if (!isBarricaded(dir)) {
                available.add(dir);
            }
        }
        if (available.isEmpty()) {
            return null;
        }
        return available.get(new Random().nextInt(available.size()));
    }

    // --- Monster Methods ---
    public boolean hasMonster() {
        return monster != null;
    }

    public Monster getMonster() {
        return monster;
    }

    public void setMonster(Monster monster) {
        this.monster = monster;
    }

    public void removeMonster() {
        this.monster = null;
    }

    // --- Puzzle Methods ---
    public boolean hasPuzzle() {
        return puzzle != null;
    }

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
    }

    public void removePuzzle() {
        this.puzzle = null;
    }

    // --- Item Methods ---
    public boolean hasItems() {
        return !items.isEmpty();
    }

    public List<Item> getItems() {
        return items;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    // --- Description Methods ---
    public String describe() {
        return description;
    }

    public String getFullDescription() {
        StringBuilder fullDesc = new StringBuilder(description);

        if (hasItems()) {
            fullDesc.append("\nItems on the floor: ");
            for (Item i : items) {
                fullDesc.append(i.getName()).append(", ");
            }
        }
        if (hasMonster()) {
            fullDesc.append("\nA ").append(monster.getName()).append(" is here!");
        }
        if (hasPuzzle()) {
            fullDesc.append("\nThere is a puzzle here: ").append(puzzle.getNarrative());
        }
        return fullDesc.toString();
    }

    // --- Getters ---
    public String getRoomId() {
        return roomId;
    }

    public String getName() {
        return name;
    }

    public String getRoomName() {
        return name;
    }

    public String getRoomDescription() {
        return description;
    }

    public boolean isVisited() {
        return visited;
    }

    public boolean isIndoor() {
        return isIndoor;
    }
}