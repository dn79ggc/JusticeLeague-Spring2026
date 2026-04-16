// GameController.java
package controller;

import model.Game;
import model.Player;
import model.Puzzle;
import model.Room;
import view.GameView;

import java.util.Scanner;

public class GameController {

    private static final String MAP_FILE = "rooms.csv";
    private static final String PUZZLE_FILE = "puzzles.csv";

    private boolean puzzleActive = false;
    private Puzzle activePuzzle = null;

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

        game.loadPuzzles(PUZZLE_FILE);

        view.showMapLoadSuccess();
        view.showWelcome(game.getTotalRooms());
        game.getRoomByNumber(1).setVisited();

        boolean gameEnd = false;
        while (!gameEnd) {
            Room currentRoom = game.getRoomByNumber(player.getLocation());
            view.showPrompt(currentRoom.getRoomName(), currentRoom.hasPuzzle());

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                view.showInvalidInput();
                continue;
            }

            gameEnd = handleInput(input, player, game, view, currentRoom);
        }

        scanner.close();
    }

    private boolean handleInput(String input, Player player, Game game, GameView view, Room currentRoom) {
        if (input.equalsIgnoreCase("q")) {
            view.showGoodbye();
            return true;
        }

        if (puzzleActive) {
            handlePuzzleInput(input, player, view, currentRoom);
            return false;
        }

        if (input.equalsIgnoreCase("solve puzzle")) {
            handleSolvePuzzle(player, view, currentRoom);
            return false;
        }

        if (input.length() == 1 && validateInput(input.charAt(0))) {
            int directionIndex = indexFromDirection(input.charAt(0));
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

        view.showInvalidInput();
        return false;
    }

    private void handleSolvePuzzle(Player player, GameView view, Room currentRoom) {
        if (!currentRoom.hasPuzzle()) {
            view.showNoPuzzleInRoom();
            return;
        }

        if (currentRoom.getMonster() != null && currentRoom.getMonster().isAlive()) {
            view.showCannotSolvePuzzleInCombat();
            return;
        }

        activePuzzle = currentRoom.getPuzzle();
        puzzleActive = true;
        String startText = activePuzzle.start();
        view.showPuzzleStart(activePuzzle.getName(), activePuzzle.getNarrative(), startText,
                activePuzzle.getAttemptsLeft(), activePuzzle.hasHint());
    }

    private void handlePuzzleInput(String input, Player player, GameView view, Room currentRoom) {
        if (input.equalsIgnoreCase("hint")) {
            view.showPuzzleHint(activePuzzle.getHint());
            return;
        }

        Puzzle.PuzzleResult result = activePuzzle.checkSolution(input);
        switch (result) {
            case CORRECT -> {
                view.showPuzzleSolved(activePuzzle.getSuccessMessage());
                currentRoom.removePuzzle();
                activePuzzle = null;
                puzzleActive = false;
            }
            case WRONG_RETRY -> view.showPuzzleWrong(activePuzzle.getFailureMessage(), activePuzzle.getAttemptsLeft());
            case WRONG_FINAL -> {
                view.showPuzzleFailed(activePuzzle.getFailureMessage());
                player.takeDamage(5);
                view.showPlayerDamage(player.getCurrentHP());
                currentRoom.removePuzzle();
                activePuzzle = null;
                puzzleActive = false;
            }
            case INVALID_INPUT -> view.showInvalidPuzzleInput();
        }
    }

    public static boolean validateInput(char input) {
        return "nesw".indexOf(input) >= 0;
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
