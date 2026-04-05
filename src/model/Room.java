package model;

public class Room {
    private int[] directions = new int[4]; // North, East, South, West
    private int roomNumber;
    private String roomName;
    private String roomDescription;
    private boolean visited = false;

    public Room(int roomNumber, int north, int east, int south, int west, String roomDescription) {
        this.roomNumber = roomNumber;
        this.directions[0] = north;
        this.directions[1] = east;
        this.directions[2] = south;
        this.directions[3] = west;
        this.roomName = "Room " + roomNumber;
        this.roomDescription = roomDescription;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public String getRoomDescription() {
        return roomDescription;
    }

    public String getRoomName() {
        return roomName;
    }

    // Returns the destination room number for a given direction index,
    // or 0 if there is no exit in that direction.
    public int getExit(int directionIndex) {
        if (directionIndex >= 0 && directionIndex < directions.length) {
            return directions[directionIndex];
        }
        return 0;
    }

    // Returns a readable list of available exits — used by GameView.
    public String getAvailableExits() {
        String[] names = {"North", "East", "South", "West"};
        StringBuilder exits = new StringBuilder("Available exits: ");
        boolean anyExit = false;
        for (int i = 0; i < directions.length; i++) {
            if (directions[i] > 0) {
                exits.append(names[i]).append(" ");
                anyExit = true;
            }
        }
        return anyExit ? exits.toString().trim() : "No exits available.";
    }

    // Mark the room as visited
    public void setVisited() {
        this.visited = true;
    }

    // Check visited status
    public boolean isVisited() {
        return visited;
    }
}
