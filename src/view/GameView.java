package view;

public class GameView {

    // Shown once after the map loads successfully
    public void showWelcome(int totalRooms) {
        System.out.println("=== Welcome to Text Explorer Game ===");
        System.out.println("Total rooms on map: " + totalRooms);
        System.out.println("Starting location: Room 1");
        System.out.println();
    }

    // Shown at the start of every game loop cycle
    public void showPrompt(String roomName) {
        System.out.println("Current Location: " + roomName);
        System.out.println("Enter direction: 'n' (North), 'e' (East), 's' (South), 'w' (West), or 'q' (Quit)");
        System.out.print("> ");
    }

    // Shown when the player enters a room they haven't visited before
    public void showNewRoom(String roomName, String roomDescription) {
        System.out.println("You discovered: " + roomName + " - " + roomDescription);
        System.out.println();
    }

    // Shown when the player re-enters an already-visited room
    public void showVisitedRoom(String roomName) {
        System.out.println("You've been to " + roomName + " before.");
        System.out.println();
    }

    public void showMoveSuccess() {
        System.out.println("Move successful!");
    }

    public void showInvalidDirection() {
        System.out.println("You cannot go this way.\n");
    }

    public void showInvalidInput() {
        System.out.println("Invalid input. Try again.\n");
    }

    // Shown when the player has explored every room
    public void showVictory() {
        System.out.println("=== CONGRATULATIONS! ===");
        System.out.println("You have discovered all rooms on the map!");
        System.out.println("Enter 'q' to quit.\n");
    }

    public void showGoodbye() {
        System.out.println("\nThank you for playing! Goodbye!");
    }

    // Shown when the map data file cannot be found
    public void showMapLoadError(String filename) {
        System.out.println("Error: data/" + filename + " not found. Check the data/ folder.");
    }

    public void showMapLoadSuccess() {
        System.out.println("Map loaded successfully!");
    }
}
