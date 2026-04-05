# Text Explorer MVC Base

A terminal-based text adventure game written in Java. The player navigates between rooms by entering directional commands. The goal is to discover every room on the map.

Built with an **MVC (Model-View-Controller)** architecture to keep game logic, display, and input handling cleanly separated — and easy to extend.

---

## Quick Start

From the project root (`text-explorer/`):

```bash
# Compile
javac -d bin src/model/Room.java src/model/Player.java src/model/Game.java src/view/GameView.java src/controller/GameController.java

# Run
java -cp bin controller.GameController
```

**Controls:** `n` `e` `s` `w` to move, `q` to quit.

---

## Project Structure

```
text-explorer/
├── src/
│   ├── model/          # Game data and logic
│   │   ├── Room.java
│   │   ├── Player.java
│   │   └── Game.java
│   ├── view/           # All output to the user
│   │   └── GameView.java
│   └── controller/     # Input handling and coordination
│       └── GameController.java
├── data/               # External data files
│   ├── Rooms.txt       # Default map
│   ├── Rooms1.txt
│   └── Rooms2.txt
└── bin/                # Compiled .class files (auto-generated)
```

---

## Class Overview

### Model — `src/model/`
> Holds all game data and rules. No printing, no user input. These classes do not depend on View or Controller.

**`Room.java`**
Represents a single room on the map. Stores the room's number, name, description, the four directional exits (N/E/S/W as neighboring room numbers, 0 if no exit), and whether the player has visited it. Exposes exits only through `getExit()` — the raw array is private.

**`Player.java`**
Tracks the player's current location, total moves made, and number of unique rooms visited. Contains `attemptMove()`, which asks the current room if an exit exists and updates the player's location if it does. Movement logic lives here, not in the Controller.

**`Game.java`**
Owns the full list of `Room` objects (the map). Loads room data from a file in `data/` via `mapGenerate(filename)`, returns `true/false` so the Controller can decide what to show. Provides `getRoomByNumber()` to centralize index math, and `allRoomsVisited()` to check win condition.

---

### View — `src/view/`
> Handles all output to the terminal. Knows nothing about game rules. Swap this class to change how the game looks without touching anything else.

**`GameView.java`**
One method per distinct message the game can display — welcome screen, movement prompt, room discovery, revisit notice, invalid input, win screen, goodbye, and load errors. The Controller calls these methods; the View just prints.

---

### Controller — `src/controller/`
> Sits between Model and View. Reads user input, tells the Model what to do, and tells the View what to display.

**`GameController.java`**
Contains `main()` and drives the game loop. Validates and translates raw character input (`n/e/s/w/q`) into direction indexes, calls `Player.attemptMove()`, and routes the result to the appropriate `GameView` method. Also holds the `MAP_FILE` constant — change it here to load a different map.

---

## Data Format — `data/*.txt`

Each line defines one room:

```
roomNumber,north,east,south,west,description
```

- `roomNumber` — unique integer ID for the room (1-based)
- `north/east/south/west` — room number in that direction, `0` if no exit
- `description` — short label shown when the player discovers the room

**Example:**
```
1,0,2,0,0,Start
2,3,4,5,1,Crosspaths
```
Room 1 has only one exit — east to Room 2. Room 2 connects north (3), east (4), south (5), and west back to Room 1.

---

## MVC Dependency Rule

```
Controller  →  Model   ✅
View        →  Model   ✅
Controller  →  View    ✅
Model       →  View    ❌  never
Model       →  Controller  ❌  never
```

When adding new classes (e.g. `Item`, `Monster`, `Puzzle`), place them in `model/`. Managers that coordinate between objects (e.g. `CombatManager`) belong in `controller/`. New display methods belong in `view/`.
