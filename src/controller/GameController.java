// GameController.java
package controller;

import model.Armor;
import model.Game;
import model.Item;
import model.KeyItem;
import model.Monster;
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
    private CombatSystem combatSystem;
    private InventoryController inventoryController;

    // Called by Main. Owns the game loop.
    public void run() {
        Scanner scanner = new Scanner(System.in);

        Game game = new Game();
        GameView view = new GameView();
        Player player = new Player(1);
        inventoryController = new InventoryController();

        if (!game.mapGenerate(MAP_FILE)) {
            view.showMapLoadError(MAP_FILE);
            return;
        }

        game.loadPuzzles(PUZZLE_FILE);
        game.loadMonstersFromCsv("monsters.csv");
        game.loadItemsFromCsv("items.csv", "weapons.csv", "armor.csv", "consumables.csv");

        view.showMapLoadSuccess();
        view.showWelcome(game.getTotalRooms());
        game.getRoomByNumber(1).setVisited();

        combatSystem = new CombatSystem(game, view);

        boolean gameEnd = false;
        while (!gameEnd) {
            if (!player.isAlive()) {
                view.showGameOverScreen();
                gameEnd = true;
                continue;
            }

            Room currentRoom = game.getRoomByNumber(player.getLocation());
            player.setCurrentRoom(currentRoom);

            if (!combatSystem.isInCombat()
                    && currentRoom != null
                    && currentRoom.hasMonster()
                    && currentRoom.getMonster().isAlive()) {
                combatSystem.startCombat(player, currentRoom.getMonster(), currentRoom);
                if (!player.isAlive() || combatSystem.isEndingReached()) {
                    gameEnd = true;
                    continue;
                }
            }

            if (combatSystem.isInCombat()) {
                view.showMessage(
                        "Combat actions: attack, defend, useitem, use [item], equip, equip [weapon], flee, inspect enemy\n");
                System.out.print("> ");
            } else {
                view.showPrompt(currentRoom.getRoomName(), currentRoom.hasPuzzle());
            }

            if (!scanner.hasNextLine()) {
                view.showGoodbye();
                gameEnd = true;
                continue;
            }

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                view.showInvalidInput();
                continue;
            }

            gameEnd = handleInput(input, player, game, view, currentRoom);
            if (!gameEnd && (!player.isAlive() || combatSystem.isEndingReached())) {
                gameEnd = true;
            }
        }

        scanner.close();
    }

    private boolean handleInput(String input, Player player, Game game, GameView view, Room currentRoom) {
        if (input.equalsIgnoreCase("q")) {
            view.showGoodbye();
            return true;
        }

        if (puzzleActive) {
            return handlePuzzleInput(input, player, view, currentRoom);
        }

        if (combatSystem != null && combatSystem.isInCombat()) {
            combatSystem.executeCombatCycle(input, player);
            return !player.isAlive() || combatSystem.isEndingReached();
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
            player.setCurrentRoom(newRoom);
            if (!newRoom.isVisited()) {
                newRoom.setVisited();
                player.incrementRoomsVisited();
                if (newRoom.hasMonster() && newRoom.getMonster().isAlive()) {
                    combatSystem.startCombat(player, newRoom.getMonster(), newRoom);
                    return false;
                }
                view.showNewRoom(newRoom.getRoomName(), newRoom.getRoomDescription());
            } else {
                if (newRoom.hasMonster() && newRoom.getMonster().isAlive()) {
                    combatSystem.startCombat(player, newRoom.getMonster(), newRoom);
                    return false;
                }
                view.showVisitedRoom(newRoom.getRoomName());
            }

            if (game.allRoomsVisited()) {
                view.showVictory();
            }

            if (combatSystem.checkEscapeWinCondition(player)) {
                return true;
            }
            return false;
        }

        // Handle other commands like "explore", "inventory", "inspect [item]", "drop
        // [item]", "equip [item]", "unequip weapon", "unequip armor", "attack",
        // "defend", "flee", "breach [direction]", "use [item]", "inspect enemy" etc.
        if (input.equalsIgnoreCase("explore")) {
            String description = player.exploreRoom();
            view.showExploreResult(description);
            return false;
        }

        if (input.equalsIgnoreCase("inventory")) {
            view.showInventory(player.showInventory());
            return false;
        }

        if (input.equalsIgnoreCase("pickup")) {
            if (currentRoom == null || !currentRoom.hasItems()) {
                view.showMessage("There are no items to pick up here.\n");
                return false;
            }

            Item roomItem = currentRoom.getItems().get(0);
            if (player.pickupItem(roomItem)) {
                view.showMessage("Picked up " + roomItem.getName() + ".\n");
            } else {
                view.showMessage("Could not pick up item (bag may be full).\n");
            }
            return false;
        }

        if (input.toLowerCase().startsWith("pickup ")) {
            if (currentRoom == null || !currentRoom.hasItems()) {
                view.showMessage("There are no items to pick up here.\n");
                return false;
            }

            String itemName = input.substring(7).trim();
            Item roomItem = null;
            for (Item candidate : currentRoom.getItems()) {
                if (candidate.getName().equalsIgnoreCase(itemName)) {
                    roomItem = candidate;
                    break;
                }
            }

            if (roomItem == null) {
                view.showMessage("That item is not in this room.\n");
                return false;
            }

            if (player.pickupItem(roomItem)) {
                view.showMessage("Picked up " + roomItem.getName() + ".\n");
            } else {
                view.showMessage("Could not pick up item (bag may be full).\n");
            }
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
            view.showMessage("You can only attack while in combat.\n");
            return false;
        }

        if (input.equalsIgnoreCase("defend")) {
            view.showMessage("You can only defend while in combat.\n");
            return false;
        }

        if (input.equalsIgnoreCase("flee")) {
            view.showMessage("You can only flee while in combat.\n");
            return false;
        }

        if (input.toLowerCase().startsWith("breach ")) {
            String direction = input.substring(7).trim().toLowerCase();

            Room room = player.getCurrentRoom();

            if (room == null) {
                view.showMessage("You are nowhere. That should not happen.\n");
                return false;
            }

            if (!room.isBarricaded(direction)) {
                view.showMessage("There is no barricade in that direction.\n");
                return false;
            }

            boolean success = player.breach(direction);

            if (success) {
                view.showMessage(
                        "You force your way through the barricade, hurting yourself in the process.\n");
                view.showPlayerDamage(player.getCurrentHP());
            } else {
                view.showMessage("You fail to breach the barricade.\n");
            }

            return false;
        }

        if (input.toLowerCase().startsWith("use ")) {
            String itemName = input.substring(4).trim();

            Item item = player.getItemByName(itemName);

            if (item == null) {
                view.showMessage("You do not have that item.\n");
                return false;
            }

            boolean used = player.useItem(item);

            if (used) {
                view.showMessage(item.getName() + " used.\n");
                view.showPlayerDamage(player.getCurrentHP());
            } else {
                view.showMessage("You cannot use that item right now.\n");
            }

            return false;
        }

        if (input.equalsIgnoreCase("inspect enemy")) {
            Monster monster = combatSystem != null && combatSystem.isInCombat()
                    ? combatSystem.getActiveMonster()
                    : currentRoom.getMonster();

            if (monster == null || !monster.isAlive()) {
                view.showMessage("There is no enemy to inspect.\n");
                return false;
            }

            String info = player.inspectEnemy(monster);
            view.showInspectEnemy(info);

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
                activePuzzle.getAttempts(), activePuzzle.hasHint());
    }

    private boolean handlePuzzleInput(String input, Player player, GameView view, Room currentRoom) {
        if (input.equalsIgnoreCase("hint")) {
            view.showPuzzleHint(activePuzzle.getHint());
            return false;
        }

        Puzzle.PuzzleResult result = activePuzzle.checkSolution(input);
        switch (result) {
            case CORRECT -> {
                view.showPuzzleSolved(activePuzzle.getSuccessMessage());
                if ("Front Gate Key".equalsIgnoreCase(activePuzzle.getName()) && !player.hasKeyItem("Front Gate Key")) {
                    player.addToInventory(new KeyItem("Front Gate Key", "GH-01"));
                    view.showMessage("You obtained the Front Gate Key.\n");
                }
                currentRoom.removePuzzle();
                activePuzzle = null;
                puzzleActive = false;
                if (combatSystem.checkEscapeWinCondition(player)) {
                    return true;
                }
            }
            case WRONG_RETRY -> view.showPuzzleWrong(activePuzzle.getFailureMessage(), activePuzzle.getAttempts());
            case WRONG_FINAL -> {
                view.showPuzzleFailed(activePuzzle.getFailureMessage());
                player.takeDamage(5);
                view.showPlayerDamage(player.getCurrentHP());
                currentRoom.removePuzzle();
                activePuzzle = null;
                puzzleActive = false;
                if (!player.isAlive()) {
                    view.showGameOverScreen();
                    return true;
                }
            }
            case INVALID_INPUT -> view.showInvalidPuzzleInput();
        }
        return false;
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
