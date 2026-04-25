package controller;

import model.Room;
import java.util.Map;

/**
 * Handles room navigation logic, movement validation, and barricade checks.
 * Implements the room-graph traversal and directional navigation system.
 * Manages player transitions between rooms in the game world.
 * 
 * @author Sebastian Ochoa Cabrera
 */
public class RoomController {

    // The Controller holds the logic/rules, acting ON the Model.
    public void attemptMove(Room currentRoom, String direction, Map<String, Room> allRooms) {

        // Rule 1: Does the exit exist?
        String targetRoomId = currentRoom.getExit(direction);
        if (targetRoomId == null) {
            System.out.println("There is no exit in that direction.");
            return; // Stop processing
        }

        // Rule 2: Is the exit blocked by the Poltergeist?
        if (currentRoom.isBarricaded(direction)) {
            System.out.println("This door has been barricaded! You cannot pass.");
            return; // Stop processing
        }

        // Action: Execute the move
        Room nextRoom = allRooms.get(targetRoomId);
        nextRoom.enter(); // Updates the visited boolean in the Model

        // Output to View
        System.out.println("You move " + direction + ".");
        System.out.println(nextRoom.getFullDescription());

        // Rule 3: 100% Combat Trigger on entry
        if (nextRoom.hasMonster()) {
            System.out.println("COMBAT STARTED WITH: " + nextRoom.getMonster().getName());
            // triggerCombatLoop(nextRoom.getMonster());
        }
    }
}