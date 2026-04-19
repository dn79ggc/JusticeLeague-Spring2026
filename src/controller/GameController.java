// GameController.java
package controller;

import model.Armor;
import model.Game;
import model.Item;
import model.Player;
import model.Puzzle;
import model.Room;
import model.Weapon;
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

        // Handle other commands like "explore", "inventory", "inspect [item]", "drop
        // [item]", "equip [item]", "unequip weapon", "unequip armor", "attack",
        // "defend", "flee"
        if (input.equalsIgnoreCase("explore")) {
            String description = player.exploreRoom();
            view.showExploreResult(description);
            return false;
        }

        if (input.equalsIgnoreCase("inventory")) {
            view.showInventory(player.showInventory());
            return false;
        }

        if (input.toLowerCase().startsWith("inspect ")) {
            String itemName = input.substring(8).trim();
            Item item = player.getItemByName(itemName);

            if (item == null) {
                view.showMessage("You do not have that item.\n");
            } else {
                view.showInspectResult(player.inspectItem(item));
            }
            return false;
        }

        if (input.toLowerCase().startsWith("drop ")) {
            String itemName = input.substring(5).trim();
            Item item = player.getItemByName(itemName);

            if (item == null) {
                view.showMessage("You do not have that item.\n");
            } else if (player.dropItem(item)) {
                view.showMessage(item.getName() + " dropped.\n");
            } else {
                view.showMessage("You cannot drop that item.\n");
            }
            return false;
        }

        if (input.toLowerCase().startsWith("equip ")) {
            String itemName = input.substring(6).trim();
            Item item = player.getItemByName(itemName);

            if (item == null) {
                view.showMessage("You do not have that item.\n");
                return false;
            }

            if (item instanceof Weapon) {
                player.equipWeapon((Weapon) item);
                view.showEquipSuccess(item.getName());
            } else if (item instanceof Armor) {
                player.equipArmor((Armor) item);
                view.showEquipSuccess(item.getName());
            } else {
                view.showMessage("That item cannot be equipped.\n");
            }

            return false;
        }

        if (input.equalsIgnoreCase("unequip weapon")) {
            if (player.unequipWeapon()) {
                view.showUnequipSuccess("weapon");
            } else {
                view.showMessage("No weapon equipped or inventory full.\n");
            }
            return false;
        }

        if (input.equalsIgnoreCase("unequip armor")) {
            if (player.unequipArmor()) {
                view.showUnequipSuccess("armor");
            } else {
                view.showMessage("No armor equipped or inventory full.\n");
            }
            return false;
        }

        if (input.equalsIgnoreCase("attack")) {

            if (!currentRoom.hasMonster() || !currentRoom.getMonster().isAlive()) {
                view.showMessage("There is nothing to attack here.\n");
                return false;
            }

            int damage = player.attack();
            currentRoom.getMonster().takeDamage(damage);

            view.showAttackResult(damage);

            if (!currentRoom.getMonster().isAlive()) {
                view.showMessage("You defeated the monster!\n");
                currentRoom.removeMonster();
                player.resetTurnFlags();
                return false;
            }

            // Monster turn would go here later (CombatSystem)
            return false;
        }

        if (input.equalsIgnoreCase("defend")) {

            if (!currentRoom.hasMonster() || !currentRoom.getMonster().isAlive()) {
                view.showMessage("You are not in combat.\n");
                return false;
            }

            player.defend();
            view.showDefend();

            // Monster turn would follow
            return false;
        }

        if (input.equalsIgnoreCase("flee")) {

            if (!currentRoom.hasMonster() || !currentRoom.getMonster().isAlive()) {
                view.showMessage("There is nothing to flee from.\n");
                return false;
            }

            boolean success = player.flee();

            if (success) {
                view.showFleeSuccess();
                currentRoom.getMonster().reset();
                Room previous = player.getPreviousRoom();

                if (previous != null) {
                    player.move(previous);
                    view.showMessage("You retreat to the previous room.\n");
                }
            } else {
                view.showFleeFail();
                // Monster turn would go here
            }

            player.resetTurnFlags();
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
