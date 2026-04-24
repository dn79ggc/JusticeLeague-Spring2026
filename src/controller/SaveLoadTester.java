package controller;

import model.Game;
import model.Player;
import model.Room;
import model.Item;
import model.Puzzle;
import model.SaveManager;

import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SaveLoadTester {
    public static void main(String[] args) {
        System.out.println("SaveLoadTester starting...");
        Game game = new Game();
        if (!game.mapGenerate("rooms.csv")) {
            System.err.println("Failed to load rooms.csv");
            return;
        }
        game.loadPuzzles("puzzles.csv");
        game.loadMonstersFromCsv("monsters.csv");
        game.loadItemsFromCsv("items.csv", "weapons.csv", "armor.csv", "consumables.csv");

        Player player = Player.loadFromCsv("data/player_data.csv", game);
        System.out.println("Player start: " + player.getName() + " at " + player.getLocation());

        // Move player to a room known to contain an item (CH-03 from data/items.csv)
        Room r = game.getRoomById("CH-03");
        Item pickedItem = null;
        if (r != null && !r.getItems().isEmpty()) {
            player.setLocation(game.getRoomNumberById(r.getRoomId()));
            player.setCurrentRoom(r);
            Item it = r.getItems().get(0);
            System.out.println("Attempting to pick up: " + it.getName() + " from " + r.getRoomId());
            boolean ok = player.pickupItem(it);
            System.out.println("Pickup result: " + ok);
            if (ok)
                pickedItem = it;
        } else {
            System.out.println("No items found in CH-03; scanning for first available item in map...");
            for (Room room : game.getAllRooms()) {
                if (!room.getItems().isEmpty()) {
                    r = room;
                    break;
                }
            }
            if (r != null && !r.getItems().isEmpty()) {
                player.setLocation(game.getRoomNumberById(r.getRoomId()));
                player.setCurrentRoom(r);
                Item it = r.getItems().get(0);
                System.out.println("Picking up " + it.getName() + " from " + r.getRoomId());
                player.pickupItem(it);
                pickedItem = it;
            }
        }

        // If we picked an item, move to a different room and drop it to test saved drop
        // location
        if (pickedItem != null) {
            Room target = game.getRoomById("GH-03");
            if (target != null) {
                player.setLocation(game.getRoomNumberById(target.getRoomId()));
                player.setCurrentRoom(target);
                boolean dropped = player.dropItem(pickedItem);
                System.out.println("Dropped picked item into " + target.getRoomId() + ": " + dropped);
            }
        }

        // Debug: show runtime item origin markers in relevant rooms before saving
        try {
            Room checkGH = game.getRoomById("GH-03");
            if (checkGH != null) {
                System.out.println("Items in GH-03 before save:");
                for (Item it : checkGH.getItems()) {
                    String orig = null;
                    try {
                        orig = it.getOriginalItemId();
                    } catch (Exception ignored) {
                    }
                    System.out.println(" - " + it.getName() + " (orig=" + orig + ")");
                }
            }
            Room checkCH = game.getRoomById("CH-03");
            if (checkCH != null) {
                System.out.println("Items in CH-03 before save:");
                for (Item it : checkCH.getItems()) {
                    String orig = null;
                    try {
                        orig = it.getOriginalItemId();
                    } catch (Exception ignored) {
                    }
                    System.out.println(" - " + it.getName() + " (orig=" + orig + ")");
                }
            }
        } catch (Exception ignored) {
        }

        // Save to slot 1
        // For testing, add a weapon to player's inventory and equip it to ensure equip
        // persistence
        model.Weapon testW = new model.Weapon("Steel Sword", 8);
        player.addToInventory(testW);
        player.equipWeapon(testW);
        player.getInventory().remove(testW);
        System.out.println("Equipped for test: " + testW.getName());

        boolean saved = SaveManager.saveGame(1, player, game);
        System.out.println("Saved to slot1: " + saved);

        // Load back
        Player loaded = SaveManager.loadGame(1, game);
        if (loaded == null) {
            System.err.println("Load failed");
            return;
        }
        System.out.println("Loaded player: " + loaded.getName() + " at " + loaded.getLocation());
        System.out.println("Equipped Weapon after load: "
                + (loaded.getEquippedWeapon() == null ? "(none)" : loaded.getEquippedWeapon().getName()));
        System.out.println("Equipped Armor after load: "
                + (loaded.getEquippedArmor() == null ? "(none)" : loaded.getEquippedArmor().getName()));
        System.out.println("Inventory (after load):");
        for (Item i : loaded.getInventory()) {
            System.out.println(" - " + i.getName());
        }

        // Diagnostic: compare saved item locations vs runtime room contents
        try {
            System.out.println("Saved items (from save.json/items.csv):");
            String itemsPath = SaveManager.SAVES_DIR + "slot1/items.csv";
            for (java.util.Map<String, String> row : SaveManager.readCsvAsStringMaps(itemsPath)) {
                System.out.println(" - " + row.getOrDefault("ItemID", "") + " | " + row.getOrDefault("Name", "")
                        + " | savedRoom=" + row.getOrDefault("RoomID", "") + " pickedUp="
                        + row.getOrDefault("pickedUp", "") + " equipped=" + row.getOrDefault("equipped", ""));
            }
        } catch (Exception e) {
            System.err.println("Diagnostic read failed: " + e.getMessage());
        }

        System.out.println("Runtime room item locations after load:");
        for (Room room : game.getAllRooms()) {
            if (!room.getItems().isEmpty()) {
                System.out.println("Room " + room.getRoomId() + " items:");
                for (Item it : room.getItems()) {
                    String orig = null;
                    try {
                        orig = it.getOriginalItemId();
                    } catch (Exception ignored) {
                    }
                    System.out.println(" - " + it.getName() + " (orig=" + orig + ")");
                }
            }
        }

        System.out.println("Solved puzzles in game after load:");
        StringBuilder diag = new StringBuilder();
        try {
            for (java.util.Map<String, String> row : SaveManager.readCsvAsStringMaps("data/puzzles.csv")) {
                String pid = row.getOrDefault("PuzzleID", "");
                if (pid == null || pid.isBlank())
                    continue;
                System.out.println(" - " + pid + " solved=" + game.isPuzzleSolved(pid));
                diag.append(pid).append(" solved=").append(game.isPuzzleSolved(pid)).append("\n");
            }
        } catch (Exception ignored) {
        }

        // Also write diagnostics to a file so CLI runs capture them
        try {
            Path out = Paths.get(SaveManager.SAVES_DIR + "slot1/diagnostic_after_load.txt");
            if (out.getParent() != null)
                Files.createDirectories(out.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append("Saved items (from save.json/items.csv):\n");
            try {
                String itemsPath = SaveManager.SAVES_DIR + "slot1/items.csv";
                for (Map<String, String> row : SaveManager.readCsvAsStringMaps(itemsPath)) {
                    sb.append(" - ").append(row.getOrDefault("ItemID", "")).append(" | ")
                            .append(row.getOrDefault("Name", "")).append(" | savedRoom=")
                            .append(row.getOrDefault("RoomID", "")).append(" pickedUp=")
                            .append(row.getOrDefault("pickedUp", "")).append(" equipped=")
                            .append(row.getOrDefault("equipped", "")).append("\n");
                }
            } catch (Exception e) {
                sb.append("(failed to read saved items)\n");
            }

            sb.append("\nRuntime room item locations after load:\n");
            for (Room room : game.getAllRooms()) {
                if (!room.getItems().isEmpty()) {
                    sb.append("Room ").append(room.getRoomId()).append(" items:\n");
                    for (Item it : room.getItems()) {
                        String orig = null;
                        try {
                            orig = it.getOriginalItemId();
                        } catch (Exception ignored) {
                        }
                        sb.append(" - ").append(it.getName()).append(" (orig=").append(orig).append(")\n");
                    }
                }
            }

            sb.append("\nRuntime puzzles present in rooms after load:\n");
            for (Room room : game.getAllRooms()) {
                if (room.hasPuzzle()) {
                    Puzzle p = room.getPuzzle();
                    sb.append("Room ").append(room.getRoomId()).append(" -> PuzzleID=")
                            .append(p.getId()).append(" solved=").append(p.isSolved()).append("\n");
                }
            }

            sb.append("\n").append(diag.toString());
            Files.writeString(out, sb.toString());
        } catch (Exception e) {
            System.err.println("Failed to write diagnostic file: " + e.getMessage());
        }

        // Load the same slot again to verify repeated-load behavior
        Player loaded2 = SaveManager.loadGame(1, game);
        System.out.println("Loaded player (2nd load): " + (loaded2 == null ? "(null)" : loaded2.getName()) + " at "
                + (loaded2 == null ? "-" : String.valueOf(loaded2.getLocation())));
        if (loaded2 != null) {
            System.out.println("Equipped Weapon after 2nd load: "
                    + (loaded2.getEquippedWeapon() == null ? "(none)" : loaded2.getEquippedWeapon().getName()));
            System.out.println("Equipped Armor after 2nd load: "
                    + (loaded2.getEquippedArmor() == null ? "(none)" : loaded2.getEquippedArmor().getName()));
        }

        System.out.println("SaveLoadTester finished.");
    }
}
