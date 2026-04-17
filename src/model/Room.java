package model;

import java.util.*;

public class Room {
    private String roomId;
    private String name;
    private String roomDescription;
    private boolean visited = false;
    private String barricadedTo = null; // RoomID of barricaded exit, null if none
    private Map<String, Integer> exits;
    private List<Item> itemsInRoom;
    private Monster monster;
    private Puzzle puzzle;

    public Room(String roomId, String name, String roomDescription) {
        this.roomId = roomId;
        this.name = name;
        this.roomDescription = roomDescription;
        this.itemsInRoom = new ArrayList<>();
        this.exits = new HashMap<>();
        this.monster = null;
        this.puzzle = null;
    }

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
        return roomDescription;
    }

    public boolean hasVisited() {
        return visited;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited() {
        this.visited = true;
    }

    public String getBarricadedTo() {
        return barricadedTo;
    }

    public void setBarricadedTo(String roomId) {
        this.barricadedTo = roomId;
    }

    public boolean isBarricaded() {
        return barricadedTo != null && !barricadedTo.equals("NONE");
    }

    public Monster getMonster() {
        return monster;
    }

    public void setMonster(Monster monster) {
        this.monster = monster;
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

    public boolean hasPuzzle() {
        return puzzle != null;
    }

    public void addItem(Item item) {
        itemsInRoom.add(item);
    }

    public void removeItem(Item item) {
        itemsInRoom.remove(item);
    }

    public void removeItem(String item) {
        itemsInRoom.removeIf(i -> i.getName().equals(item));
    }

    public List<Item> getItems() {
        return itemsInRoom;
    }

    public void addExit(String direction, int destinationRoomNumber) {
        if (destinationRoomNumber > 0) {
            exits.put(direction, destinationRoomNumber);
        }
    }

    public int getExit(int directionIndex) {
        return switch (directionIndex) {
            case 0 -> exits.getOrDefault("N", 0);
            case 1 -> exits.getOrDefault("E", 0);
            case 2 -> exits.getOrDefault("S", 0);
            case 3 -> exits.getOrDefault("W", 0);
            default -> 0;
        };
    }

    public String getFullDescription() {
        return roomDescription;
    }
}
