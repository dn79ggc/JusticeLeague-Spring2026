package model;

import java.io.File;
import java.io.IOException;
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

    // Loads rooms from the given filename inside the data/ folder.
    // Returns true on success, false if the file is not found.
    public boolean mapGenerate(String filename) {
        try {
            Table table = Table.read().csv(DATA_DIR + filename);

            Map<String, Integer> idToIndex = new HashMap<>();
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                String roomId = row.getString("RoomID");
                String name = row.getString("Name");
                String description = row.getString("Description");
                Room tempRoom = new Room(roomId, name, description);
                map.add(tempRoom);
                rooms.put(roomId, tempRoom);
                idToIndex.put(roomId, i + 1);
                idToRoomNumber.put(roomId, i + 1);
            }

            for (int i = 0; i < table.rowCount(); i++) {
                Room room = map.get(i);
                Row row = table.row(i);
                java.util.Set<String> seenDestinations = new java.util.HashSet<>();
                room.addExit("N", parseExit(row.getString("North"), room.getRoomId(), seenDestinations, idToIndex));
                room.addExit("S", parseExit(row.getString("South"), room.getRoomId(), seenDestinations, idToIndex));
                room.addExit("E", parseExit(row.getString("East"), room.getRoomId(), seenDestinations, idToIndex));
                room.addExit("W", parseExit(row.getString("West"), room.getRoomId(), seenDestinations, idToIndex));
            }

            buildRoomCoordinates();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int parseExit(String roomId, String currentRoomId, java.util.Set<String> seenDestinations,
            Map<String, Integer> idToIndex) {
        if (roomId == null || roomId.isBlank() || roomId.equalsIgnoreCase("null")) {
            return 0;
        }
        if (roomId.equals(currentRoomId)) {
            return 0;
        }
        if (seenDestinations.contains(roomId)) {
            return 0;
        }
        seenDestinations.add(roomId);
        return idToIndex.getOrDefault(roomId, 0);
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

            for (int d = 0; d < DIRECTION_OFFSETS.length; d++) {
                int destination = room.getExit(d);
                if (destination <= 0) {
                    continue;
                }
                Room neighbor = getRoomByNumber(destination);
                if (neighbor == null) {
                    continue;
                }
                String neighborId = neighbor.getRoomId();
                if (roomCoordinates.containsKey(neighborId)) {
                    continue;
                }

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
