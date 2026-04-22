package model;

import java.util.*;

public class Room {
    // Properties strictly from the System Design PDF
    private String roomId; // Needed for your HashMap
    private String name;
    private String description;
    private Map<String, String> exits;
    private boolean isIndoor;
    private List<Item> items;
    private Monster monster;
    private Puzzle puzzle;
    private boolean visited;
    private Set<String> barricadedExits;

    public Room(String roomId, String name, String description, boolean isIndoor) {
        this.roomId = roomId;
        this.name = name;
        this.description = description;
        this.isIndoor = isIndoor;

        this.exits = new HashMap<>();
        this.items = new ArrayList<>();
        this.barricadedExits = new HashSet<>();

        this.monster = null;
        this.puzzle = null;
        this.visited = false;
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

    public Map<String, String> getExits() {
        return exits;
    }

    public void enter() {
        this.visited = true;
    }

    // --- Barricade Methods ---
    public boolean isBarricaded(String direction) {
        return barricadedExits.contains(direction.toUpperCase());
    }

    public void barricadeExit(String direction) {
        barricadedExits.add(direction.toUpperCase());
    }

    public void unbarricadeExit(String direction) {
        barricadedExits.remove(direction.toUpperCase());
    }

    // --- Monster Methods ---
    public boolean hasMonster() { return monster != null; }
    public Monster getMonster() { return monster; }
    public void setMonster(Monster monster) { this.monster = monster; }
    public void removeMonster() { this.monster = null; }

    // --- Puzzle Methods ---
    public boolean hasPuzzle() { return puzzle != null; }
    public Puzzle getPuzzle() { return puzzle; }
    public void setPuzzle(Puzzle puzzle) { this.puzzle = puzzle; }
    public void removePuzzle() { this.puzzle = null; }

    // --- Item Methods ---
    public boolean hasItems() { return !items.isEmpty(); }
    public List<Item> getItems() { return items; }
    public void addItem(Item item) { items.add(item); }
    public void removeItem(Item item) { items.remove(item); }

    // --- Description Methods ---
    public String describe() {
        return description;
    }

    public String getFullDescription() {
        StringBuilder fullDesc = new StringBuilder(description);

        if (hasItems()) {
            fullDesc.append("\nItems on the floor: ");
            for (Item i : items) { fullDesc.append(i.getName()).append(", "); }
        }
        if (hasMonster()) {
            fullDesc.append("\nA ").append(monster.getName()).append(" is here!");
        }
        if (hasPuzzle()) {
            fullDesc.append("\nThere is a puzzle here: ").append(puzzle.getDescription());
        }
        return fullDesc.toString();
    }

    // --- Getters ---
    public String getRoomId() { return roomId; }
    public String getName() { return name; }
    public boolean isVisited() { return visited; }
    public boolean isIndoor() { return isIndoor; }
}