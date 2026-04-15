# JusticeLeague-Spring2026

This repository contains a Java room-exploration game with both a terminal text mode and a separate JavaFX GUI shell.

The project uses a simple MVC architecture to separate game state, display, and input handling.

---

## Quick Start

From the project root:

```powershell
# Compile the text game
javac -d bin src/model/*.java src/view/GameView.java src/controller/*.java

# Run the text mode game
java -cp bin controller.Main
```

```powershell
# Compile the JavaFX GUI
javac --module-path lib/javafx-sdk-26/lib --add-modules javafx.controls,javafx.fxml,javafx.media -d bin src/view/GameGUI.java

# Run the JavaFX GUI
java --module-path lib/javafx-sdk-26/lib --add-modules javafx.controls,javafx.fxml,javafx.media -cp bin view.GameGUI
```

---

## Requirements

- Java 26
- JavaFX 26 for the GUI mode

This repo is configured to use `${env:JAVA_HOME}` so the shared workspace is not tied to one machine.

### Verify Java 26

```powershell
java -version
```

It should report something like `java version "26"`.

---

## Project Structure

```
JusticeLeague-Spring2026/
+-- lib/                 # JavaFX SDK and support files
¦   +-- javafx-sdk-26/
+-- src/
¦   +-- controller/      # Input handling and game flow
¦   ¦   +-- GameController.java
¦   ¦   +-- Main.java
¦   +-- model/           # Game data and rules
¦   ¦   +-- Game.java
¦   ¦   +-- Player.java
¦   ¦   +-- Room.java
¦   +-- view/            # Output and UI components
¦       +-- GameGUI.java
¦       +-- GameView.java
+-- data/                # Map files and game data
¦   +-- Rooms.txt
¦   +-- Rooms1.txt
¦   +-- Rooms2.txt
+-- bin/                 # Compiled .class files (auto-generated)
+-- README.md
```

---

## Class Overview

### Model — `src/model/`
> Stores game state and movement rules.

- `Room.java` — Represents a room with exits, a description, and visited state.
- `Player.java` — Tracks the player's location, moves, and rooms discovered.
- `Game.java` — Loads the room map from `data/`, returns room data, and checks win conditions.

### View — `src/view/`
> Renders output and UI.

- `GameView.java` — Terminal-based display methods for the text game.
- `GameGUI.java` — JavaFX-based UI shell with buttons and status panels.

### Controller — `src/controller/`
> Coordinates input, model updates, and view output.

- `GameController.java` — Runs the text game loop and interprets movement commands.
- `Main.java` — Application entry point for text mode.

---

## Data Format

Each line in `data/*.txt` describes one room:

```
roomNumber,north,east,south,west,description
```

- `roomNumber` — unique integer ID (1-based)
- `north/east/south/west` — room number in that direction, `0` if no exit
- `description` — short label for the room

**Example:**

```
1,0,2,0,0,Start
2,3,4,5,1,Crosspaths
```

---

## Notes

- `bin/` contains generated class files and is ignored by Git.
- `lib/javafx-sdk-26/` provides the JavaFX runtime for the GUI.
- The text mode and GUI mode are separate entry points.
