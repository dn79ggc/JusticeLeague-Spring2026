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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SaveManager handles all save/load operations for the game using Tablesaw
 * dataframes.
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
            System.err.println("SaveManager.getSlotSummary – slot " + slot + ": " + e.getMessage());
            e.printStackTrace();

            // Fallback: try a simple, resilient CSV parse of the first data row.
            try {
                java.util.List<String> lines = Files.readAllLines(Paths.get(path));
                if (lines.size() < 2)
                    return "Slot " + slot + " \u2013 Empty";
                String header = lines.get(0);
                String values = lines.get(1);
                String[] cols = header.split(",");
                String[] vals = values.split(",", -1);
                java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
                for (int i = 0; i < Math.min(cols.length, vals.length); i++) {
                    map.put(cols[i].trim(), vals[i].trim());
                }
                String name = map.getOrDefault("Name", "");
                String roomId = map.getOrDefault("CurrentRoomID", "");
                String hp = map.getOrDefault("CurrentHP", "");
                String maxHp = map.getOrDefault("MaxHP", "");
                if (name == null || name.isBlank())
                    return "Slot " + slot + " \u2013 Empty";
                return "Slot " + slot + " \u2013 " + name + " | " + roomId + " | HP: " + hp + "/" + maxHp;
            } catch (Exception ex2) {
                System.err.println("SaveManager.getSlotSummary fallback failed: " + ex2.getMessage());
                ex2.printStackTrace();
                return "Slot " + slot + " \u2013 (error reading save)";
            }
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

        for (Map.Entry<String, String> e : idToName.entrySet()) {
            String id = e.getKey();
            String name = e.getValue();
            String lname = name == null ? "" : name.toLowerCase();
            boolean picked = nameCounts.containsKey(lname);
            boolean equipped = false;
            int count = nameCounts.getOrDefault(lname, 0);
            if (player.getEquippedWeapon() != null && player.getEquippedWeapon().getName().equalsIgnoreCase(name)) {
                equipped = true;
            }
            if (player.getEquippedArmor() != null && player.getEquippedArmor().getName().equalsIgnoreCase(name)) {
                equipped = true;
            }

            String roomId = idToBaseRoom.getOrDefault(id, "");
            if (!picked && game != null && name != null && !name.isBlank()) {
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

            colId.append(id);
            colName.append(name == null ? "" : name);
            colRoom.append(roomId == null ? "" : roomId);
            colPicked.append(picked);
            colEq.append(equipped);
            colUsed.append(false);
            colCount.append(String.valueOf(count));
        }

        Table table = Table.create("items", colId, colName, colRoom, colPicked, colEq, colUsed, colCount);
        table.write().csv(path);
    }

    private static void writeMonstersTable(String path, Game game) throws IOException {
        Map<String, String[]> existing = readMonsterStatesFromPath(path);
        if (existing.isEmpty()) {
            existing = readMonsterStatesFromPath(SAVES_DIR + BASE_SLOT + "/monsters.csv");
        }

        for (Room room : game.getAllRooms()) {
            if (room.getMonster() != null) {
                Monster m = room.getMonster();
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

        Map<String, Boolean> puzzleStates = new LinkedHashMap<>();
        String basePuzzlesPath = SAVES_DIR + BASE_SLOT + "/puzzles.csv";
        if (new File(basePuzzlesPath).exists()) {
            Table baseTable = Table.read().csv(basePuzzlesPath);
            for (int i = 0; i < baseTable.rowCount(); i++) {
                Row row = baseTable.row(i);
                puzzleStates.put(row.getString("PuzzleID"), false);
            }
        }

        for (Room room : game.getAllRooms()) {
            if (room.hasPuzzle()) {
                Puzzle puzzle = room.getPuzzle();
                for (String pid : puzzleStates.keySet()) {
                    puzzleStates.put(pid, puzzle.isSolved());
                    break;
                }
            }
        }

        if (new File(path).exists()) {
            try {
                Table existing = Table.read().csv(path);
                for (int i = 0; i < existing.rowCount(); i++) {
                    Row row = existing.row(i);
                    puzzleStates.put(row.getString("PuzzleID"), parseBool(row.getString("solved")));
                }
            } catch (Exception ignored) {
            }
        }

        Map<String, Boolean> roomSolvedMap = new LinkedHashMap<>();
        for (Room room : game.getAllRooms()) {
            if (room.hasPuzzle()) {
                roomSolvedMap.put(room.getRoomId(), room.getPuzzle().isSolved());
            }
        }

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
            Table table = Table.read().csv(roomsCsvPath);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                Room r = game.getRoomById(row.getString("RoomID"));
                if (r == null)
                    continue;
                if (parseBool(row.getString("visited")))
                    r.setVisited();
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
            Map<String, String> pidToRoomId = new LinkedHashMap<>();
            String dataPuzzlesPath = "data/puzzles.csv";
            if (new File(dataPuzzlesPath).exists()) {
                try {
                    Table dataTable = Table.read().csv(dataPuzzlesPath);
                    for (int i = 0; i < dataTable.rowCount(); i++) {
                        Row row = dataTable.row(i);
                        pidToRoomId.put(row.getString("PuzzleID"), row.getString("RoomID"));
                    }
                } catch (Exception ignored) {
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

    private static void restoreItems(String itemsCsvPath, Game game, Player player) {
        if (!new File(itemsCsvPath).exists() || game == null || player == null)
            return;
        try {
            Table table = Table.read().csv(itemsCsvPath);

            Map<String, Integer> nameToCount = new LinkedHashMap<>();
            Map<String, Boolean> nameToEquipped = new LinkedHashMap<>();
            Map<String, String> nameToRoomId = new LinkedHashMap<>();

            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                String name = null;
                try {
                    name = row.getString("Name");
                } catch (Exception ignored) {
                }
                if (name == null)
                    continue;
                String key = name.trim();
                if (key.isEmpty())
                    continue;
                if (nameToCount.containsKey(key.toLowerCase()))
                    continue;

                int count = 0;
                try {
                    String raw = row.getString("count");
                    count = raw == null ? 0 : Integer.parseInt(raw.trim());
                } catch (Exception ignored) {
                }
                boolean equipped = parseBool(row.getString("equipped"));
                String roomId = null;
                try {
                    String rawRoom = row.getString("RoomID");
                    if (rawRoom != null && !rawRoom.isBlank())
                        roomId = rawRoom.trim();
                } catch (Exception ignored) {
                }

                nameToCount.put(key.toLowerCase(), Math.max(0, count));
                nameToEquipped.put(key.toLowerCase(), equipped);
                nameToRoomId.put(key.toLowerCase(), roomId);
            }

            for (Map.Entry<String, Integer> e : nameToCount.entrySet()) {
                String lname = e.getKey();
                int count = e.getValue();
                boolean equip = nameToEquipped.getOrDefault(lname, false);
                String displayName = null;

                for (int i = 0; i < count; i++) {
                    String preferredRoom = nameToRoomId.get(lname);
                    Item found = null;
                    if (preferredRoom != null && !preferredRoom.isBlank()) {
                        found = findAndRemoveRoomItemByNameInRoom(game, lname, preferredRoom);
                    }
                    if (found == null) {
                        found = findAndRemoveRoomItemByName(game, lname);
                    }
                    Item toGive = cloneItemForPlayer(found, lname);
                    if (!player.addToInventory(toGive)) {
                        Room r = game.getRoomByNumber(player.getLocation());
                        if (r != null)
                            r.addItem(toGive);
                    }
                    if (displayName == null && toGive != null)
                        displayName = toGive.getName();
                }

                if (equip) {
                    String preferredRoom = nameToRoomId.get(lname);
                    Item found = null;
                    if (preferredRoom != null && !preferredRoom.isBlank()) {
                        found = findAndRemoveRoomItemByNameInRoom(game, lname, preferredRoom);
                    }
                    if (found == null) {
                        found = findAndRemoveRoomItemByName(game, lname);
                    }
                    Item equipItem = cloneItemForPlayer(found, lname);
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
                    if (displayName == null && equipItem != null)
                        displayName = equipItem.getName();
                }
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
        for (Room r : game.getAllRooms()) {
            List<Item> items = r.getItems();
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                if (it != null && it.getName() != null && it.getName().equalsIgnoreCase(lowerName)) {
                    r.removeItem(it);
                    return it;
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
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            if (it != null && it.getName() != null && it.getName().equalsIgnoreCase(lowerName)) {
                r.removeItem(it);
                return it;
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
}
