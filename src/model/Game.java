package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

/**
 * Main game state manager.
 * Loads rooms, monsters, items, and puzzles from data files;
 * tracks solved puzzles and win conditions.
 * 
 * @author Thang Pham
 */
public class Game {
    // All data files live in the data/ folder at the project root.
    private static final String DATA_DIR = "data/";
    private static final String MONSTER_FLAVOR_FILE = "monster_flavor_text.csv";
    private static final Pattern FIRST_INTEGER_PATTERN = Pattern.compile("-?\\d+");

    private Map<String, Room> rooms = new HashMap<>();
    private Map<String, Integer> idToRoomNumber = new HashMap<>();
    private ArrayList<Room> map = new ArrayList<>();
    private Map<String, int[]> roomCoordinates = new HashMap<>();
    private int minRow = 0;
    private int maxRow = 0;
    private int minCol = 0;
    private int maxCol = 0;

    private Map<String, Monster> monstersById = new HashMap<>();
    private Set<String> solvedPuzzles = new LinkedHashSet<>();

    public void addSolvedPuzzle(String puzzleId) {
        if (puzzleId == null || puzzleId.isBlank())
            return;
        solvedPuzzles.add(puzzleId);
    }

    public boolean isPuzzleSolved(String puzzleId) {
        return puzzleId != null && solvedPuzzles.contains(puzzleId);
    }

    public Set<String> getSolvedPuzzles() {
        return Set.copyOf(solvedPuzzles);
    }

