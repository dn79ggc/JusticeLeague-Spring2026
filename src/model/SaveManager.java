package model;

import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.Writer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SaveManager handles all save/load operations for the game using Tablesaw
 * dataframes.
 *
 * @author Thang Pham
 */
public class SaveManager {

    public static String SAVES_DIR = "saves/";
    public static final String BASE_SLOT = "base";
    public static final int NUM_SLOTS = 3;

    static {
        try {
            File d = new File(SAVES_DIR);
            if (d.exists() && d.isDirectory()) {
                SAVES_DIR = d.getCanonicalPath().replace('\\', '/') + "/";
            } else {
                File cur = new File(System.getProperty("user.dir"));
                for (int i = 0; i < 6 && cur != null; i++) {
                    File candidate = new File(cur, "saves");
                    if (candidate.exists() && candidate.isDirectory()) {
                        SAVES_DIR = candidate.getCanonicalPath().replace('\\', '/') + "/";
                        break;
                    }
                    cur = cur.getParentFile();
                }
            }
        } catch (IOException ignored) {
        }
    }

    // -------------------------------------------------------------------------
    // Public API – Save
    // -------------------------------------------------------------------------

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
            // Also write a consolidated JSON save for more robust persistence
            try {
                writeJsonSave(dir + "save.json", player, game);
            } catch (Exception ignored) {
            }

            // Verify files were written and are non-empty
            String[] files = new String[] { "player.csv", "items.csv", "monsters.csv", "puzzles.csv", "rooms.csv" };
            for (String f : files) {
                Path p = Paths.get(dir + f);
                if (!Files.exists(p) || Files.size(p) == 0) {
                    throw new IOException("SaveManager: failed to write " + p.toString());
                }
            }

