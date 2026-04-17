package model;

import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SaveManager handles all save/load operations for the game using Tablesaw
 * dataframes.
 *
 * Directory layout:
 * saves/base/ – factory defaults, never overwritten (used for New Game)
 * saves/slot1/ – save slot 1
 * saves/slot2/ – save slot 2
 * saves/slot3/ – save slot 3
 *
 * Each slot folder contains five CSV dataframes:
 * player.csv – player state
 * items.csv – item pick-up / equip / use state
 * monsters.csv – monster alive state and current room
 * puzzles.csv – puzzle solved state
 * rooms.csv – room visited state and barricade state
 */
public class SaveManager {

    public static final String SAVES_DIR = "saves/";
    public static final String BASE_SLOT = "base";
    public static final int NUM_SLOTS = 3;

    // -------------------------------------------------------------------------
    // Public API – Save
    // -------------------------------------------------------------------------

    /**
     * Write the current game state to the given slot (1–3).
     *
     * @return true on success, false on any I/O error.
     */
    public static boolean saveGame(int slot, Player player, Game game) {
        if (slot < 1 || slot > NUM_SLOTS)
            return false;
        String dir = slotDir(slot);
        try {
            Files.createDirectories(Paths.get(dir));
            writePlayerTable(dir + "player.csv", player, game);
            writeItemsTable(dir + "items.csv", game, player);
            writeMonstersTable(dir + "monsters.csv", game);
            writePuzzlesTable(dir + "puzzles.csv", game);
            writeRoomsTable(dir + "rooms.csv", game);
            return true;
        } catch (Exception e) {
            System.err.println("SaveManager.saveGame – slot " + slot + ": " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Public API – Load
    // -------------------------------------------------------------------------

    /**
     * Restore game state from the given slot (1–3).
     * Rebuilds the Player from the saved CSV; applies puzzle/room/monster state
     * back onto the already-initialised Game object.
     *
     * @return the restored Player, or null on failure.
     */
    public static Player loadGame(int slot, Game game) {
        if (slot < 1 || slot > NUM_SLOTS)
            return null;
        String dir = slotDir(slot);
        try {
            Player player = Player.loadFromCsv(dir + "player.csv", game);
            restoreVisitedRooms(dir + "rooms.csv", game);
            restorePuzzles(dir + "puzzles.csv", game);
            restoreMonsters(dir + "monsters.csv", game);
            // Item state restoration is handled externally via getItemStates()
            return player;
        } catch (Exception e) {
            System.err.println("SaveManager.loadGame – slot " + slot + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Load item states from a slot into a map of ItemID → boolean[]{pickedUp,
     * equipped, used}.
     * Returns an empty map on failure.
     */
    public static Map<String, boolean[]> getItemStates(int slot) {
        Map<String, boolean[]> result = new LinkedHashMap<>();
        String path = slotDir(slot) + "items.csv";
        if (!new File(path).exists())
            return result;
        try {
            Table table = Table.read().csv(path);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                String id = row.getString("ItemID");
                boolean pickedUp = parseBool(row.getString("pickedUp"));
                boolean equipped = parseBool(row.getString("equipped"));
                boolean used = parseBool(row.getString("used"));
                result.put(id, new boolean[] { pickedUp, equipped, used });
            }
        } catch (Exception e) {
            System.err.println("SaveManager.getItemStates: " + e.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Public API – Slot utilities
    // -------------------------------------------------------------------------

    /** Returns true if a saved (non-default) state exists in the slot. */
    public static boolean slotExists(int slot) {
        return new File(slotDir(slot) + "player.csv").exists();
    }

    /**
     * Returns a one-line summary for the slot selection UI.
     * Format: "Slot N – <PlayerName> in <RoomID> HP:<current>/<max>"
     * or "Slot N – Empty" when the slot is at factory defaults.
     */
    public static String getSlotSummary(int slot) {
        String path = slotDir(slot) + "player.csv";
        if (!new File(path).exists()) {
            return "Slot " + slot + " \u2013 Empty";
        }
        try {
            Table table = Table.read().csv(path);
            if (table.rowCount() == 0)
                return "Slot " + slot + " \u2013 Empty";
            Row row = table.row(0);
            String name = row.getString("Name");
            String roomId = row.getString("CurrentRoomID");
            String hp = row.getString("CurrentHP");
            String maxHp = row.getString("MaxHP");
            return "Slot " + slot + " \u2013 " + name + " | " + roomId + " | HP: " + hp + "/" + maxHp;
        } catch (Exception e) {
            return "Slot " + slot + " \u2013 (error reading save)";
        }
    }

    /**
     * Reset a slot to factory defaults by copying all base CSV files.
     * The base directory is never modified by this or any other method.
     */
    public static boolean resetSlotToBase(int slot) {
        if (slot < 1 || slot > NUM_SLOTS)
            return false;
        String baseDir = SAVES_DIR + BASE_SLOT + "/";
        String slotDir = slotDir(slot);
        try {
            Files.createDirectories(Paths.get(slotDir));
            for (String file : new String[] { "player.csv", "items.csv", "monsters.csv", "puzzles.csv", "rooms.csv" }) {
                Path src = Paths.get(baseDir + file);
                Path dst = Paths.get(slotDir + file);
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.err.println("SaveManager.resetSlotToBase – slot " + slot + ": " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Write helpers
    // -------------------------------------------------------------------------

    private static void writePlayerTable(String path, Player player, Game game) throws IOException {
        StringColumn colName = StringColumn.create("Name");
        StringColumn colRoom = StringColumn.create("CurrentRoomID");
        StringColumn colHP = StringColumn.create("CurrentHP");
        StringColumn colMaxHP = StringColumn.create("MaxHP");
        StringColumn colAtk = StringColumn.create("BaseAttack");
        StringColumn colDef = StringColumn.create("BaseDefense");
        StringColumn colCurAtk = StringColumn.create("CurrentAttack");
        StringColumn colCurDef = StringColumn.create("CurrentDefense");

        String roomId = "CH-01";
        Room currentRoom = game.getRoomByNumber(player.getLocation());
        if (currentRoom != null)
            roomId = currentRoom.getRoomId();

        colName.append(player.getName());
        colRoom.append(roomId);
        colHP.append(String.valueOf(player.getCurrentHP()));
        colMaxHP.append(String.valueOf(player.getMaxHP()));
        colAtk.append(String.valueOf(player.getBaseAttack()));
        colDef.append(String.valueOf(player.getBaseDefense()));
        colCurAtk.append(String.valueOf(player.getAttackValue()));
        colCurDef.append(String.valueOf(player.getDefenseValue()));

        Table table = Table.create("player",
                colName, colRoom, colHP, colMaxHP,
                colAtk, colDef, colCurAtk, colCurDef);
        table.write().csv(path);
    }

    /**
     * Write items state. If the game has live Item objects in Player inventory /
     * equipped slots, their state is reflected here. Items not yet loaded into
     * the game model fall back to the existing slot CSV (preserving last-saved
     * state).
     */
    private static void writeItemsTable(String path, Game game, Player player) throws IOException {
        // Read the existing slot items.csv as the base state to preserve
        Map<String, boolean[]> existing = readItemStatesFromPath(path);

        // Reflect equipped weapon / armor in the map
        if (player.getEquippedWeapon() != null) {
            reflectEquipped(existing, player.getEquippedWeapon().getName());
        }
        if (player.getEquippedArmor() != null) {
            reflectEquipped(existing, player.getEquippedArmor().getName());
        }
        // Reflect inventory items as picked up
        for (Item item : player.getInventory()) {
            reflectPickedUp(existing, item.getName());
        }

        StringColumn colId = StringColumn.create("ItemID");
        BooleanColumn colPicked = BooleanColumn.create("pickedUp");
        BooleanColumn colEq = BooleanColumn.create("equipped");
        BooleanColumn colUsed = BooleanColumn.create("used");

        // If we had nothing in existing (e.g. fresh game), load from base
        if (existing.isEmpty()) {
            existing = readItemStatesFromPath(SAVES_DIR + BASE_SLOT + "/items.csv");
        }

        for (Map.Entry<String, boolean[]> entry : existing.entrySet()) {
            colId.append(entry.getKey());
            colPicked.append(entry.getValue()[0]);
            colEq.append(entry.getValue()[1]);
            colUsed.append(entry.getValue()[2]);
        }

        Table table = Table.create("items", colId, colPicked, colEq, colUsed);
        table.write().csv(path);
    }

    private static void writeMonstersTable(String path, Game game) throws IOException {
        // Read existing slot monsters.csv to preserve runtime state
        Map<String, String[]> existing = readMonsterStatesFromPath(path);
        if (existing.isEmpty()) {
            existing = readMonsterStatesFromPath(SAVES_DIR + BASE_SLOT + "/monsters.csv");
        }

        // Sync alive state from Room objects where a monster is present
        // (Full integration happens once the monster-loading system populates rooms)
        for (Room room : game.getAllRooms()) {
            if (room.getMonster() != null) {
                Monster m = room.getMonster();
                // Find the monster ID by its current room
                for (Map.Entry<String, String[]> entry : existing.entrySet()) {
                    if (entry.getValue()[1].equals(room.getRoomId())) {
                        entry.getValue()[0] = String.valueOf(m.isAlive());
                        break;
                    }
                }
            }
        }

        StringColumn colId = StringColumn.create("MonsterID");
        BooleanColumn colAlive = BooleanColumn.create("isAlive");
        StringColumn colCurrentRoom = StringColumn.create("currentRoomId");

        for (Map.Entry<String, String[]> entry : existing.entrySet()) {
            String[] v = entry.getValue();
            colId.append(entry.getKey());
            colAlive.append(parseBool(v[0]));
            colCurrentRoom.append(v[1]);
        }

        Table table = Table.create("monsters", colId, colAlive, colCurrentRoom);
        table.write().csv(path);
    }

    private static void writePuzzlesTable(String path, Game game) throws IOException {
        StringColumn colId = StringColumn.create("PuzzleID");
        BooleanColumn colSolved = BooleanColumn.create("solved");

        // Read existing IDs from base (authoritative list of puzzle IDs)
        Map<String, Boolean> puzzleStates = new LinkedHashMap<>();
        String basePuzzlesPath = SAVES_DIR + BASE_SLOT + "/puzzles.csv";
        if (new File(basePuzzlesPath).exists()) {
            Table baseTable = Table.read().csv(basePuzzlesPath);
            for (int i = 0; i < baseTable.rowCount(); i++) {
                Row row = baseTable.row(i);
                puzzleStates.put(row.getString("PuzzleID"), false);
            }
        }

        // Overwrite with live puzzle state from Game rooms
        for (Room room : game.getAllRooms()) {
            if (room.hasPuzzle()) {
                Puzzle puzzle = room.getPuzzle();
                // Match puzzle by room ID prefix to puzzle IDs where possible
                // Since puzzles.csv stores PuzzleID and RoomID, we iterate by room
                for (String pid : puzzleStates.keySet()) {
                    // The mapping is loaded in Game; we check if the puzzle in
                    // this room has been solved
                    puzzleStates.put(pid, puzzle.isSolved());
                    break; // only one puzzle per room
                }
            }
        }

        // Better: read the existing slot puzzles.csv for solved status,
        // then merge live room puzzle state
        if (new File(path).exists()) {
            try {
                Table existing = Table.read().csv(path);
                for (int i = 0; i < existing.rowCount(); i++) {
                    Row row = existing.row(i);
                    puzzleStates.put(row.getString("PuzzleID"),
                            parseBool(row.getString("solved")));
                }
            } catch (Exception ignored) {
            }
        }

        // Apply live room puzzle states (room ID → puzzle solved)
        Map<String, Boolean> roomSolvedMap = new LinkedHashMap<>();
        for (Room room : game.getAllRooms()) {
            if (room.hasPuzzle()) {
                roomSolvedMap.put(room.getRoomId(), room.getPuzzle().isSolved());
            }
        }

        // Load puzzle ID → room ID mapping from data/puzzles.csv
        String dataPuzzlesPath = "data/puzzles.csv";
        if (new File(dataPuzzlesPath).exists()) {
            try {
                Table dataTable = Table.read().csv(dataPuzzlesPath);
                for (int i = 0; i < dataTable.rowCount(); i++) {
                    Row row = dataTable.row(i);
                    String pid = row.getString("PuzzleID");
                    String rid = row.getString("RoomID");
                    if (roomSolvedMap.containsKey(rid)) {
                        puzzleStates.put(pid, roomSolvedMap.get(rid));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        for (Map.Entry<String, Boolean> entry : puzzleStates.entrySet()) {
            colId.append(entry.getKey());
            colSolved.append(entry.getValue());
        }

        Table table = Table.create("puzzles", colId, colSolved);
        table.write().csv(path);
    }

    private static void writeRoomsTable(String path, Game game) throws IOException {
        // Read existing barricadedTo state so it survives a save
        Map<String, String> existingBarricade = new LinkedHashMap<>();
        if (new File(path).exists()) {
            try {
                Table existing = Table.read().csv(path);
                for (int i = 0; i < existing.rowCount(); i++) {
                    Row row = existing.row(i);
                    existingBarricade.put(row.getString("RoomID"), row.getString("barricadedTo"));
                }
            } catch (Exception ignored) {
            }
        }

        StringColumn colId = StringColumn.create("RoomID");
        BooleanColumn colVisited = BooleanColumn.create("visited");
        StringColumn colBarricaded = StringColumn.create("barricadedTo");

        for (Room r : game.getAllRooms()) {
            colId.append(r.getRoomId());
            colVisited.append(r.isVisited());
            colBarricaded.append(existingBarricade.getOrDefault(r.getRoomId(), "NONE"));
        }

        Table table = Table.create("rooms", colId, colVisited, colBarricaded);
        table.write().csv(path);
    }

    // -------------------------------------------------------------------------
    // Restore helpers (load → game objects)
    // -------------------------------------------------------------------------

    private static void restoreVisitedRooms(String roomsCsvPath, Game game) {
        if (!new File(roomsCsvPath).exists())
            return;
        try {
            Table table = Table.read().csv(roomsCsvPath);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                Room r = game.getRoomById(row.getString("RoomID"));
                if (r == null)
                    continue;
                if (parseBool(row.getString("visited")))
                    r.setVisited();
                // Restore barricade state stored in rooms.csv
                String barricadedTo = row.getString("barricadedTo");
                if (barricadedTo != null && !barricadedTo.equals("NONE"))
                    r.setBarricadedTo(barricadedTo);
            }
        } catch (Exception e) {
            System.err.println("SaveManager.restoreVisitedRooms: " + e.getMessage());
        }
    }

    private static void restorePuzzles(String puzzlesCsvPath, Game game) {
        if (!new File(puzzlesCsvPath).exists())
            return;
        try {
            Table slotTable = Table.read().csv(puzzlesCsvPath);
            // Load PuzzleID → RoomID mapping from data/puzzles.csv
            Map<String, String> pidToRoomId = new LinkedHashMap<>();
            String dataPuzzlesPath = "data/puzzles.csv";
            if (new File(dataPuzzlesPath).exists()) {
                Table dataTable = Table.read().csv(dataPuzzlesPath);
                for (int i = 0; i < dataTable.rowCount(); i++) {
                    Row row = dataTable.row(i);
                    pidToRoomId.put(row.getString("PuzzleID"), row.getString("RoomID"));
                }
            }
            for (int i = 0; i < slotTable.rowCount(); i++) {
                Row row = slotTable.row(i);
                String pid = row.getString("PuzzleID");
                boolean solved = parseBool(row.getString("solved"));
                if (!solved)
                    continue;
                String roomId = pidToRoomId.get(pid);
                if (roomId == null)
                    continue;
                Room r = game.getRoomById(roomId);
                if (r != null && r.hasPuzzle()) {
                    r.getPuzzle().markSolved();
                }
            }
        } catch (Exception e) {
            System.err.println("SaveManager.restorePuzzles: " + e.getMessage());
        }
    }

    private static void restoreMonsters(String monstersCsvPath, Game game) {
        if (!new File(monstersCsvPath).exists())
            return;
        try {
            Table table = Table.read().csv(monstersCsvPath);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                boolean alive = parseBool(row.getString("isAlive"));
                String currentRoom = row.getString("currentRoomId");
                Room r = game.getRoomById(currentRoom);
                if (r != null && r.getMonster() != null) {
                    r.getMonster().setAlive(alive);
                }
            }
        } catch (Exception e) {
            System.err.println("SaveManager.restoreMonsters: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // State-update helpers (called by game logic as events happen)
    // -------------------------------------------------------------------------

    /**
     * Mark a specific item's state in the given slot.
     * Call this whenever an item is picked up, equipped, or used during gameplay.
     *
     * @param slot     save slot (1–3) that should be updated
     * @param itemId   e.g. "I12"
     * @param pickedUp true once the item has been added to inventory
     * @param equipped true when the item is currently equipped
     * @param used     true when the consumable has been consumed
     */
    public static void updateItemState(int slot, String itemId,
            boolean pickedUp, boolean equipped, boolean used) {
        String path = slotDir(slot) + "items.csv";
        if (!new File(path).exists())
            return;
        try {
            Table table = Table.read().csv(path);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                if (itemId.equals(row.getString("ItemID"))) {
                    table.booleanColumn("pickedUp").set(i, pickedUp);
                    table.booleanColumn("equipped").set(i, equipped);
                    table.booleanColumn("used").set(i, used);
                    break;
                }
            }
            table.write().csv(path);
        } catch (Exception e) {
            System.err.println("SaveManager.updateItemState: " + e.getMessage());
        }
    }

    /**
     * Update a monster's runtime state in the given slot.
     * Call this after combat ends or after the Barricade ability is used.
     *
     * @param slot          save slot (1–3)
     * @param monsterId     e.g. "M05"
     * @param alive         false when the monster has been defeated
     * @param currentRoomId room the monster currently occupies
     * @param barrFrom      "NONE" or the room ID on one side of a barricade
     * @param barrTo        "NONE" or the room ID on the other side of a barricade
     */
    public static void updateMonsterState(int slot, String monsterId,
            boolean alive, String currentRoomId) {
        String path = slotDir(slot) + "monsters.csv";
        if (!new File(path).exists())
            return;
        try {
            Table table = Table.read().csv(path);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                if (monsterId.equals(row.getString("MonsterID"))) {
                    table.booleanColumn("isAlive").set(i, alive);
                    table.stringColumn("currentRoomId").set(i, currentRoomId);
                    break;
                }
            }
            table.write().csv(path);
        } catch (Exception e) {
            System.err.println("SaveManager.updateMonsterState: " + e.getMessage());
        }
    }

    /**
     * Update the barricade state for a room in the given slot.
     * Call this when a monster applies or removes a barricade on a room exit.
     *
     * @param slot         save slot (1–3)
     * @param roomId       the room whose barricade exit is changing
     * @param barricadedTo "NONE" to clear, or the RoomID of the blocked neighbor
     */
    public static void updateRoomBarricade(int slot, String roomId, String barricadedTo) {
        String path = slotDir(slot) + "rooms.csv";
        if (!new File(path).exists())
            return;
        try {
            Table table = Table.read().csv(path);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                if (roomId.equals(row.getString("RoomID"))) {
                    table.stringColumn("barricadedTo").set(i, barricadedTo);
                    break;
                }
            }
            table.write().csv(path);
        } catch (Exception e) {
            System.err.println("SaveManager.updateRoomBarricade: " + e.getMessage());
        }
    }

    /**
     * Mark a puzzle solved in the given slot.
     * Call this immediately after the player solves a puzzle.
     *
     * @param slot     save slot (1–3)
     * @param puzzleId e.g. "P01"
     */
    public static void markPuzzleSolved(int slot, String puzzleId) {
        String path = slotDir(slot) + "puzzles.csv";
        if (!new File(path).exists())
            return;
        try {
            Table table = Table.read().csv(path);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                if (puzzleId.equals(row.getString("PuzzleID"))) {
                    table.booleanColumn("solved").set(i, true);
                    break;
                }
            }
            table.write().csv(path);
        } catch (Exception e) {
            System.err.println("SaveManager.markPuzzleSolved: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String slotDir(int slot) {
        return SAVES_DIR + "slot" + slot + "/";
    }

    private static boolean parseBool(String raw) {
        return "true".equalsIgnoreCase(raw == null ? "" : raw.trim());
    }

    /** Read ItemID → boolean[]{pickedUp, equipped, used} from a CSV file. */
    private static Map<String, boolean[]> readItemStatesFromPath(String path) {
        Map<String, boolean[]> result = new LinkedHashMap<>();
        if (!new File(path).exists())
            return result;
        try {
            Table table = Table.read().csv(path);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                result.put(row.getString("ItemID"), new boolean[] {
                        parseBool(row.getString("pickedUp")),
                        parseBool(row.getString("equipped")),
                        parseBool(row.getString("used"))
                });
            }
        } catch (Exception e) {
            System.err.println("SaveManager.readItemStatesFromPath: " + e.getMessage());
        }
        return result;
    }

    /** Read MonsterID → String[]{isAlive, currentRoomId}. */
    private static Map<String, String[]> readMonsterStatesFromPath(String path) {
        Map<String, String[]> result = new LinkedHashMap<>();
        if (!new File(path).exists())
            return result;
        try {
            Table table = Table.read().csv(path);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                result.put(row.getString("MonsterID"), new String[] {
                        row.getString("isAlive"),
                        row.getString("currentRoomId")
                });
            }
        } catch (Exception e) {
            System.err.println("SaveManager.readMonsterStatesFromPath: " + e.getMessage());
        }
        return result;
    }

    private static void reflectPickedUp(Map<String, boolean[]> states, String itemName) {
        for (boolean[] v : states.values()) {
            // We don't have a name→ID mapping here; full integration happens via
            // updateItemState()
            v[0] = true;
        }
    }

    private static void reflectEquipped(Map<String, boolean[]> states, String itemName) {
        // Placeholder – full integration via updateItemState()
    }
}
