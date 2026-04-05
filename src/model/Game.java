package model;

import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Game {
    // All data files live in the data/ folder at the project root.
    // Any future data files (items, monsters, puzzles) should go there too.
    private static final String DATA_DIR = "data/";

    private ArrayList<Room> map = new ArrayList<Room>();

    // Loads rooms from the given filename inside the data/ folder.
    // Returns true on success, false if the file is not found.
    // No printing here — the Controller reads this result and tells the View.
    public boolean mapGenerate(String filename) {
        try {
            File file = new File(DATA_DIR + filename);
            Scanner fileScanner = new Scanner(file);

            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                String[] parts = line.split(",");

                int roomNumber  = Integer.parseInt(parts[0]);
                int north       = Integer.parseInt(parts[1]);
                int east        = Integer.parseInt(parts[2]);
                int south       = Integer.parseInt(parts[3]);
                int west        = Integer.parseInt(parts[4]);
                String description = parts[5];

                Room tempRoom = new Room(roomNumber, north, east, south, west, description);
                map.add(tempRoom);
            }
            fileScanner.close();
            return true;

        } catch (FileNotFoundException e) {
            return false;
        }
    }

    public int getTotalRooms() {
        return map.size();
    }

    // Centralizes the (roomNumber - 1) index math so it never leaks into Controller.
    public Room getRoomByNumber(int roomNumber) {
        if (roomNumber >= 1 && roomNumber <= map.size()) {
            return map.get(roomNumber - 1);
        }
        return null;
    }

    public int countVisitedRooms() {
        int count = 0;
        for (Room room : map) {
            if (room.isVisited()) count++;
        }
        return count;
    }

    public boolean allRoomsVisited() {
        for (Room room : map) {
            if (!room.isVisited()) return false;
        }
        return true;
    }
}
