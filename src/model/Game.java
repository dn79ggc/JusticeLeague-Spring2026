package model;

import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Game {
    // All data files live in the data/ folder at the project root.
    // Any future data files (items, monsters, puzzles) should go there too.
    private static final String DATA_DIR = "data/";

    private Map<String, Room> rooms = new HashMap<>();

    private ArrayList<Room> map = new ArrayList<Room>();

    // Loads rooms from the given filename inside the data/ folder.
    // Returns true on success, false if the file is not found.
    // This implementation also converts string room IDs into sequential room
    // numbers
    // so the current controller and player code can operate using numeric room
    // indexes.
    public boolean mapGenerate(String filename) {
        try {
            File file = new File(DATA_DIR + filename);
            Scanner fileScanner = new Scanner(file);

            if (!fileScanner.hasNextLine()) {
                fileScanner.close();
                return false;
            }

            fileScanner.nextLine(); // Skip header

            List<String[]> rows = new ArrayList<>();
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) {
                    continue;
                }
                rows.add(parts);
            }
            fileScanner.close();

            Map<String, Integer> idToIndex = new HashMap<>();
            for (int i = 0; i < rows.size(); i++) {
                String roomId = rows.get(i)[0];
                Room tempRoom = new Room(roomId, rows.get(i)[1], rows.get(i)[2]);
                map.add(tempRoom);
                idToIndex.put(roomId, i + 1);
            }

            for (int i = 0; i < rows.size(); i++) {
                Room room = map.get(i);
                String[] parts = rows.get(i);
                room.addExit("N", parseExit(parts[3], idToIndex));
                room.addExit("S", parseExit(parts[4], idToIndex));
                room.addExit("E", parseExit(parts[5], idToIndex));
                room.addExit("W", parseExit(parts[6], idToIndex));
            }

            return true;

        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private int parseExit(String roomId, Map<String, Integer> idToIndex) {
        if (roomId == null || roomId.isBlank() || roomId.equalsIgnoreCase("null")) {
            return 0;
        }
        return idToIndex.getOrDefault(roomId, 0);
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
}