            System.out.println("SaveManager.saveGame – slot " + slot + ": saved to " + dir);
            return true;
        } catch (Exception e) {
            System.err.println("SaveManager.saveGame – slot " + slot + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Public API – Load
    // -------------------------------------------------------------------------

    public static Player loadGame(int slot, Game game) {
        if (slot < 1 || slot > NUM_SLOTS)
            return null;
        String dir = slotDir(slot);
        try {
            // Re-load base definitions into the provided Game instance so we have a
            // consistent baseline before applying runtime state from the save.
            try {
                game.mapGenerate("rooms.csv");
            } catch (Exception ignored) {
            }
            try {
                game.loadPuzzles("puzzles.csv");
            } catch (Exception ignored) {
            }
            try {
                game.loadMonstersFromCsv("monsters.csv");
            } catch (Exception ignored) {
            }
            try {
                game.loadItemsFromCsv("items.csv", "weapons.csv", "armor.csv", "consumables.csv");
            } catch (Exception ignored) {
            }

            Player player = Player.loadFromCsv(dir + "player.csv", game);
            restoreVisitedRooms(dir + "rooms.csv", game);
            restorePuzzles(dir + "puzzles.csv", game);
            restoreMonsters(dir + "monsters.csv", game);
            restoreItems(dir + "items.csv", game, player);
            return player;
        } catch (Exception e) {
            System.err.println("SaveManager.loadGame – slot " + slot + ": " + e.getMessage());
            return null;
        }
    }

    public static Map<String, boolean[]> getItemStates(int slot) {
        Map<String, boolean[]> result = new LinkedHashMap<>();
        String path = slotDir(slot) + "items.csv";
        if (!new File(path).exists())
            return result;
        try {
            for (Map<String, String> row : readCsvAsStringMaps(path)) {
                String id = row.getOrDefault("ItemID", "");
                boolean pickedUp = parseBool(row.getOrDefault("pickedUp", "false"));
                boolean equipped = parseBool(row.getOrDefault("equipped", "false"));
                boolean used = parseBool(row.getOrDefault("used", "false"));
                result.put(id, new boolean[] { pickedUp, equipped, used });
            }
        } catch (Exception e) {
            System.err.println("SaveManager.getItemStates: " + e.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Slot utilities
    // -------------------------------------------------------------------------

    public static boolean slotExists(int slot) {
        return new File(slotDir(slot) + "player.csv").exists();
    }

    public static String getSlotSummary(int slot) {
        String path = slotDir(slot) + "player.csv";
        if (!new File(path).exists()) {
            return "Slot " + slot + " \u2013 Empty";
        }
        try {
            List<Map<String, String>> rows = readCsvAsStringMaps(path);
            if (rows.isEmpty())
                return "Slot " + slot + " \u2013 Empty";
            Map<String, String> row = rows.get(0);
            String name = row.getOrDefault("Name", "");
            String roomId = row.getOrDefault("CurrentRoomID", "");
            String hp = row.getOrDefault("CurrentHP", "");
            String maxHp = row.getOrDefault("MaxHP", "");
            if (name == null || name.isBlank())
                return "Slot " + slot + " \u2013 Empty";
            return "Slot " + slot + " \u2013 " + name + " | " + roomId + " | HP: " + hp + "/" + maxHp;
        } catch (Exception e) {
            System.err.println("SaveManager.getSlotSummary – slot " + slot + ": " + e.getMessage());
            e.printStackTrace();
            return "Slot " + slot + " \u2013 (error reading save)";
        }
    }

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

    private static void writeItemsTable(String path, Game game, Player player) throws IOException {
        Map<String, String> idToName = new LinkedHashMap<>();
        Map<String, String> idToBaseRoom = new LinkedHashMap<>();
        String basePath = SAVES_DIR + BASE_SLOT + "/items.csv";
        if (!new File(basePath).exists()) {
            basePath = "data/items.csv";
        }
        if (new File(basePath).exists()) {
            try {
                Table base = Table.read().csv(basePath);
                for (int i = 0; i < base.rowCount(); i++) {
                    Row r = base.row(i);
                    String id = "";
                    try {
                        id = r.getString("ItemID");
                    } catch (Exception ignored) {
                    }
                    String name = "";
                    try {
                        name = r.getString("Name");
                    } catch (Exception ignored) {
                    }
                    String room = "";
                    try {
                        room = r.getString("RoomID");
                    } catch (Exception ignored) {
                    }
                    if (id == null || id.isBlank())
                        continue;
                    idToName.put(id, name == null ? "" : name);
                    idToBaseRoom.put(id, room == null ? "" : room);
                }
            } catch (Exception ignored) {
            }
        }

        Map<String, Integer> nameCounts = new LinkedHashMap<>();
        for (Item it : player.getInventory()) {
            String n = it.getName() == null ? "" : it.getName().toLowerCase();
            nameCounts.put(n, nameCounts.getOrDefault(n, 0) + 1);
        }

        StringColumn colId = StringColumn.create("ItemID");
        StringColumn colName = StringColumn.create("Name");
        StringColumn colRoom = StringColumn.create("RoomID");
        BooleanColumn colPicked = BooleanColumn.create("pickedUp");
        BooleanColumn colEq = BooleanColumn.create("equipped");
        BooleanColumn colUsed = BooleanColumn.create("used");
        StringColumn colCount = StringColumn.create("count");

        // Work copy of counts so we assign picked flags deterministically
        Map<String, Integer> remaining = new LinkedHashMap<>(nameCounts);

        // Determine which base ItemIDs correspond to the player's equipped items (if
        // any)
        String equipWeaponId = null;
        String equipArmorId = null;
        String equippedWeaponName = player.getEquippedWeapon() == null ? null
                : player.getEquippedWeapon().getName();
        String equippedArmorName = player.getEquippedArmor() == null ? null
                : player.getEquippedArmor().getName();
        // Helper to normalize names for tolerant matching (remove punctuation,
        // normalize whitespace)
        java.util.function.Function<String, String> normalize = s -> {
            if (s == null)
                return "";
            return s.replaceAll("[^A-Za-z0-9 ]", "").trim().toLowerCase();
        };
        String equippedWeaponNorm = normalize.apply(equippedWeaponName);
        String equippedArmorNorm = normalize.apply(equippedArmorName);

        for (Map.Entry<String, String> e : idToName.entrySet()) {
            String id = e.getKey();
            String nm = e.getValue();
            if (equipWeaponId == null && equippedWeaponName != null && nm != null) {
                if (nm.equalsIgnoreCase(equippedWeaponName) || normalize.apply(nm).equals(equippedWeaponNorm)) {
                    equipWeaponId = id;
                }
            }
            if (equipArmorId == null && equippedArmorName != null && nm != null) {
                if (nm.equalsIgnoreCase(equippedArmorName) || normalize.apply(nm).equals(equippedArmorNorm)) {
                    equipArmorId = id;
                }
            }
            if (equipWeaponId != null && equipArmorId != null)
                break;
        }

        // Build a mapping from base ItemID -> runtime room (if present) using the
        // originalItemId marker set when the Game loads items. Also build a
        // runtime name->room queue as a fallback for items that don't carry an
        // origin id (defensive).
        Map<String, String> originIdToRoom = new java.util.LinkedHashMap<>();
        Map<String, java.util.ArrayDeque<String>> runtimeRoomQueues = new java.util.LinkedHashMap<>();
        if (game != null) {
            for (Room r : game.getAllRooms()) {
                for (Item it : r.getItems()) {
                    if (it == null || it.getName() == null)
                        continue;
                    String n = normalizeName(it.getName());
                    runtimeRoomQueues.computeIfAbsent(n, k -> new java.util.ArrayDeque<>()).add(r.getRoomId());
                    try {
                        String orig = it.getOriginalItemId();
                        if (orig != null && !orig.isBlank()) {
                            originIdToRoom.put(orig, r.getRoomId());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Debug hooks removed: origin->room mapping logging was used during testing

        for (Map.Entry<String, String> e : idToName.entrySet()) {
            String id = e.getKey();
            String name = e.getValue();
            String lname = name == null ? "" : name.toLowerCase();
            int remain = remaining.getOrDefault(lname, 0);
            boolean picked = remain > 0;
            int count = picked ? 1 : 0;
            if (picked) {
                remaining.put(lname, remain - 1);
            }

            boolean equipped = false;
            if ((equipWeaponId != null && equipWeaponId.equals(id))
                    || (equipArmorId != null && equipArmorId.equals(id))) {
                equipped = true;
            }

            String roomId = idToBaseRoom.getOrDefault(id, "");
            if (!picked) {
                // Prefer an exact runtime mapping from original base ItemID -> room (if
                // available)
                if (originIdToRoom != null && originIdToRoom.containsKey(id)) {
                    roomId = originIdToRoom.get(id);
                    // remove from runtime queue to avoid double-assignment
                    if (runtimeRoomQueues != null) {
                        java.util.ArrayDeque<String> q = runtimeRoomQueues.get(lname);
                        if (q != null)
                            q.removeFirstOccurrence(roomId);
                    }
                } else {
                    // Fallback to name-based runtime queues
                    if (runtimeRoomQueues != null) {
                        java.util.ArrayDeque<String> q = runtimeRoomQueues.get(lname);
                        if (q != null && !q.isEmpty()) {
                            String baseRoom = idToBaseRoom.getOrDefault(id, "");
                            if (baseRoom != null && !baseRoom.isBlank() && q.contains(baseRoom)) {
                                q.removeFirstOccurrence(baseRoom);
                                roomId = baseRoom;
                            } else {
                                roomId = q.pollFirst();
                            }
                        }
                    }
                }
                // Fallback: scan rooms for the first matching item if no runtime queue entry
                if ((roomId == null || roomId.isBlank()) && game != null && name != null && !name.isBlank()) {
                    String foundRoom = "";
                    for (Room r : game.getAllRooms()) {
                        for (Item it : r.getItems()) {
                            if (it != null && it.getName() != null && it.getName().equalsIgnoreCase(name)) {
                                foundRoom = r.getRoomId();
                                break;
                            }
                        }
                        if (!foundRoom.isBlank())
                            break;
                    }
                    if (!foundRoom.isBlank())
                        roomId = foundRoom;
                }
            }

            colId.append(id);
            colName.append(name == null ? "" : name);
            colRoom.append(roomId == null ? "" : roomId);
            colPicked.append(picked);
            colEq.append(equipped);
            colUsed.append(false);
            colCount.append(String.valueOf(count));
        }

        // (debug prints removed)

        Table table = Table.create("items", colId, colName, colRoom, colPicked, colEq, colUsed, colCount);
        table.write().csv(path);
    }

    private static void writeMonstersTable(String path, Game game) throws IOException {
        // Build a definitive list of Monster IDs (preserve base order) and
        // write current alive state + current room from the running Game.
        Map<String, String> baseMap = new LinkedHashMap<>();
        String basePath = SAVES_DIR + BASE_SLOT + "/monsters.csv";
        if (new File(basePath).exists()) {
            try {
                for (Map<String, String> r : readCsvAsStringMaps(basePath)) {
                    String id = r.getOrDefault("MonsterID", "").trim();
                    if (id == null || id.isBlank())
                        continue;
                    baseMap.put(id, r.getOrDefault("currentRoomId", ""));
                }
            } catch (Exception ignored) {
            }
        }

        // If baseMap is empty, fall back to provided path
        if (baseMap.isEmpty() && new File(path).exists()) {
            try {
                for (Map<String, String> r : readCsvAsStringMaps(path)) {
                    String id = r.getOrDefault("MonsterID", "").trim();
                    if (id == null || id.isBlank())
                        continue;
                    baseMap.put(id, r.getOrDefault("currentRoomId", ""));
                }
            } catch (Exception ignored) {
            }
        }

        StringColumn colId = StringColumn.create("MonsterID");
        BooleanColumn colAlive = BooleanColumn.create("isAlive");
        StringColumn colCurrentRoom = StringColumn.create("currentRoomId");

        // Write monsters in base order, populating alive + current room from Game
        for (Map.Entry<String, String> e : baseMap.entrySet()) {
            String monsterId = e.getKey();
            Monster m = game.getMonsterById(monsterId);
            boolean alive = true;
            String currentRoom = e.getValue();
            if (m != null) {
                alive = m.isAlive();
                String rId = m.getRoomId();
                if (rId != null && !rId.isBlank())
                    currentRoom = rId;
            }
            colId.append(monsterId);
            colAlive.append(alive);
            colCurrentRoom.append(currentRoom == null ? "" : currentRoom);
        }

        Table table = Table.create("monsters", colId, colAlive, colCurrentRoom);
        table.write().csv(path);
    }

    private static void writePuzzlesTable(String path, Game game) throws IOException {
        StringColumn colId = StringColumn.create("PuzzleID");
        BooleanColumn colSolved = BooleanColumn.create("solved");

        Map<String, String> pidToRoom = new LinkedHashMap<>();
        String dataPuzzlesPath = "data/puzzles.csv";
        if (new File(dataPuzzlesPath).exists()) {
            try {
                for (Map<String, String> row : readCsvAsStringMaps(dataPuzzlesPath)) {
                    String pid = row.getOrDefault("PuzzleID", "").trim();
                    String rid = row.getOrDefault("RoomID", "").trim();
                    if (pid == null || pid.isBlank())
                        continue;
                    pidToRoom.put(pid, rid);
                }
            } catch (Exception ignored) {
            }
        }

        // Start with existing solved flags from the current slot (preserve previous
        // saves)
        Map<String, Boolean> solvedMap = new LinkedHashMap<>();
        for (String pid : pidToRoom.keySet()) {
            solvedMap.put(pid, false);
        }
        if (new File(path).exists()) {
            try {
                for (Map<String, String> row : readCsvAsStringMaps(path)) {
                    String pid = row.getOrDefault("PuzzleID", "").trim();
                    if (pid == null || pid.isBlank())
                        continue;
                    boolean solved = parseBool(row.getOrDefault("solved", "false"));
                    solvedMap.put(pid, solvedMap.getOrDefault(pid, false) || solved);
                }
            } catch (Exception ignored) {
            }
        }

        // Merge runtime solved puzzles from Game (puzzles solved this session)
        if (game != null) {
            try {
                for (String pid : game.getSolvedPuzzles()) {
                    if (pid != null && !pid.isBlank())
                        solvedMap.put(pid, true);
                }
            } catch (Exception ignored) {
            }
        }

        // Also mark puzzles as solved if their room no longer contains the puzzle
        // (e.g., removed via key or completed). This ensures removals persist
        // across saves even if the Game solved registry wasn't updated.
        for (Map.Entry<String, String> entry : pidToRoom.entrySet()) {
            String pid = entry.getKey();
            String roomId = entry.getValue();
            if (roomId != null && !roomId.isBlank()) {
                Room r = game.getRoomById(roomId);
                if (r != null) {
                    try {
                        // If the puzzle object is missing (removed) or already marked solved,
                        // treat it as solved for persistence purposes.
                        if (!r.hasPuzzle() || (r.hasPuzzle() && r.getPuzzle().isSolved())) {
                            solvedMap.put(pid, true);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        for (Map.Entry<String, Boolean> e : solvedMap.entrySet()) {
            colId.append(e.getKey());
            colSolved.append(e.getValue());
        }

        Table table = Table.create("puzzles", colId, colSolved);
        table.write().csv(path);
    }

    private static void writeRoomsTable(String path, Game game) throws IOException {
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
    // Restore helpers
    // -------------------------------------------------------------------------

    private static void restoreVisitedRooms(String roomsCsvPath, Game game) {
        if (!new File(roomsCsvPath).exists())
            return;
        try {
            for (Map<String, String> row : readCsvAsStringMaps(roomsCsvPath)) {
                Room r = game.getRoomById(row.getOrDefault("RoomID", ""));
                if (r == null)
                    continue;
                if (parseBool(row.getOrDefault("visited", "false")))
                    r.setVisited();
                String barricadedTo = row.getOrDefault("barricadedTo", "");
                if (barricadedTo != null && !barricadedTo.equals("NONE") && !barricadedTo.isBlank())
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
            Map<String, String> pidToRoomId = new LinkedHashMap<>();
            String dataPuzzlesPath = "data/puzzles.csv";
            if (new File(dataPuzzlesPath).exists()) {
                try {
                    for (Map<String, String> row : readCsvAsStringMaps(dataPuzzlesPath)) {
                        pidToRoomId.put(row.getOrDefault("PuzzleID", ""), row.getOrDefault("RoomID", ""));
                    }
                } catch (Exception ignored) {
                }
            }

            for (Map<String, String> row : readCsvAsStringMaps(puzzlesCsvPath)) {
                String pid = row.getOrDefault("PuzzleID", "");
                boolean solved = parseBool(row.getOrDefault("solved", "false"));
                if (!solved)
                    continue;
                // Record in game's solved registry so manual saves pick this up
                try {
                    game.addSolvedPuzzle(pid);
                } catch (Exception ignored) {
                }
                String roomId = pidToRoomId.get(pid);
                if (roomId == null)
                    continue;
                Room r = game.getRoomById(roomId);
                if (r != null) {
                    // If the puzzle was solved in the save, remove it from the room so
                    // the UI and runtime don't present it as available.
                    try {
                        if (r.hasPuzzle()) {
                            r.removePuzzle();
                        }
                    } catch (Exception ignored) {
                    }
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
            for (Map<String, String> row : readCsvAsStringMaps(monstersCsvPath)) {
                boolean alive = parseBool(row.getOrDefault("isAlive", "false"));
                String currentRoom = row.getOrDefault("currentRoomId", "");
                Room r = game.getRoomById(currentRoom);
                if (r != null && r.getMonster() != null) {
                    r.getMonster().setAlive(alive);
                }
            }
        } catch (Exception e) {
            System.err.println("SaveManager.restoreMonsters: " + e.getMessage());
        }
    }

    private static void restoreItems(String itemsCsvPath, Game game, Player player) {
        if (!new File(itemsCsvPath).exists() || game == null || player == null)
            return;
        try {
            java.util.List<java.util.Map<String, String>> rows = readCsvAsStringMaps(itemsCsvPath);

            // First, handle equipped items deterministically using ItemID when available.
            for (java.util.Map<String, String> row : rows) {
                String itemId = row.getOrDefault("ItemID", "");
                String name = row.getOrDefault("Name", "");
                String roomId = row.getOrDefault("RoomID", "");
                boolean equipped = parseBool(row.getOrDefault("equipped", "false"));
                if (!equipped)
                    continue;

                Item found = findAndRemoveRoomItemByOriginalId(game, itemId);
                if (found == null && roomId != null && !roomId.isBlank()) {
                    found = findAndRemoveRoomItemByNameInRoom(game, normalizeName(name), roomId);
                }
                if (found == null) {
                    found = findAndRemoveRoomItemByName(game, normalizeName(name));
                }

                Item equipItem = cloneItemForPlayer(found, name);
                if (equipItem instanceof Weapon) {
                    player.equipWeapon((Weapon) equipItem);
                } else if (equipItem instanceof Armor) {
                    player.equipArmor((Armor) equipItem);
                } else {
                    if (!player.addToInventory(equipItem)) {
                        Room r = game.getRoomByNumber(player.getLocation());
                        if (r != null)
                            r.addItem(equipItem);
                    }
                }
            }

            // Then add inventory items by explicit counts using ItemID when possible.
            for (java.util.Map<String, String> row : rows) {
                String itemId = row.getOrDefault("ItemID", "");
                String name = row.getOrDefault("Name", "");
                String roomId = row.getOrDefault("RoomID", "");
                int count = 0;
                try {
                    count = Integer.parseInt(row.getOrDefault("count", "0"));
                } catch (Exception ignored) {
                }
                for (int i = 0; i < Math.max(0, count); i++) {
                    Item found = findAndRemoveRoomItemByOriginalId(game, itemId);
                    if (found == null && roomId != null && !roomId.isBlank()) {
                        found = findAndRemoveRoomItemByNameInRoom(game, normalizeName(name), roomId);
                    }
                    if (found == null) {
                        found = findAndRemoveRoomItemByName(game, normalizeName(name));
                    }
                    Item toGive = cloneItemForPlayer(found, name);
                    if (!player.addToInventory(toGive)) {
                        Room r = game.getRoomByNumber(player.getLocation());
                        if (r != null)
                            r.addItem(toGive);
                    }
                }
            }

            // Finally, ensure room-located (not picked up) prototypes are placed in their
            // saved rooms.
            for (java.util.Map<String, String> row : rows) {
                boolean pickedUp = parseBool(row.getOrDefault("pickedUp", "false"));
                if (pickedUp)
                    continue;
                String itemId = row.getOrDefault("ItemID", "");
                String name = row.getOrDefault("Name", "");
                String savedRoom = row.getOrDefault("RoomID", "");
                if (savedRoom == null || savedRoom.isBlank())
                    continue;

                // Try to find the prototype by original ItemID and move it to the saved room.
                Item proto = findAndRemoveRoomItemByOriginalId(game, itemId);
                if (proto != null) {
                    Room dest = game.getRoomById(savedRoom);
                    if (dest != null)
                        dest.addItem(proto);
                    continue;
                }

                // If not found by ID, try to find any prototype with the same name and move it.
                Item byName = findAndRemoveRoomItemByNameInRoom(game, normalizeName(name), savedRoom);
                if (byName != null) {
                    Room dest = game.getRoomById(savedRoom);
                    if (dest != null)
                        dest.addItem(byName);
                    continue;
                }

                // As a last resort, clone a new prototype into the saved room.
                Item clone = cloneItemForPlayer(null, name);
                Room dest = game.getRoomById(savedRoom);
                if (dest != null)
                    dest.addItem(clone);
            }
        } catch (Exception ex) {
            System.err.println("SaveManager.restoreItems: " + ex.getMessage());
        }
    }

    private static Item cloneItemForPlayer(Item prototype, String fallbackName) {
        if (prototype == null) {
            if (fallbackName == null)
                return null;
            return new Item(fallbackName);
        }
        if (prototype instanceof Weapon w) {
            return new Weapon(w.getName(), w.getDamage(), w.getWeaponType(), w.getMissChance(), w.getSpecialEffect());
        }
        if (prototype instanceof Armor a) {
            return new Armor(a.getName(), a.getDefense());
        }
        if (prototype instanceof Consumable c) {
            return new Consumable(c.getName(), c.getHpEffect(), c.getDefEffect(), c.getDefDuration(), c.getHpPenalty());
        }
        if (prototype instanceof KeyItem k) {
            return new KeyItem(k.getName(), k.getUnlockTarget());
        }
        return new Item(prototype.getName(), prototype.getItemType(), prototype.getRarity(), prototype.getDescription(),
                prototype.getBenefit(), prototype.getWeakness());
    }

    private static Item findAndRemoveRoomItemByName(Game game, String lowerName) {
        if (game == null || lowerName == null)
            return null;
        String target = normalizeName(lowerName);
        for (Room r : game.getAllRooms()) {
            List<Item> items = r.getItems();
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                if (it != null && it.getName() != null) {
                    if (normalizeName(it.getName()).equals(target)) {
                        r.removeItem(it);
                        return it;
                    }
                }
            }
        }
        return null;
    }

    private static Item findAndRemoveRoomItemByNameInRoom(Game game, String lowerName, String roomId) {
        if (game == null || lowerName == null || roomId == null)
            return null;
        Room r = game.getRoomById(roomId);
        if (r == null)
            return null;
        List<Item> items = r.getItems();
        String target = normalizeName(lowerName);
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            if (it != null && it.getName() != null) {
                if (normalizeName(it.getName()).equals(target)) {
                    r.removeItem(it);
                    return it;
                }
            }
        }
        return null;
    }

    private static Item findAndRemoveRoomItemByOriginalId(Game game, String originalId) {
        if (game == null || originalId == null || originalId.isBlank())
            return null;
        for (Room r : game.getAllRooms()) {
            List<Item> items = r.getItems();
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                if (it == null)
                    continue;
                try {
                    String orig = it.getOriginalItemId();
                    if (orig != null && !orig.isBlank() && orig.equalsIgnoreCase(originalId)) {
                        r.removeItem(it);
                        return it;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static Item findRoomItemByOriginalId(Game game, String originalId) {
        if (game == null || originalId == null || originalId.isBlank())
            return null;
        for (Room r : game.getAllRooms()) {
            for (Item it : r.getItems()) {
                if (it == null)
                    continue;
                try {
                    String orig = it.getOriginalItemId();
                    if (orig != null && !orig.isBlank() && orig.equalsIgnoreCase(originalId)) {
                        return it;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // State-update helpers
    // -------------------------------------------------------------------------

    public static void updateItemState(int slot, String itemId, boolean pickedUp, boolean equipped, boolean used) {
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

    public static void updateItemStateByName(int slot, String name, boolean pickedUp, boolean equipped, boolean used) {
        if (name == null || name.isBlank())
            return;
        String path = slotDir(slot) + "items.csv";
        if (!new File(path).exists())
            return;
        try {
            Table table = Table.read().csv(path);
            boolean updated = false;
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                String n = row.getString("Name");
                if (n != null && n.equalsIgnoreCase(name)) {
                    table.booleanColumn("pickedUp").set(i, pickedUp);
                    table.booleanColumn("equipped").set(i, equipped);
                    table.booleanColumn("used").set(i, used);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                for (int i = 0; i < table.rowCount(); i++) {
                    Row row = table.row(i);
                    String id = row.getString("ItemID");
                    if (id != null && id.equalsIgnoreCase(name)) {
                        table.booleanColumn("pickedUp").set(i, pickedUp);
                        table.booleanColumn("equipped").set(i, equipped);
                        table.booleanColumn("used").set(i, used);
                        updated = true;
                        break;
                    }
                }
            }
            table.write().csv(path);
        } catch (Exception e) {
            System.err.println("SaveManager.updateItemStateByName: " + e.getMessage());
        }
    }

    public static void updateMonsterState(int slot, String monsterId, boolean alive, String currentRoomId) {
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

    private static String normalizeName(String s) {
        if (s == null)
            return "";
        return s.replaceAll("[^A-Za-z0-9 ]", "").trim().toLowerCase();
    }

    /**
     * Read a CSV file and return rows as maps of header -> value (all as strings).
     * This is a lightweight fallback parser used to avoid Tablesaw type-casting
     * issues when reading saved CSVs that contain boolean/integer typed columns.
     */
    public static java.util.List<java.util.Map<String, String>> readCsvAsStringMaps(String path) throws IOException {
        // Prefer JSON-based save if present in the same directory as the requested
        // CSV. This allows a more robust save format while keeping the existing
        // CSV-based APIs intact.
        Path p = Paths.get(path);
        Path parent = p.getParent();
        if (parent != null) {
            Path json = parent.resolve("save.json");
            if (Files.exists(json)) {
                try {
                    SaveData sd = readJsonSave(json.toString());
                    String fname = p.getFileName().toString().toLowerCase();
                    java.util.List<java.util.Map<String, String>> rows = new ArrayList<>();
                    if (fname.contains("player")) {
                        java.util.Map<String, String> m = new LinkedHashMap<>();
                        if (sd.player != null) {
                            m.put("Name", sd.player.name == null ? "" : sd.player.name);
                            m.put("CurrentRoomID", sd.player.currentRoomId == null ? "" : sd.player.currentRoomId);
                            m.put("CurrentHP", String.valueOf(sd.player.currentHP));
                            m.put("MaxHP", String.valueOf(sd.player.maxHP));
                            m.put("BaseAttack", String.valueOf(sd.player.baseAttack));
                            m.put("BaseDefense", String.valueOf(sd.player.baseDefense));
                            m.put("CurrentAttack", String.valueOf(sd.player.currentAttack));
                            m.put("CurrentDefense", String.valueOf(sd.player.currentDefense));
                        }
                        rows.add(m);
                        return rows;
                    } else if (fname.contains("items")) {
                        if (sd.items != null) {
                            for (ItemEntry it : sd.items) {
                                java.util.Map<String, String> m = new LinkedHashMap<>();
                                m.put("ItemID", it.itemId == null ? "" : it.itemId);
                                m.put("Name", it.name == null ? "" : it.name);
                                m.put("RoomID", it.roomId == null ? "" : it.roomId);
                                m.put("pickedUp", String.valueOf(it.pickedUp));
                                m.put("equipped", String.valueOf(it.equipped));
                                m.put("used", String.valueOf(it.used));
                                m.put("count", String.valueOf(it.count));
                                rows.add(m);
                            }
                        }
                        return rows;
                    } else if (fname.contains("monsters")) {
                        if (sd.monsters != null) {
                            for (MonsterEntry me : sd.monsters) {
                                java.util.Map<String, String> m = new LinkedHashMap<>();
                                m.put("MonsterID", me.monsterId == null ? "" : me.monsterId);
                                m.put("isAlive", String.valueOf(me.isAlive));
                                m.put("currentRoomId", me.currentRoomId == null ? "" : me.currentRoomId);
                                rows.add(m);
                            }
                        }
                        return rows;
                    } else if (fname.contains("puzzles")) {
                        if (sd.puzzles != null) {
                            for (PuzzleEntry pe : sd.puzzles) {
                                java.util.Map<String, String> m = new LinkedHashMap<>();
                                m.put("PuzzleID", pe.puzzleId == null ? "" : pe.puzzleId);
                                m.put("solved", String.valueOf(pe.solved));
                                rows.add(m);
                            }
                        }
                        return rows;
                    } else if (fname.contains("rooms")) {
                        if (sd.rooms != null) {
                            for (RoomEntry re : sd.rooms) {
                                java.util.Map<String, String> m = new LinkedHashMap<>();
                                m.put("RoomID", re.roomId == null ? "" : re.roomId);
                                m.put("visited", String.valueOf(re.visited));
                                m.put("barricadedTo", re.barricadedTo == null ? "" : re.barricadedTo);
                                rows.add(m);
                            }
                        }
                        return rows;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Fallback: CSV parser (existing behavior)
        java.util.List<String> lines = Files.readAllLines(Paths.get(path));
        java.util.List<java.util.Map<String, String>> rows = new ArrayList<>();
        if (lines == null || lines.isEmpty())
            return rows;
        String headerLine = lines.get(0);
        String[] headers = headerLine.split(",", -1);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank())
                continue;
            String[] vals = line.split(",", -1);
            Map<String, String> map = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++) {
                String key = headers[j].trim();
                if (key.length() >= 2 && key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1).replace("\"\"", "\"");
                }
                String val = j < vals.length ? vals[j].trim() : "";
                if (val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) {
                    val = val.substring(1, val.length() - 1).replace("\"\"", "\"");
                }
                map.put(key, val);
            }
            rows.add(map);
        }
        return rows;
    }

    // JSON save/load helper classes and methods
    private static class SaveData {
        public PlayerEntry player;
        public List<ItemEntry> items = new ArrayList<>();
        public List<MonsterEntry> monsters = new ArrayList<>();
        public List<PuzzleEntry> puzzles = new ArrayList<>();
        public List<RoomEntry> rooms = new ArrayList<>();
    }

    private static class PlayerEntry {
        public String name;
        public String currentRoomId;
        public int currentHP;
        public int maxHP;
        public int baseAttack;
        public int baseDefense;
        public int currentAttack;
        public int currentDefense;
    }

    private static class ItemEntry {
        public String itemId;
        public String name;
        public String roomId;
        public boolean pickedUp;
        public boolean equipped;
        public boolean used;
        public int count;
    }

    private static class MonsterEntry {
        public String monsterId;
        public boolean isAlive;
        public String currentRoomId;
    }

    private static class PuzzleEntry {
        public String puzzleId;
        public boolean solved;
    }

    private static class RoomEntry {
        public String roomId;
        public boolean visited;
        public String barricadedTo;
    }

    private static SaveData readJsonSave(String jsonPath) throws IOException {
        String json = Files.readString(Paths.get(jsonPath));
        Gson g = new Gson();
        SaveData sd = g.fromJson(json, SaveData.class);
        if (sd == null)
            sd = new SaveData();
        return sd;
    }

    private static void writeJsonSave(String jsonPath, Player player, Game game) throws IOException {
        SaveData sd = new SaveData();

        // Player
        PlayerEntry pe = new PlayerEntry();
        pe.name = player.getName();
        Room currentRoom = game.getRoomByNumber(player.getLocation());
        pe.currentRoomId = currentRoom == null ? "" : currentRoom.getRoomId();
        pe.currentHP = player.getCurrentHP();
        pe.maxHP = player.getMaxHP();
        pe.baseAttack = player.getBaseAttack();
        pe.baseDefense = player.getBaseDefense();
        pe.currentAttack = player.getAttackValue();
        pe.currentDefense = player.getDefenseValue();
        sd.player = pe;

        // Items (reuse existing CSV-building logic for deterministic mapping)
        Map<String, String> idToName = new LinkedHashMap<>();
        Map<String, String> idToBaseRoom = new LinkedHashMap<>();
        String basePath = SAVES_DIR + BASE_SLOT + "/items.csv";
        if (!new File(basePath).exists()) {
            basePath = "data/items.csv";
        }
        if (new File(basePath).exists()) {
            try {
                Table base = Table.read().csv(basePath);
                for (int i = 0; i < base.rowCount(); i++) {
                    Row r = base.row(i);
                    String id = "";
                    try {
                        id = r.getString("ItemID");
                    } catch (Exception ignored) {
                    }
                    String name = "";
                    try {
                        name = r.getString("Name");
                    } catch (Exception ignored) {
                    }
                    String room = "";
                    try {
                        room = r.getString("RoomID");
                    } catch (Exception ignored) {
                    }
                    if (id == null || id.isBlank())
                        continue;
                    idToName.put(id, name == null ? "" : name);
                    idToBaseRoom.put(id, room == null ? "" : room);
                }
            } catch (Exception ignored) {
            }
        }

        Map<String, Integer> nameCounts = new LinkedHashMap<>();
        for (Item it : player.getInventory()) {
            String n = it.getName() == null ? "" : it.getName().toLowerCase();
            nameCounts.put(n, nameCounts.getOrDefault(n, 0) + 1);
        }

        // Determine equipped IDs
        String equipWeaponId = null;
        String equipArmorId = null;
        String equippedWeaponName = player.getEquippedWeapon() == null ? null
                : player.getEquippedWeapon().getName();
        String equippedArmorName = player.getEquippedArmor() == null ? null
                : player.getEquippedArmor().getName();
        java.util.function.Function<String, String> normalize = s -> {
            if (s == null)
                return "";
            return s.replaceAll("[^A-Za-z0-9 ]", "").trim().toLowerCase();
        };
        String equippedWeaponNorm = normalize.apply(equippedWeaponName);
        String equippedArmorNorm = normalize.apply(equippedArmorName);

        for (Map.Entry<String, String> e : idToName.entrySet()) {
            String id = e.getKey();
            String nm = e.getValue();
            if (equipWeaponId == null && equippedWeaponName != null && nm != null) {
                if (nm.equalsIgnoreCase(equippedWeaponName) || normalize.apply(nm).equals(equippedWeaponNorm)) {
                    equipWeaponId = id;
                }
            }
            if (equipArmorId == null && equippedArmorName != null && nm != null) {
                if (nm.equalsIgnoreCase(equippedArmorName) || normalize.apply(nm).equals(equippedArmorNorm)) {
                    equipArmorId = id;
                }
            }
            if (equipWeaponId != null && equipArmorId != null)
                break;
        }

        // Build origin -> room map and runtime queues
        Map<String, String> originIdToRoom = new java.util.LinkedHashMap<>();
        Map<String, java.util.ArrayDeque<String>> runtimeRoomQueues = new java.util.LinkedHashMap<>();
        if (game != null) {
            for (Room r : game.getAllRooms()) {
                for (Item it : r.getItems()) {
                    if (it == null || it.getName() == null)
                        continue;
                    String n = normalizeName(it.getName());
                    runtimeRoomQueues.computeIfAbsent(n, k -> new java.util.ArrayDeque<>()).add(r.getRoomId());
                    try {
                        String orig = it.getOriginalItemId();
                        if (orig != null && !orig.isBlank()) {
                            originIdToRoom.put(orig, r.getRoomId());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Build item entries
        Map<String, Integer> remaining = new LinkedHashMap<>(nameCounts);
        for (Map.Entry<String, String> e : idToName.entrySet()) {
            String id = e.getKey();
            String name = e.getValue();
            String lname = normalizeName(name == null ? "" : name);
            int remain = remaining.getOrDefault(lname, 0);
            boolean picked = remain > 0;
            int count = picked ? 1 : 0;
            if (picked) {
                remaining.put(lname, remain - 1);
            }

            boolean equipped = false;
            if ((equipWeaponId != null && equipWeaponId.equals(id))
                    || (equipArmorId != null && equipArmorId.equals(id))) {
                equipped = true;
            }

            String roomId = idToBaseRoom.getOrDefault(id, "");
            if (!picked) {
                // Prefer mapping from original ItemID -> runtime room when available
                if (originIdToRoom != null && originIdToRoom.containsKey(id)) {
                    roomId = originIdToRoom.get(id);
                    if (runtimeRoomQueues != null) {
                        java.util.ArrayDeque<String> q = runtimeRoomQueues.get(lname);
                        if (q != null)
                            q.removeFirstOccurrence(roomId);
                    }
                } else {
                    if (runtimeRoomQueues != null) {
                        java.util.ArrayDeque<String> q = runtimeRoomQueues.get(lname);
                        if (q != null && !q.isEmpty()) {
                            String baseRoom = idToBaseRoom.getOrDefault(id, "");
                            if (baseRoom != null && !baseRoom.isBlank() && q.contains(baseRoom)) {
                                q.removeFirstOccurrence(baseRoom);
                                roomId = baseRoom;
                            } else {
                                roomId = q.pollFirst();
                            }
                        }
                    }
                }
                if ((roomId == null || roomId.isBlank()) && game != null && name != null && !name.isBlank()) {
                    String foundRoom = "";
                    for (Room r : game.getAllRooms()) {
                        for (Item it : r.getItems()) {
                            if (it != null && it.getName() != null && it.getName().equalsIgnoreCase(name)) {
                                foundRoom = r.getRoomId();
                                break;
                            }
                        }
                        if (!foundRoom.isBlank())
                            break;
                    }
                    if (!foundRoom.isBlank())
                        roomId = foundRoom;
                }
            }

            ItemEntry ie = new ItemEntry();
            ie.itemId = id;
            ie.name = name == null ? "" : name;
            ie.roomId = roomId == null ? "" : roomId;
            ie.pickedUp = picked;
            ie.equipped = equipped;
            ie.used = false;
            ie.count = count;
            sd.items.add(ie);
        }

        // Monsters
        Map<String, String> baseMap = new LinkedHashMap<>();
        String baseMonsters = SAVES_DIR + BASE_SLOT + "/monsters.csv";
        if (new File(baseMonsters).exists()) {
            try {
                for (Map<String, String> r : readCsvAsStringMaps(baseMonsters)) {
                    String id = r.getOrDefault("MonsterID", "").trim();
                    if (id == null || id.isBlank())
                        continue;
                    baseMap.put(id, r.getOrDefault("currentRoomId", ""));
                }
            } catch (Exception ignored) {
            }
        }
        if (baseMap.isEmpty() && new File("data/monsters.csv").exists()) {
            try {
                for (Map<String, String> r : readCsvAsStringMaps("data/monsters.csv")) {
                    String id = r.getOrDefault("MonsterID", "").trim();
                    if (id == null || id.isBlank())
                        continue;
                    baseMap.put(id, r.getOrDefault("currentRoomId", ""));
                }
            } catch (Exception ignored) {
            }
        }
        for (Map.Entry<String, String> e : baseMap.entrySet()) {
            String monsterId = e.getKey();
            Monster m = game.getMonsterById(monsterId);
            boolean alive = true;
            String monsterCurrentRoom = e.getValue();
            if (m != null) {
                alive = m.isAlive();
                String rId = m.getRoomId();
                if (rId != null && !rId.isBlank())
                    monsterCurrentRoom = rId;
            }
            MonsterEntry me = new MonsterEntry();
            me.monsterId = monsterId;
            me.isAlive = alive;
            me.currentRoomId = monsterCurrentRoom == null ? "" : monsterCurrentRoom;
            sd.monsters.add(me);
        }

        // Puzzles
        Map<String, String> pidToRoom = new LinkedHashMap<>();
        String dataPuzzlesPath = "data/puzzles.csv";
        if (new File(dataPuzzlesPath).exists()) {
            try {
                for (Map<String, String> row : readCsvAsStringMaps(dataPuzzlesPath)) {
                    String pid = row.getOrDefault("PuzzleID", "").trim();
                    String rid = row.getOrDefault("RoomID", "").trim();
                    if (pid == null || pid.isBlank())
                        continue;
                    pidToRoom.put(pid, rid);
                }
            } catch (Exception ignored) {
            }
        }
        Map<String, Boolean> solvedMap = new LinkedHashMap<>();
        for (String pid : pidToRoom.keySet()) {
            solvedMap.put(pid, false);
        }
        String puzzlesPath = SAVES_DIR + BASE_SLOT + "/puzzles.csv";
        if (new File(puzzlesPath).exists()) {
            try {
                for (Map<String, String> row : readCsvAsStringMaps(puzzlesPath)) {
                    String pid = row.getOrDefault("PuzzleID", "").trim();
                    if (pid == null || pid.isBlank())
                        continue;
                    boolean solved = parseBool(row.getOrDefault("solved", "false"));
                    solvedMap.put(pid, solvedMap.getOrDefault(pid, false) || solved);
                }
            } catch (Exception ignored) {
            }
        }
        if (game != null) {
            try {
                for (String pid : game.getSolvedPuzzles()) {
                    if (pid != null && !pid.isBlank())
                        solvedMap.put(pid, true);
                }
            } catch (Exception ignored) {
            }
        }
        for (Map.Entry<String, String> entry : pidToRoom.entrySet()) {
            String pid = entry.getKey();
            String roomId = entry.getValue();
            if (roomId != null && !roomId.isBlank()) {
                Room r = game.getRoomById(roomId);
                if (r != null) {
                    try {
                        if (!r.hasPuzzle() || (r.hasPuzzle() && r.getPuzzle().isSolved())) {
                            solvedMap.put(pid, true);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        for (Map.Entry<String, Boolean> e : solvedMap.entrySet()) {
            PuzzleEntry pe2 = new PuzzleEntry();
            pe2.puzzleId = e.getKey();
            pe2.solved = e.getValue();
            sd.puzzles.add(pe2);
        }

        // Rooms
        Map<String, String> existingBarricade = new LinkedHashMap<>();
        String roomsJsonBase = SAVES_DIR + BASE_SLOT + "/rooms.csv";
        if (new File(roomsJsonBase).exists()) {
            try {
                Table existing = Table.read().csv(roomsJsonBase);
                for (int i = 0; i < existing.rowCount(); i++) {
                    Row row = existing.row(i);
                    existingBarricade.put(row.getString("RoomID"), row.getString("barricadedTo"));
                }
            } catch (Exception ignored) {
            }
        }
        for (Room r : game.getAllRooms()) {
            RoomEntry re = new RoomEntry();
            re.roomId = r.getRoomId();
            re.visited = r.isVisited();
            re.barricadedTo = existingBarricade.getOrDefault(r.getRoomId(), "NONE");
            sd.rooms.add(re);
        }

        // Write JSON
        Gson g = new GsonBuilder().setPrettyPrinting().create();
        try (Writer w = new FileWriter(jsonPath)) {
            g.toJson(sd, w);
        }
    }

    private static Map<String, boolean[]> readItemStatesFromPath(String path) {
        Map<String, boolean[]> result = new LinkedHashMap<>();
        if (!new File(path).exists())
            return result;
        try {
            for (Map<String, String> row : readCsvAsStringMaps(path)) {
                String id = row.getOrDefault("ItemID", "").trim();
                if (id == null || id.isBlank())
                    continue;
                result.put(id, new boolean[] {
                        parseBool(row.getOrDefault("pickedUp", "false")),
                        parseBool(row.getOrDefault("equipped", "false")),
                        parseBool(row.getOrDefault("used", "false"))
                });
            }
        } catch (Exception e) {
            System.err.println("SaveManager.readItemStatesFromPath: " + e.getMessage());
        }
        return result;
    }

    private static Map<String, String[]> readMonsterStatesFromPath(String path) {
        Map<String, String[]> result = new LinkedHashMap<>();
        if (!new File(path).exists())
            return result;
        try {
            for (Map<String, String> row : readCsvAsStringMaps(path)) {
                String id = row.getOrDefault("MonsterID", "").trim();
                if (id == null || id.isBlank())
                    continue;
                result.put(id, new String[] {
                        row.getOrDefault("isAlive", "false"),
                        row.getOrDefault("currentRoomId", "")
                });
            }
        } catch (Exception e) {
            System.err.println("SaveManager.readMonsterStatesFromPath: " + e.getMessage());
        }
        return result;
    }

}
