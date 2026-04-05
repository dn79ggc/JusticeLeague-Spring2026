// GameController.java
package controller;

import model.Game;
import model.Player;
import model.Room;
import view.GameView;
import java.util.Scanner;

public class GameController {

    private static final String MAP_FILE = "Rooms.txt";

    // Called by Main. Owns the game loop.
    public void run() {
        Scanner scanner = new Scanner(System.in);

        Game game = new Game();
        GameView view = new GameView();
        Player player = new Player(1);
        boolean gameEnd = false;

        boolean loaded = game.mapGenerate(MAP_FILE);
        if (!loaded) {
            view.showMapLoadError(MAP_FILE);
            return;
        }
        view.showMapLoadSuccess();
        view.showWelcome(game.getTotalRooms());

        game.getRoomByNumber(1).setVisited();

        while (!gameEnd) {
            Room currentRoom = game.getRoomByNumber(player.getLocation());
            view.showPrompt(currentRoom.getRoomName());

            char input = scanner.next().toLowerCase().charAt(0);

            if (input == 'q') {
                gameEnd = true;
                view.showGoodbye();
                continue;
            }

            if (!validateInput(input)) {
                view.showInvalidInput();
                continue;
            }

            int directionIndex = indexFromDirection(input);
            int nextRoomNumber = player.attemptMove(currentRoom, directionIndex);

            if (nextRoomNumber == -1) {
                view.showInvalidDirection();
            } else {
                view.showMoveSuccess();
                Room newRoom = game.getRoomByNumber(nextRoomNumber);
                if (!newRoom.isVisited()) {
                    newRoom.setVisited();
                    player.incrementRoomsVisited();
                    view.showNewRoom(newRoom.getRoomName(), newRoom.getRoomDescription());
                } else {
                    view.showVisitedRoom(newRoom.getRoomName());
                }
                if (game.allRoomsVisited()) {
                    view.showVictory();
                }
            }
        }
        scanner.close();
    }

    public static boolean validateInput(char input) {
        return "neswq".indexOf(input) >= 0;
    }

    public static int indexFromDirection(char direction) {
        switch (direction) {
            case 'n': return 0;
            case 'e': return 1;
            case 's': return 2;
            case 'w': return 3;
            default:  return -1;
        }
    }
}