package model;

public class Player {
    private int location;
    private int roomsVisited;
    private int totalMoves;

    public Player(int startingLocation) {
        this.location = startingLocation;
        this.roomsVisited = 1; // Player starts in Room 1, which counts
        this.totalMoves = 0;
    }

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
}
