# JusticeLeague-Spring2026

This repository contains a Java room-exploration game with a JavaFX GUI as the primary interface. A terminal text mode is also available as a legacy entry point.

The project uses an MVC architecture to separate game state, display, and input handling, and supports combat, puzzles, inventory management, and save/load functionality.

---

## Quick Start

This project includes a Gradle wrapper so the repository can be built and run from any supported machine with Java 26 installed.

From the project root:

```powershell
# Build the project
./gradlew build
```

```powershell
# Run the JavaFX GUI (primary mode)
./gradlew runGui
```

On Windows PowerShell, use `./gradlew.bat` if needed:

```powershell
./gradlew.bat runGui
```

### Legacy Text Mode

```powershell
# Run the terminal text game (legacy)
./gradlew runTextGame
```

---

## Requirements

- Java 26 installed and available on the PATH or via `JAVA_HOME`

JavaFX dependencies are downloaded automatically through Gradle -- no extra IDE-specific JavaFX module setup is required.

---

## Project Structure

```
JusticeLeague-Spring2026/
+-- src/
|   +-- controller/             # Input handling and game flow
|   |   +-- CombatSystem.java
|   |   +-- GameController.java
|   |   +-- InventoryController.java
|   |   +-- Main.java
|   |   +-- RoomController.java
|   |   +-- SaveLoadTester.java
|   +-- model/                  # Game data and rules
|   |   +-- Armor.java
|   |   +-- Consumable.java
|   |   +-- DicePuzzle.java
|   |   +-- EffectType.java
|   |   +-- FlavorText.java
|   |   +-- Game.java
|   |   +-- Item.java
|   |   +-- KeyItem.java
|   |   +-- Monster.java
|   |   +-- MonsterAbility.java
|   |   +-- NumberGuessPuzzle.java
|   |   +-- Player.java
|   |   +-- PokerPuzzle.java
|   |   +-- Puzzle.java
|   |   +-- PuzzleLoader.java
|   |   +-- RPSPuzzle.java
|   |   +-- RiddlePuzzle.java
|   |   +-- Room.java
|   |   +-- SaveManager.java
|   |   +-- ScramblePuzzle.java
|   |   +-- SelectionPuzzle.java
|   |   +-- StatusEffect.java
|   |   +-- Weapon.java
|   +-- view/                   # Output and UI components
|       +-- GameGUI.java
|       +-- GameView.java
+-- data/                       # CSV data files loaded at runtime
|   +-- armor.csv
|   +-- consumables.csv
|   +-- flavor_text.csv
|   +-- items.csv
|   +-- monster_flavor_text.csv
|   +-- monsters.csv
|   +-- player_data.csv
|   +-- puzzles.csv
|   +-- rooms.csv
|   +-- weapons.csv
+-- saves/                      # Save-game slots (auto-generated)
+-- build.gradle
+-- README.md
```

---

## Class Overview

### Model -- `src/model/`
> Stores game state, data, and rules.

- `Game.java` -- Loads rooms, monsters, items, and puzzles from `data/`; tracks solved puzzles and win conditions.
- `Player.java` -- Tracks location, HP, inventory, equipped gear, and room history. Supports CSV load/save.
- `Room.java` -- Represents a room with directional exits, description, visited state, items, monster, and puzzle.
- `Item.java` -- Base class for all items.
- `Weapon.java` -- Equippable weapon with attack bonus.
- `Armor.java` -- Equippable armor with defense bonus.
- `Consumable.java` -- Usable item that restores HP or applies status effects.
- `KeyItem.java` -- Quest item required to unlock specific doors or win conditions.
- `Monster.java` -- Enemy with HP, attack, defense, abilities, and status effects.
- `MonsterAbility.java` -- Defines special actions a monster can perform in combat.
- `StatusEffect.java` -- Timed effect (buff or debuff) applied to a combatant.
- `EffectType.java` -- Enum of all possible status effect types.
- `Puzzle.java` -- Abstract base for all puzzle types; tracks attempts and result states.
- `PuzzleLoader.java` -- Reads puzzle definitions from `data/puzzles.csv`.
- `DicePuzzle.java` -- Dice-rolling puzzle variant.
- `NumberGuessPuzzle.java` -- Number-guessing puzzle variant.
- `PokerPuzzle.java` -- Card draw poker puzzle variant.
- `RPSPuzzle.java` -- Rock-Paper-Scissors puzzle variant.
- `RiddlePuzzle.java` -- Text riddle puzzle variant.
- `ScramblePuzzle.java` -- Letter-unscramble puzzle variant.
- `SelectionPuzzle.java` -- Multiple-choice selection puzzle variant.
- `FlavorText.java` -- Loads and serves narrative strings by key from `data/flavor_text.csv`.
- `SaveManager.java` -- Handles save/load for up to three named slots using CSV and JSON via Tablesaw and Gson.

### View -- `src/view/`
> Renders output and UI.

- `GameGUI.java` -- Full JavaFX GUI with a three-column layout:
  - **Left panel**: player and room information.
  - **Center panel**: narrative output log with state-driven button panels (main menu, exploration, combat, text puzzles, card puzzles, game over).
  - **Right panel**: interactive map preview and inventory list.
  - Includes save/load dialogs supporting up to three named save slots.
- `GameView.java` -- Terminal-based display methods used by the legacy text game loop.

### Controller -- `src/controller/`
> Coordinates input, model updates, and view output.

- `GameController.java` -- Drives the legacy terminal game loop; interprets movement, combat, inventory, puzzle, and item commands from standard input.
- `Main.java` -- Entry point for the legacy text mode; creates and runs `GameController`.
- `CombatSystem.java` -- Manages turn-based combat between the player and a monster; handles attack, defend, flee, item use, and status-effect ticks. Shared by both the GUI and text-mode controllers.
- `InventoryController.java` -- Helper that validates and executes inventory actions (pick up, drop, equip, unequip, use).
- `RoomController.java` -- Helper for room-entry logic, barricade checks, and movement validation.
- `SaveLoadTester.java` -- Stand-alone harness for testing save/load round-trips; run with `./gradlew runSaveLoadTest`.

---

## Data Files

All runtime data lives in `data/` as CSV files:

| File | Description |
|---|---|
| `rooms.csv` | Room graph: IDs, directional exits, names, descriptions, coordinates |
| `monsters.csv` | Monster stats, abilities, and room assignments |
| `monster_flavor_text.csv` | Narrative text shown when a monster is encountered |
| `puzzles.csv` | Puzzle definitions: type, name, narrative, answer, hints, rewards |
| `items.csv` | Generic item definitions |
| `weapons.csv` | Weapon stats (attack bonus) |
| `armor.csv` | Armor stats (defense bonus) |
| `consumables.csv` | Consumable item effects and heal values |
| `flavor_text.csv` | Keyed narrative strings used throughout the GUI |
| `player_data.csv` | Default player starting stats loaded on new game |

---

## Notes

- `build/` and `.gradle/` are ignored by Git because Gradle handles all build outputs.
- `lib/javafx-sdk-26/` is **not** required; Gradle downloads JavaFX automatically.
- `saves/` is created automatically at runtime and holds up to three named save slots plus a base slot.
- The legacy text mode (`./gradlew runTextGame`) uses `controller.Main` as its entry point and shares the `CombatSystem` and model classes with the GUI.

### Verify Java 26

```powershell
java -version
```

It should report something like `java version "26"`.
