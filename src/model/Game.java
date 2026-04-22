package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

public class Game {
    // All data files live in the data/ folder at the project root.
    private static final String DATA_DIR = "data/";

    private Map<String, Room> rooms = new HashMap<>();
    private Map<String, Integer> idToRoomNumber = new HashMap<>();
    private ArrayList<Room> map = new ArrayList<>();
    private Map<String, int[]> roomCoordinates = new HashMap<>();
    private int minRow = 0;
    private int maxRow = 0;
    private int minCol = 0;
    private int maxCol = 0;

    private Map<String, Monster> monstersById = new HashMap<>();

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
                java.util.Set<String> seenDestinations = new java.util.HashSet<>();
                room.addExit("N", parseExit(row.getString("North")));
                room.addExit("S", parseExit(row.getString("South")));
                room.addExit("E", parseExit(row.getString("East")));
                room.addExit("W", parseExit(row.getString("West")));
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

    public boolean loadPuzzles(String filename) {
        return PuzzleLoader.loadAllPuzzles(DATA_DIR + filename, this);
    }

    public Room getRoomById(String roomId) {
        return rooms.get(roomId);
    }

    public int getRoomNumberById(String roomId) {
        if (roomId == null) {
            return 0;
        }
        return idToRoomNumber.getOrDefault(roomId, 0);
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

    private static final int[][] DIRECTION_OFFSETS = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

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
            if (room == null) {
                continue;
            }

            String[] directions = {"N", "E", "S", "W"}; // Matches the order of DIRECTION_OFFSETS

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
                int[] desiredCoords = new int[] { coords[0] + DIRECTION_OFFSETS[d][0],
                        coords[1] + DIRECTION_OFFSETS[d][1] };
                if (isCoordinateTaken(desiredCoords[0], desiredCoords[1])) {
                    desiredCoords = findNearestFreeCoordinate(desiredCoords[0], desiredCoords[1]);
                }

                roomCoordinates.put(neighborId, desiredCoords);
                updateBounds(desiredCoords);
                queue.add(neighborId);
            }
        }
    }

    private boolean isCoordinateTaken(int row, int col) {
        for (int[] coords : roomCoordinates.values()) {
            if (coords[0] == row && coords[1] == col) {
                return true;
            }
        }
        return false;
    }

    private int[] findNearestFreeCoordinate(int startRow, int startCol) {
        Deque<int[]> search = new ArrayDeque<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        search.add(new int[] { startRow, startCol });
        visited.add(startRow + "," + startCol);

        while (!search.isEmpty()) {
            int[] current = search.removeFirst();
            if (!isCoordinateTaken(current[0], current[1])) {
                return current;
            }
            for (int[] offset : DIRECTION_OFFSETS) {
                int nextRow = current[0] + offset[0];
                int nextCol = current[1] + offset[1];
                String key = nextRow + "," + nextCol;
                if (visited.add(key)) {
                    search.add(new int[] { nextRow, nextCol });
                }
            }
        }
        return new int[] { startRow, startCol };
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
