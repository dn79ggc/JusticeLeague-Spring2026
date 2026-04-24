package controller;

import model.Game;
import model.Player;
import model.Room;
import model.Item;
import model.SaveManager;

import java.util.List;

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
        if (r != null && !r.getItems().isEmpty()) {
            player.setLocation(game.getRoomNumberById(r.getRoomId()));
            player.setCurrentRoom(r);
            Item it = r.getItems().get(0);
            System.out.println("Attempting to pick up: " + it.getName() + " from " + r.getRoomId());
            boolean ok = player.pickupItem(it);
            System.out.println("Pickup result: " + ok);
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
            }
        }

        // Save to slot 1
        boolean saved = SaveManager.saveGame(1, player, game);
        System.out.println("Saved to slot1: " + saved);

        // Load back
        Player loaded = SaveManager.loadGame(1, game);
        if (loaded == null) {
            System.err.println("Load failed");
            return;
        }
        System.out.println("Loaded player: " + loaded.getName() + " at " + loaded.getLocation());
        System.out.println("Inventory (after load):");
        for (Item i : loaded.getInventory()) {
            System.out.println(" - " + i.getName());
        }

        System.out.println("SaveLoadTester finished.");
    }
}