    // Loads rooms from the given filename inside the data/ folder.
    // Returns true on success, false if the file is not found.
    public boolean mapGenerate(String filename) {
        try {
            rooms.clear();
            idToRoomNumber.clear();
            map.clear();
            Table table = Table.read().csv(DATA_DIR + filename);

            Map<String, Integer> idToIndex = new HashMap<>();
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                String roomId = row.getString("RoomID");
                String name = row.getString("Name");
                String description = row.getString("Description");
                Room tempRoom = new Room(roomId, name, description, false);
                map.add(tempRoom);
                rooms.put(roomId, tempRoom);
                idToIndex.put(roomId, i + 1);
                idToRoomNumber.put(roomId, i + 1);
            }

            for (int i = 0; i < table.rowCount(); i++) {
                Room room = map.get(i);
                Row row = table.row(i);
                String north = parseExit(row.getString("North"));
                String south = parseExit(row.getString("South"));
                String east = parseExit(row.getString("East"));
                String west = parseExit(row.getString("West"));

                room.addExit("N", north);
                room.addExit("S", south);
                room.addExit("E", east);
                room.addExit("W", west);

                room.setExitNumber("N", north == null ? null : idToIndex.get(north));
                room.setExitNumber("S", south == null ? null : idToIndex.get(south));
                room.setExitNumber("E", east == null ? null : idToIndex.get(east));
                room.setExitNumber("W", west == null ? null : idToIndex.get(west));
            }

            buildRoomCoordinates();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadMonstersFromTxt(String filename) {
        monstersById.clear();
        int counter = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_DIR + filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",", 13);
                if (parts.length < 13) {
                    continue;
                }
                createAndAttachMonsterFromParts(String.format("M%02d", counter++), parts, true);
            }
            loadMonsterFlavorFromCsv(MONSTER_FLAVOR_FILE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadItemsFromCsv(String itemsFilename, String weaponsFilename,
            String armorFilename, String consumablesFilename) {
        try {
            for (Room room : map) {
                room.getItems().clear();
            }

            Table weaponsTable = Table.read().csv(DATA_DIR + weaponsFilename);
            Table armorTable = Table.read().csv(DATA_DIR + armorFilename);
            Table consumablesTable = Table.read().csv(DATA_DIR + consumablesFilename);
            Table itemsTable = Table.read().csv(DATA_DIR + itemsFilename);

            Map<String, WeaponStats> weaponStatsByItemId = new HashMap<>();
            for (int i = 0; i < weaponsTable.rowCount(); i++) {
                String itemId = getCellString(weaponsTable, "ItemID", i);
                weaponStatsByItemId.put(itemId, new WeaponStats(
                        getCellString(weaponsTable, "weaponType", i),
                        parseIntSafe(getCellString(weaponsTable, "attackBonus", i), 0),
                        parseIntSafe(getCellString(weaponsTable, "missChance", i), 0),
                        getCellString(weaponsTable, "specialEffect", i)));
            }

            Map<String, Integer> armorBonusByItemId = new HashMap<>();
            for (int i = 0; i < armorTable.rowCount(); i++) {
                String itemId = getCellString(armorTable, "ItemID", i);
                int defenseBonus = parseIntSafe(getCellString(armorTable, "defenseBonus", i), 0);
                armorBonusByItemId.put(itemId, defenseBonus);
            }

            Map<String, ConsumableStats> consumableStatsByItemId = new HashMap<>();
            for (int i = 0; i < consumablesTable.rowCount(); i++) {
                String itemId = getCellString(consumablesTable, "ItemID", i);
                consumableStatsByItemId.put(itemId, new ConsumableStats(
                        parseIntSafe(getCellString(consumablesTable, "healAmount", i), 0),
                        parseIntSafe(getCellString(consumablesTable, "tempDefBonus", i), 0),
                        parseIntSafe(getCellString(consumablesTable, "tempDefTurns", i), 0),
                        parseIntSafe(getCellString(consumablesTable, "hpPenalty", i), 0)));
            }

            for (int i = 0; i < itemsTable.rowCount(); i++) {
                String itemId = getCellString(itemsTable, "ItemID", i);
                String itemName = getCellString(itemsTable, "Name", i);
                String itemType = getCellString(itemsTable, "Item Type", i);
                String rarity = getCellString(itemsTable, "Rarity", i);
                String description = getCellString(itemsTable, "Description", i);
                String roomId = getCellString(itemsTable, "RoomID", i);
                String benefits = getCellString(itemsTable, "Benefits", i);
                String weaknesses = getCellString(itemsTable, "Weaknesses", i);

                Item item = createItem(itemId, itemName, itemType, rarity, description,
                        benefits, weaknesses, weaponStatsByItemId, armorBonusByItemId,
                        consumableStatsByItemId);
                Room room = getRoomById(roomId);
                if (item != null && room != null) {
                    // record the base ItemID on the runtime instance for save/load mapping
                    try {
                        item.setOriginalItemId(itemId);
                    } catch (Exception ignored) {
                    }
                    room.addItem(item);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadMonstersFromCsv(String filename) {
        monstersById.clear();
        try {
            Table table = Table.read().csv(DATA_DIR + filename);
            List<String> columnNames = table.columnNames();
            boolean hasMonsterId = columnNames.contains("MonsterID");
            boolean hasAlive = columnNames.contains("isAlive");
            for (int i = 0; i < table.rowCount(); i++) {
                String[] parts = new String[] {
                        getCellString(table, "monsterClass", i),
                        getCellString(table, "name", i),
                        getCellString(table, "level", i),
                        getCellString(table, "maxHp", i),
                        getCellString(table, "speed", i),
                        getCellString(table, "roomId", i),
                        getCellString(table, "resistType", i),
                        getCellString(table, "resistModifier", i),
                        getCellString(table, "weakType", i),
                        getCellString(table, "weakModifier", i),
                        getCellString(table, "description", i),
                        getCellString(table, "abilities", i),
                        getCellString(table, "specialFlags", i)
                };

                String monsterId = hasMonsterId
                        ? getCellString(table, "MonsterID", i)
                        : String.format("M%02d", i + 1);
                boolean alive = !hasAlive || parseBoolean(getCellString(table, "isAlive", i), true);
                createAndAttachMonsterFromParts(monsterId, parts, alive);
            }

            loadMonsterFlavorFromCsv(MONSTER_FLAVOR_FILE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadMonsterFlavorFromCsv(String filename) {
        try {
            Table table = Table.read().csv(DATA_DIR + filename);
            List<String> columnNames = table.columnNames();
            if (!columnNames.contains("MonsterID")) {
                return false;
            }

            boolean hasStart = columnNames.contains("combatStartText");
            boolean hasDeath = columnNames.contains("combatDeathText");

            for (int i = 0; i < table.rowCount(); i++) {
                String monsterId = getCellString(table, "MonsterID", i);
                Monster monster = monstersById.get(monsterId);
                if (monster == null) {
                    continue;
                }

                if (hasStart) {
                    monster.setCombatStartText(getCellString(table, "combatStartText", i));
                }
                if (hasDeath) {
                    monster.setCombatDeathText(getCellString(table, "combatDeathText", i));
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void createAndAttachMonsterFromParts(String id, String[] parts, boolean alive) {
        String monsterClass = parts[0].trim();
        String name = parts[1].trim();
        int level = Integer.parseInt(parts[2].trim());
        int maxHp = Integer.parseInt(parts[3].trim());
        int speed = Integer.parseInt(parts[4].trim());
        String roomId = parts[5].trim();
        String resistType = parts[6].trim();
        double resistModifier = Double.parseDouble(parts[7].trim());
        String weakType = parts[8].trim();
        double weakModifier = Double.parseDouble(parts[9].trim());
        String description = parts[10].trim();
        String abilitiesRaw = parts[11].trim();
        String flagsRaw = parts[12].trim();

        Monster monster = new Monster(id, monsterClass, name, level, maxHp, speed, roomId,
                resistType, resistModifier, weakType, weakModifier, description);
        if (!alive) {
            monster.setAlive(false);
        }

        if (!"NONE".equalsIgnoreCase(abilitiesRaw)) {
            String[] abilityParts = abilitiesRaw.split("\\|");
            for (String rawAbility : abilityParts) {
                String[] a = rawAbility.trim().split(":");
                if (a.length < 5) {
                    continue;
                }
                String abilityName = a[0].trim();
                double damagePercent = Double.parseDouble(a[1].trim());
                double hitChance = Double.parseDouble(a[2].trim());
                EffectType effectType = EffectType.valueOf(a[3].trim().toUpperCase());
                double effectChance = Double.parseDouble(a[4].trim());
                monster.addAbility(new MonsterAbility(abilityName, damagePercent, hitChance, effectType, effectChance));
            }
        }

        if (!"NONE".equalsIgnoreCase(flagsRaw)) {
            String[] flagParts = flagsRaw.split(";");
            for (String rawFlag : flagParts) {
                String[] kv = rawFlag.trim().split("=");
                if (kv.length != 2) {
                    continue;
                }
                double value;
                if ("true".equalsIgnoreCase(kv[1].trim())) {
                    value = 1.0;
                } else if ("false".equalsIgnoreCase(kv[1].trim())) {
                    value = 0.0;
                } else {
                    value = Double.parseDouble(kv[1].trim());
                }
                monster.setSpecialFlag(kv[0].trim(), value);
            }
        }

        monstersById.put(id, monster);
        Room room = rooms.get(roomId);
        if (room != null) {
            room.setMonster(monster);
        }
    }

    private String parseExit(String targetRoomId) {
        if (targetRoomId == null || targetRoomId.isBlank() || targetRoomId.equalsIgnoreCase("null")) {
            return null; // Return null so the Room knows there is no exit
        }
        return targetRoomId.trim();
    }

    private boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(raw.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw.trim())) {
            return false;
        }
        return fallback;
    }

    private String getCellString(Table table, String columnName, int rowIndex) {
        return table.column(columnName).getString(rowIndex);
    }

    private int parseIntSafe(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private int extractFirstInt(String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        Matcher matcher = FIRST_INTEGER_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private boolean containsIgnoreCase(String text, String token) {
        return text != null && token != null && text.toLowerCase().contains(token.toLowerCase());
    }

    private Item createItem(String itemId, String itemName, String itemType, String rarity,
            String description, String benefits, String weaknesses,
            Map<String, WeaponStats> weaponStatsByItemId,
            Map<String, Integer> armorBonusByItemId,
            Map<String, ConsumableStats> consumableStatsByItemId) {
        String normalizedType = itemType == null ? "" : itemType.toLowerCase();

        if (normalizedType.contains("key")) {
            return new KeyItem(itemName, benefits);
        }

        if (normalizedType.contains("armor")) {
            int defenseBonus = armorBonusByItemId.getOrDefault(itemId, extractFirstInt(benefits, 0));
            return new Armor(itemName, Math.max(0, defenseBonus));
        }

        if (normalizedType.contains("weapon")) {
            WeaponStats stats = weaponStatsByItemId.get(itemId);
            int attack = stats != null ? stats.attackBonus() : extractFirstInt(benefits, 0);
            int missChance = stats != null ? stats.missChance() : extractFirstInt(weaknesses, 0);
            String weaponType = stats != null && stats.weaponType() != null && !stats.weaponType().isBlank()
                    ? stats.weaponType()
                    : (normalizedType.contains("ranged") ? "ranged" : "melee");
            String specialEffect = stats != null ? stats.specialEffect() : "NONE";
            return new Weapon(itemName, Math.max(0, attack), weaponType, Math.max(0, missChance), specialEffect);
        }

        if (normalizedType.contains("consumable")) {
            ConsumableStats stats = consumableStatsByItemId.get(itemId);
            int hpEffect = stats != null ? stats.healAmount()
                    : (containsIgnoreCase(benefits, "HP")
                            ? Math.max(0, extractFirstInt(benefits, 0))
                            : 0);
            int defEffect = stats != null ? stats.tempDefBonus()
                    : (containsIgnoreCase(benefits, "DEF")
                            ? Math.max(0, extractFirstInt(benefits, 0))
                            : 0);
            int defDuration;
            if (stats != null) {
                defDuration = stats.tempDefTurns();
            } else if (defEffect > 0) {
                int parsedDuration = extractFirstInt(benefits, 1);
                defDuration = Math.max(1, parsedDuration);
            } else {
                defDuration = 0;
            }
            int hpPenalty = stats != null ? stats.hpPenalty()
                    : (containsIgnoreCase(weaknesses, "HP")
                            ? Math.max(0, Math.abs(extractFirstInt(weaknesses, 0)))
                            : 0);
            return new Consumable(itemName, hpEffect, defEffect, defDuration, hpPenalty);
        }

        return new Item(itemName, itemType, rarity, description, benefits, weaknesses);
    }

    private record WeaponStats(String weaponType, int attackBonus, int missChance, String specialEffect) {
    }

    private record ConsumableStats(int healAmount, int tempDefBonus, int tempDefTurns, int hpPenalty) {
    }

    public boolean loadPuzzles(String filename) {
        return PuzzleLoader.loadAllPuzzles(DATA_DIR + filename, this);
    }

    public Room getRoomById(String roomId) {
        if (roomId == null) {
            return null;
        }
        Room exact = rooms.get(roomId);
        if (exact != null) {
            return exact;
        }
        return rooms.get(roomId.trim().toUpperCase());
    }

    public int getRoomNumberById(String roomId) {
        if (roomId == null) {
            return 0;
        }
        Integer exact = idToRoomNumber.get(roomId);
        if (exact != null) {
            return exact;
        }
        return idToRoomNumber.getOrDefault(roomId.trim().toUpperCase(), 0);
    }

    public int getTotalRooms() {
        return map.size();
    }

    public List<Room> getAllRooms() {
        return map;
    }

    public Map<String, Room> getRoomGraph() {
        return rooms;
    }

    public List<Monster> getAllMonsters() {
        return new ArrayList<>(monstersById.values());
    }

    public Monster getMonsterById(String monsterId) {
        if (monsterId == null) {
            return null;
        }
        return monstersById.get(monsterId);
    }

    // Centralizes the (roomNumber - 1) index math so it never leaks into
    // Controller.
    public Room getRoomByNumber(int roomNumber) {
        if (roomNumber >= 1 && roomNumber <= map.size()) {
            return map.get(roomNumber - 1);
        }
        return null;
    }

    public int countVisitedRooms() {
        int count = 0;
        for (Room room : map) {
            if (room.isVisited())
                count++;
        }
        return count;
    }

    public boolean allRoomsVisited() {
        for (Room room : map) {
            if (!room.isVisited())
                return false;
        }
        return true;
    }

    private static final int COORD_STEP = 2;
    private static final Map<String, Integer> EDGE_EXTRA_ROOMS = createEdgeExtraRooms();
    private static final int[][] DIRECTION_OFFSETS = {
            { -COORD_STEP, 0 },
            { 0, COORD_STEP },
            { COORD_STEP, 0 },
            { 0, -COORD_STEP }
    };

    private static Map<String, Integer> createEdgeExtraRooms() {
        Map<String, Integer> overrides = new HashMap<>();
        // Stretch this corridor so GH branch rooms don't crowd the church/graveyard
        // side.
        overrides.put(normalizedEdgeKey("RD-01", "GH-01"), 2);
        // Add a little extra spacing along the east road and its connection
        // to the shops so the TH/GS clusters don't overlap.
        overrides.put(normalizedEdgeKey("RD-01", "RD-02"), 2);
        overrides.put(normalizedEdgeKey("RD-02", "GS-01"), 2);
        return overrides;
    }

    private static String normalizedEdgeKey(String a, String b) {
        if (a.compareToIgnoreCase(b) <= 0) {
            return a.toUpperCase() + "|" + b.toUpperCase();
        }
        return b.toUpperCase() + "|" + a.toUpperCase();
    }

    private int edgeStepMultiplier(String fromRoomId, String toRoomId) {
        int extraRooms = EDGE_EXTRA_ROOMS.getOrDefault(normalizedEdgeKey(fromRoomId, toRoomId), 0);
        return Math.max(1, 1 + extraRooms);
    }

    private void setRoomCoordinate(String roomId, int row, int col) {
        if (!rooms.containsKey(roomId)) {
            return;
        }
        roomCoordinates.put(roomId, new int[] { row, col });
    }

    private void applyManualLayoutOverrides() {
        int[] rd01 = roomCoordinates.get("RD-01");
        if (rd01 == null) {
            return;
        }

        // Keep the guardhouse branch compact and directional.
        int gh01Row = rd01[0];
        int gh01Col = rd01[1] - (COORD_STEP * edgeStepMultiplier("RD-01", "GH-01"));
        int gh02Row = gh01Row - COORD_STEP;
        int gh02Col = gh01Col;
        int gh03Row = gh02Row;
        int gh03Col = gh02Col + COORD_STEP;
        int gh04Row = gh02Row;
        int gh04Col = gh02Col - COORD_STEP;

        setRoomCoordinate("GH-01", gh01Row, gh01Col);
        setRoomCoordinate("GH-02", gh02Row, gh02Col);
        setRoomCoordinate("GH-03", gh03Row, gh03Col);
        setRoomCoordinate("GH-04", gh04Row, gh04Col);

        // If one of the GH coordinates collides with an existing room (outside
        // of the GH branch), nudge GH-03 horizontally until it is free. This
        // prevents GH-03 from being omitted from the computed map when
        // coordinates conflict.
        int[] gh03 = roomCoordinates.get("GH-03");
        if (gh03 != null) {
            boolean changed = false;
            while (true) {
                boolean collision = false;
                for (Map.Entry<String, int[]> e : roomCoordinates.entrySet()) {
                    String key = e.getKey();
                    if (key == null)
                        continue;
                    if (key.equalsIgnoreCase("GH-01") || key.equalsIgnoreCase("GH-02")
                            || key.equalsIgnoreCase("GH-03") || key.equalsIgnoreCase("GH-04"))
                        continue; // ignore GH branch itself
                    int[] c = e.getValue();
                    if (c != null && c[0] == gh03[0] && c[1] == gh03[1]) {
                        collision = true;
                        break;
                    }
                }
                if (!collision)
                    break;
                // nudge GH-03 further to the right to avoid overlap
                gh03[1] = gh03[1] + COORD_STEP;
                roomCoordinates.put("GH-03", new int[] { gh03[0], gh03[1] });
                changed = true;
            }
            if (changed) {
                updateBounds(gh03);
            }
        }

        recalculateBounds();
    }

    private void recalculateBounds() {
        minRow = Integer.MAX_VALUE;
        maxRow = Integer.MIN_VALUE;
        minCol = Integer.MAX_VALUE;
        maxCol = Integer.MIN_VALUE;
        for (int[] coords : roomCoordinates.values()) {
            if (coords == null || coords.length < 2) {
                continue;
            }
            minRow = Math.min(minRow, coords[0]);
            maxRow = Math.max(maxRow, coords[0]);
            minCol = Math.min(minCol, coords[1]);
            maxCol = Math.max(maxCol, coords[1]);
        }
        if (roomCoordinates.isEmpty()) {
            minRow = maxRow = minCol = maxCol = 0;
        }
    }

    private void buildRoomCoordinates() {
        roomCoordinates.clear();
        if (map.isEmpty()) {
            return;
        }

        String startId = map.get(0).getRoomId();
        roomCoordinates.put(startId, new int[] { 0, 0 });
        minRow = maxRow = 0;
        minCol = maxCol = 0;

        Deque<String> queue = new ArrayDeque<>();
        queue.add(startId);

        while (!queue.isEmpty()) {
            String roomId = queue.removeFirst();
            int[] coords = roomCoordinates.get(roomId);
            Room room = rooms.get(roomId);
            if (room == null || coords == null) {
                continue;
            }

            String[] directions = { "N", "E", "S", "W" }; // Matches the order of DIRECTION_OFFSETS

            for (int d = 0; d < directions.length; d++) {
                // 1. Get the target Room ID using the String direction
                String destinationId = room.getExit(directions[d]);

                // 2. If it's null, there is no exit. Skip.
                if (destinationId == null) {
                    continue;
                }

                // 3. Fetch the actual Room object from your HashMap
                Room neighbor = rooms.get(destinationId);
                if (neighbor == null) {
                    continue;
                }

                String neighborId = neighbor.getRoomId();
                if (roomCoordinates.containsKey(neighborId)) {
                    continue;
                }

                // (Keep your existing coordinate math here)
                int multiplier = edgeStepMultiplier(roomId, neighborId);
                int deltaRow = DIRECTION_OFFSETS[d][0] * multiplier;
                int deltaCol = DIRECTION_OFFSETS[d][1] * multiplier;
                int[] desiredCoords = new int[] { coords[0] + deltaRow, coords[1] + deltaCol };
                if (isCoordinateTaken(desiredCoords[0], desiredCoords[1])) {
                    desiredCoords = findNearestFreeAlongDirection(coords[0], coords[1], deltaRow, deltaCol);
                }

                roomCoordinates.put(neighborId, desiredCoords);
                updateBounds(desiredCoords);
                queue.add(neighborId);
            }
        }

        applyManualLayoutOverrides();
    }

    private boolean isCoordinateTaken(int row, int col) {
        for (int[] coords : roomCoordinates.values()) {
            if (coords[0] == row && coords[1] == col) {
                return true;
            }
        }
        return false;
    }

    private int[] findNearestFreeAlongDirection(int baseRow, int baseCol, int deltaRow, int deltaCol) {
        int scale = 1;
        while (scale < 20) {
            int row = baseRow + (deltaRow * scale);
            int col = baseCol + (deltaCol * scale);
            if (!isCoordinateTaken(row, col)) {
                return new int[] { row, col };
            }
            scale++;
        }
        return new int[] { baseRow + deltaRow, baseCol + deltaCol };
    }

    private void updateBounds(int[] coords) {
        minRow = Math.min(minRow, coords[0]);
        maxRow = Math.max(maxRow, coords[0]);
        minCol = Math.min(minCol, coords[1]);
        maxCol = Math.max(maxCol, coords[1]);
    }

    public int[] getRoomCoordinates(String roomId) {
        return roomCoordinates.get(roomId);
    }

    public int getRoomNumberAt(int row, int col) {
        for (Map.Entry<String, int[]> entry : roomCoordinates.entrySet()) {
            int[] coords = entry.getValue();
            if (coords[0] == row && coords[1] == col) {
                return getRoomNumberById(entry.getKey());
            }
        }
        return 0;
    }

    public int getMapMinRow() {
        return minRow;
    }

    public int getMapMaxRow() {
        return maxRow;
    }

    public int getMapMinCol() {
        return minCol;
    }

    public int getMapMaxCol() {
        return maxCol;
    }

    public int getMapRows() {
        return maxRow - minRow + 1;
    }

    public int getMapCols() {
        return maxCol - minCol + 1;
    }
}
