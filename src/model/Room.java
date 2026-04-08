package model;
import java.util.*;

public class Room {
    private String roomId;
    private String name;
    private String roomDescription;
    private boolean visited = false;
    private Map <String, String> exits;
    private ArrayList<Item> itemsInRoom;
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

    public String getRoomDescription() {
        return roomDescription;
    }

    public boolean hasVisited() {
        return visited;
    }

    public void setVisited() {
        this.visited = true;
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

    public void addItem(Item item){
        itemsInRoom.add(item);
    }

    public void removeItem(String item){
        this.itemsInRoom.remove(item);
    }

    public void addExit(String direction, String roomId){
        if(roomId != null && !roomId.equals("null")){
            exits.put(direction, roomId);
        }
    }

    public void getExit(String direction){
        exits.getOrDefault(direction, null);
    }




}