package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Game;
import model.Item;
import model.Monster;
import model.Player;
import model.Room;
import model.Weapon;
import view.GameView;

/**
 * Runs repeated combat simulations to evaluate Poltergeist barricade behavior.
 */
public class BarricadeMonteCarloTester {
    private static final int RUNS = 100;
    private static final int MAX_COMBATS_PER_RUN = 12;
    private static final int MAX_TURNS_PER_COMBAT = 80;

    private static final class RunResult {
        int barricadeEvents;
        int combats;
        boolean monsterKilled;
        boolean playerDied;
        boolean monsterRelocatedAfterBarricade = true;
        boolean hpResetAfterBarricade = true;
        boolean singleReferenceInvariant = true;
        boolean hadException;
        String exceptionMessage;
        String equippedWeaponName;
        int equippedWeaponDamage;
        int playerTurnsToKill = -1;
    }

    private static final class CaptureView extends GameView {
        private int barricadeEvents;

        @Override
        public void displayMessage(String message, MessageType type) {
            if (message == null) {
                return;
            }
            String normalized = message.toLowerCase();
            if (normalized.contains("flee") && normalized.contains("barricade")) {
                barricadeEvents++;
            }
        }

        @Override
        public void showCombatStart(String monsterName, int level) {
        }

        @Override
        public void updateMonsterStats(String name, int currentHp, int maxHp) {
        }

        @Override
        public void updatePlayerStats(int currentHp, int maxHp, int atk, int def) {
        }

        @Override
        public void setCombatActionsEnabled(boolean enabled) {
        }

        int getBarricadeEvents() {
            return barricadeEvents;
        }
    }

    public static void main(String[] args) {
        List<RunResult> results = new ArrayList<>();

        for (int i = 0; i < RUNS; i++) {
            results.add(runSingleSimulation());
        }

        int runsWithBarricade = 0;
        int runsWithMultipleBarricades = 0;
        int totalBarricades = 0;
        int monsterKills = 0;
        int playerDeaths = 0;
        int relocationFailures = 0;
        int hpResetFailures = 0;
        int invariantFailures = 0;
        int exceptions = 0;
        List<Integer> turnsToKill = new ArrayList<>();

        for (RunResult result : results) {
            totalBarricades += result.barricadeEvents;
            if (result.barricadeEvents > 0) {
                runsWithBarricade++;
            }
            if (result.barricadeEvents > 1) {
                runsWithMultipleBarricades++;
            }
            if (result.monsterKilled) {
                monsterKills++;
            }
            if (result.playerDied) {
                playerDeaths++;
            }
            if (!result.monsterRelocatedAfterBarricade) {
                relocationFailures++;
            }
            if (!result.hpResetAfterBarricade) {
                hpResetFailures++;
            }
            if (!result.singleReferenceInvariant) {
                invariantFailures++;
            }
            if (result.hadException) {
                exceptions++;
            }
            if (result.playerTurnsToKill >= 0) {
                turnsToKill.add(result.playerTurnsToKill);
            }
        }

        System.out.println("=== Barricade Monte Carlo Report ===");
        System.out.println("Runs: " + RUNS);
        System.out.println("Runs with >=1 barricade activation: " + runsWithBarricade + " / " + RUNS);
        System.out.println("Runs with multiple barricades: " + runsWithMultipleBarricades + " / " + RUNS);
        System.out.println("Total barricade activations: " + totalBarricades);
        System.out.println(
                "Average barricade activations per run: " + String.format("%.2f", totalBarricades / (double) RUNS));
        System.out.println("Monster killed before player death: " + monsterKills + " / " + RUNS);
        System.out.println("Player deaths: " + playerDeaths + " / " + RUNS);
        System.out.println("Relocation invariant failures after barricade: " + relocationFailures);
        System.out.println("HP reset-to-full failures after barricade: " + hpResetFailures);
        System.out.println("Single-room-reference invariant failures: " + invariantFailures);
        System.out.println("Exceptions: " + exceptions);
        if (!turnsToKill.isEmpty()) {
            Collections.sort(turnsToKill);
            double averageTurns = turnsToKill.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            int minTurns = turnsToKill.get(0);
            int maxTurns = turnsToKill.get(turnsToKill.size() - 1);
            int medianTurns = turnsToKill.get(turnsToKill.size() / 2);
            System.out.println("Turns to kill (nearby weapon equipped): avg=" + String.format("%.2f", averageTurns)
                    + ", median=" + medianTurns + ", min=" + minTurns + ", max=" + maxTurns);
        }

        if (exceptions > 0) {
            System.out.println("--- Exception Samples ---");
            int printed = 0;
            for (RunResult result : results) {
                if (!result.hadException) {
                    continue;
                }
                System.out.println(result.exceptionMessage == null ? "(no message)" : result.exceptionMessage);
                printed++;
                if (printed >= 5) {
                    break;
                }
            }
        }
    }

