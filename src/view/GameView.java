package view;

import java.util.List;

public class GameView {

    public enum MessageType {
        SYSTEM,
        NARRATION,
        DAMAGE,
        HEAL,
        ERROR,
        SEPARATOR
    }

    public enum WinType {
        CLEANSE,
        ESCAPE
    }

    // Shown once after the map loads successfully
    public void showWelcome(int totalRooms) {
        System.out.println("=== Welcome to Text Explorer Game ===");
        System.out.println("Total rooms on map: " + totalRooms);
        System.out.println("Starting location: Room 1");
        System.out.println();
    }

    // Shown at the start of every game loop cycle
    public void showPrompt(String roomName, boolean hasPuzzle) {
        System.out.println("Current Location: " + roomName);
        System.out.println("Enter direction: 'n' (North), 'e' (East), 's' (South), 'w' (West), or 'q' (Quit)");
        if (hasPuzzle) {
            System.out.println("Click 'Solve Puzzle' to attempt the puzzle in this room.");
        }
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

    public void showNoPuzzleInRoom() {
        System.out.println("There is no puzzle in this room.\n");
    }

    public void showCannotSolvePuzzleInCombat() {
        System.out.println("You cannot focus on the puzzle while something is trying to kill you.\n");
    }

    public void showPuzzleStart(String puzzleName, String narrative, String startText, int attemptsRemaining,
            boolean hasHint) {
        System.out.println("=== Puzzle: " + puzzleName + " ===");
        System.out.println(formatOutput(narrative));
        System.out.println();
        System.out.println(formatOutput(startText));
        System.out.println("Attempts left: " + attemptsRemaining);
        if (hasHint) {
            System.out.println("Type 'hint' for a clue.");
        }
        System.out.println("Type your answer and press Enter.");
        System.out.println();
    }

    public void showPuzzleSolved(String successMessage) {
        System.out.println(successMessage);
        System.out.println("The puzzle fades away.\n");
    }

    public void showPuzzleWrong(String failureMessage, int attemptsLeft) {
        System.out.println(failureMessage);
        System.out.println("Attempts left: " + attemptsLeft + "\n");
    }

    public void showPuzzleFailed(String failureMessage) {
        System.out.println(failureMessage);
        System.out.println("The puzzle crumbles. You lose 5 HP.\n");
    }

    public void showInvalidPuzzleInput() {
        System.out.println("That is not a valid answer. Try again.\n");
    }

    public void showPuzzleHint(String hint) {
        System.out.println("Hint: " + formatOutput(hint) + "\n");
    }

    public void showPlayerDamage(int currentHP) {
        System.out.println("Current HP: " + currentHP + "\n");
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

    public void showExploreResult(String description) {
        System.out.println(formatOutput(description) + "\n");
    }

    public void showInventory(List<String> items) {
        for (String line : items) {
            System.out.println(line);
        }
        System.out.println();
    }

    public void showInspectResult(String info) {
        System.out.println(formatOutput(info) + "\n");
    }

    public void showMessage(String message) {
        System.out.println(formatOutput(message));
    }

    public void displayMessage(String message, MessageType type) {
        String formatted = formatOutput(message);
        if (type == MessageType.SEPARATOR) {
            System.out.println(formatted);
            return;
        }
        System.out.println("[" + type + "] " + formatted);
    }

    public void showCombatStart(String monsterName, int level) {
        System.out.println("=== Combat Start ===");
        System.out.println(monsterName + " (Lv " + level + ")");
    }

    public void updateMonsterStats(String name, int currentHp, int maxHp) {
        System.out.println("Monster: " + name + " HP " + currentHp + "/" + maxHp);
    }

    public void updatePlayerStats(int currentHp, int maxHp, int atk, int def) {
        System.out.println("Player HP " + currentHp + "/" + maxHp + " | ATK " + atk + " | DEF " + def);
    }

    public void setCombatActionsEnabled(boolean enabled) {
        // Text UI does not disable controls; command loop gates actions by state.
    }

    public void updateEquipment(String weaponName, int weaponAtk, String armorName, int defense) {
        System.out.println("Equipped: " + weaponName + " (ATK+" + weaponAtk + ") | Armor: " + armorName
                + " | DEF " + defense);
    }

    public void showGameOverScreen() {
        System.out.println("=== GAME OVER ===");
    }

    public void showWinScreen(WinType winType) {
        if (winType == WinType.CLEANSE) {
            System.out.println("=== CLEANSE ENDING ===");
        } else {
            System.out.println("=== ESCAPE ENDING ===");
        }
    }

    public void showEquipSuccess(String itemName) {
        System.out.println(itemName + " equipped successfully.\n");
    }

    public void showUnequipSuccess(String slot) {
        System.out.println("Unequipped " + slot + " successfully.\n");
    }

    public void showAttackResult(int damage) {
        System.out.println("You attack and deal " + damage + " damage.\n");
    }

    public void showDefend() {
        System.out.println("You brace yourself and prepare to defend.\n");
    }

    public void showFleeSuccess() {
        System.out.println("You successfully flee from combat!\n");
    }

    public void showFleeFail() {
        System.out.println("You failed to flee!\n");
    }

    public void showInspectEnemy(String info) {
        System.out.println(formatOutput(info) + "\n");
    }

    private String formatOutput(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "\n");
    }

    // Shown when the map data file cannot be found
    public void showMapLoadError(String filename) {
        System.out.println("Error: data/" + filename + " not found. Check the data/ folder.");
    }

    public void showMapLoadSuccess() {
        System.out.println("Map loaded successfully!");
    }
}
