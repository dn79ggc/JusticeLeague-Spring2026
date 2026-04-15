// GameController.java
package controller;

import model.Game;
import model.Player;
import model.Room;
import view.GameView;

import java.io.File;
import java.io.*;
import java.util.*;

public class GameController {

    private static final String MAP_FILE = "Rooms.txt";

    // Called by Main. Owns the game loop.
    public void run() {
        Scanner scanner = new Scanner(System.in);

        Game game = new Game();
        GameView view = new GameView();
        Player player = new Player(1);

        if (!game.mapGenerate(MAP_FILE)) {
            view.showMapLoadError(MAP_FILE);
            return;
        }

        view.showMapLoadSuccess();
        view.showWelcome(game.getTotalRooms());
        game.getRoomByNumber(1).setVisited();

        boolean gameEnd = false;
        while (!gameEnd) {
            Room currentRoom = game.getRoomByNumber(player.getLocation());
            view.showPrompt(currentRoom.getRoomName());

            char input = scanner.next().toLowerCase().charAt(0);
            gameEnd = handleInput(input, player, game, view, currentRoom);
        }

        scanner.close();
    }

    private boolean handleInput(char input, Player player, Game game, GameView view, Room currentRoom) {
        if (input == 'q') {
            view.showGoodbye();
            return true;
        }

        if (!validateInput(input)) {
            view.showInvalidInput();
            return false;
        }

        int directionIndex = indexFromDirection(input);
        int nextRoomNumber = player.attemptMove(currentRoom, directionIndex);

        if (nextRoomNumber == -1) {
            view.showInvalidDirection();
            return false;
        }

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
        return false;
    }

    public static boolean validateInput(char input) {
        return "neswq".indexOf(input) >= 0;
    }

    public static int indexFromDirection(char direction) {
        return switch (direction) {
            case 'n' -> 0;
            case 'e' -> 1;
            case 's' -> 2;
            case 'w' -> 3;
            default -> -1;
        };
    }
}