    private static RunResult runSingleSimulation() {
        RunResult result = new RunResult();
        try {
            Game game = new Game();
            if (!game.mapGenerate("rooms.csv")) {
                throw new IllegalStateException("Failed to load rooms.csv");
            }
            game.loadPuzzles("puzzles.csv");
            game.loadMonstersFromCsv("monsters.csv");
            game.loadItemsFromCsv("items.csv", "weapons.csv", "armor.csv", "consumables.csv");

            Player player = Player.loadFromCsv("data/player_data.csv", game);
            if (player == null) {
                throw new IllegalStateException("Failed to load player_data.csv");
            }

            Monster target = findFirstPoltergeist(game);
            if (target == null) {
                throw new IllegalStateException("No Poltergeist found in loaded data");
            }

            Room monsterRoom = findMonsterRoom(game, target);
            if (monsterRoom == null) {
                throw new IllegalStateException("Poltergeist room not found");
            }

            Weapon nearby = findBestNearbyWeapon(game, monsterRoom);
            if (nearby != null) {
                player.addToInventory(nearby);
                player.equipWeapon(nearby);
                player.getInventory().remove(nearby);
                result.equippedWeaponName = nearby.getName();
                result.equippedWeaponDamage = nearby.getDamage();
            } else {
                result.equippedWeaponName = "(none)";
                result.equippedWeaponDamage = 0;
            }

            CaptureView view = new CaptureView();
            CombatSystem combatSystem = new CombatSystem(game, view);

            int combats = 0;
            int playerTurns = 0;
            while (combats < MAX_COMBATS_PER_RUN && target.isAlive() && player.isAlive()) {
                Room currentMonsterRoom = findMonsterRoom(game, target);
                if (currentMonsterRoom == null) {
                    result.singleReferenceInvariant = false;
                    break;
                }

                player.setLocation(game.getRoomNumberById(currentMonsterRoom.getRoomId()));
                player.setCurrentRoom(currentMonsterRoom);

                int barricadesBeforeCombat = view.getBarricadeEvents();
                combatSystem.startCombat(player, target, currentMonsterRoom);

                int turnGuard = 0;
                while (combatSystem.isInCombat() && target.isAlive() && player.isAlive()
                        && turnGuard < MAX_TURNS_PER_COMBAT) {
                    combatSystem.executeCombatCycle("attack", player);
                    turnGuard++;
                    playerTurns++;
                }

                combats++;
                int barricadesThisCombat = view.getBarricadeEvents() - barricadesBeforeCombat;
                if (barricadesThisCombat > 0) {
                    Room relocated = findMonsterRoom(game, target);
                    if (relocated == null || relocated == currentMonsterRoom) {
                        result.monsterRelocatedAfterBarricade = false;
                    }
                    if (target.getCurrentHp() != target.getMaxHp()) {
                        result.hpResetAfterBarricade = false;
                    }
                }

                if (countMonsterReferences(game, target) != 1 && target.isAlive()) {
                    result.singleReferenceInvariant = false;
                }

                if (turnGuard >= MAX_TURNS_PER_COMBAT) {
                    break;
                }
            }

            result.combats = combats;
            result.barricadeEvents = view.getBarricadeEvents();
            result.monsterKilled = !target.isAlive();
            result.playerDied = !player.isAlive();
            if (result.monsterKilled) {
                result.playerTurnsToKill = playerTurns;
            }
        } catch (Exception ex) {
            result.hadException = true;
            result.exceptionMessage = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        return result;
    }

    private static Monster findFirstPoltergeist(Game game) {
        for (Monster monster : game.getAllMonsters()) {
            if (monster != null && monster.isType("Poltergeist") && monster.isAlive()) {
                return monster;
            }
        }
        return null;
    }

    private static Room findMonsterRoom(Game game, Monster target) {
        if (target == null) {
            return null;
        }
        for (Room room : game.getAllRooms()) {
            if (room.getMonster() == target) {
                return room;
            }
        }
        return null;
    }

    private static int countMonsterReferences(Game game, Monster target) {
        int count = 0;
        for (Room room : game.getAllRooms()) {
            if (room.getMonster() == target) {
                count++;
            }
        }
        return count;
    }

    private static Weapon findBestNearbyWeapon(Game game, Room origin) {
        Weapon best = findBestWeaponInRoom(origin);
        if (origin == null) {
            return best;
        }

        for (int dir = 0; dir < 4; dir++) {
            int neighborNumber = origin.getExit(dir);
            if (neighborNumber <= 0) {
                continue;
            }
            Room neighbor = game.getRoomByNumber(neighborNumber);
            Weapon candidate = findBestWeaponInRoom(neighbor);
            if (candidate != null && (best == null || candidate.getDamage() > best.getDamage())) {
                best = candidate;
            }
        }

        return best;
    }

    private static Weapon findBestWeaponInRoom(Room room) {
        if (room == null || !room.hasItems()) {
            return null;
        }

        Weapon best = null;
        for (Item item : room.getItems()) {
            if (item instanceof Weapon weapon) {
                if (best == null || weapon.getDamage() > best.getDamage()) {
                    best = weapon;
                }
            }
        }
        return best;
    }
}
