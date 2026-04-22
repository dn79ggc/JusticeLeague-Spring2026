package controller;

import model.Armor;
import model.Item;
import model.Player;
import model.Weapon;
import view.GameView;

public class InventoryController {

    // Handles only item-related commands
    // Returns true if the command was an item command
    // Returns false if the command was not for inventory/items
    public boolean handleItemCommand(String input, Player player, GameView view) {

        if (input.equalsIgnoreCase("inventory")) {
            view.showInventory(player.showInventory());
            return true;
        }

        if (input.toLowerCase().startsWith("inspect ")) {
            String itemName = input.substring(8).trim();
            Item item = player.getItemByName(itemName);

            if (item == null) {
                view.showMessage("You do not have that item.\n");
            } else {
                view.showInspectResult(player.inspectItem(item));
            }
            return true;
        }

        if (input.toLowerCase().startsWith("drop ")) {
            String itemName = input.substring(5).trim();
            Item item = player.getItemByName(itemName);

            if (item == null) {
                view.showMessage("You do not have that item.\n");
            } else if (player.dropItem(item)) {
                view.showMessage(item.getName() + " dropped.\n");
            } else {
                view.showMessage("You cannot drop that item.\n");
            }
            return true;
        }

        if (input.toLowerCase().startsWith("equip ")) {
            String itemName = input.substring(6).trim();
            Item item = player.getItemByName(itemName);

            if (item == null) {
                view.showMessage("You do not have that item.\n");
                return true;
            }

            if (item instanceof Weapon) {
                player.equipWeapon((Weapon) item);
                view.showEquipSuccess(item.getName());
            } else if (item instanceof Armor) {
                player.equipArmor((Armor) item);
                view.showEquipSuccess(item.getName());
            } else {
                view.showMessage("That item cannot be equipped.\n");
            }

            return true;
        }

        if (input.equalsIgnoreCase("unequip weapon")) {
            if (player.unequipWeapon()) {
                view.showUnequipSuccess("weapon");
            } else {
                view.showMessage("No weapon equipped or inventory full.\n");
            }
            return true;
        }

        if (input.equalsIgnoreCase("unequip armor")) {
            if (player.unequipArmor()) {
                view.showUnequipSuccess("armor");
            } else {
                view.showMessage("No armor equipped or inventory full.\n");
            }
            return true;
        }

        if (input.toLowerCase().startsWith("use ")) {
            String itemName = input.substring(4).trim();
            Item item = player.getItemByName(itemName);

            if (item == null) {
                view.showMessage("You do not have that item.\n");
                return true;
            }

            boolean used = player.useItem(item);

            if (used) {
                view.showMessage(item.getName() + " used.\n");
                view.showPlayerDamage(player.getCurrentHP());
            } else {
                view.showMessage("You cannot use that item right now.\n");
            }

            return true;
        }

        return false;
    }
}