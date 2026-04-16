package model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

public class PuzzleLoader {

    public static record PuzzleDefinition(
            String id,
            String type,
            String roomId,
            String name,
            String narrative,
            String solution,
            int maxAttempts,
            String successMessage,
            String failureMessage,
            String hint,
            String extraData) {
    }

    public static boolean loadAllPuzzles(String filePath, Game game) {
        try {
            Map<String, PuzzleDefinition> definitions = loadPuzzleDefinitions(filePath);
            for (PuzzleDefinition definition : definitions.values()) {
                String type = definition.type().toUpperCase().trim();
                Room room = game.getRoomById(definition.roomId());
                if (room == null) {
                    continue;
                }

                if (type.equals("PICKUP")) {
                    if (!definition.extraData().isBlank()) {
                        room.addItem(new KeyItem(definition.extraData().trim()));
                    }
                    continue;
                }

                Puzzle puzzle = createPuzzle(definition);
                if (puzzle != null) {
                    room.setPuzzle(puzzle);
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static Map<String, PuzzleDefinition> loadPuzzleDefinitions(String filePath) throws IOException {
        Table table = Table.read().csv(filePath);
        Map<String, PuzzleDefinition> definitions = new HashMap<>();

        for (int i = 0; i < table.rowCount(); i++) {
            Row row = table.row(i);
            String id = row.getString("PuzzleID");
            String type = row.getString("PuzzleType");
            String roomId = row.getString("RoomID");
            String name = row.getString("PuzzleName");
            String narrative = row.getString("NarrativeText");
            String solution = row.getString("Solution");
            int maxAttempts = getIntValue(row, "MaxAttempts", 0);
            String successMessage = row.getString("SuccessMessage");
            String failureMessage = row.getString("FailureMessage");
            String hint = row.getString("Hint");
            String extraData = row.getString("ExtraData");

            definitions.put(id, new PuzzleDefinition(id, type, roomId, name, narrative, solution,
                    maxAttempts, successMessage, failureMessage, hint, extraData));
        }

        return definitions;
    }

    public static Puzzle createPuzzle(PuzzleDefinition definition) {
        String type = definition.type().toUpperCase().trim();
        String extraData = definition.extraData() != null ? definition.extraData() : "";

        return switch (type) {
            case "SCRAMBLE" -> new ScramblePuzzle(definition.id(), definition.name(), definition.narrative(),
                    definition.solution(), definition.maxAttempts(), definition.successMessage(),
                    definition.failureMessage(), definition.hint(), splitExtraData(extraData, "|"));
            case "NUMBER_GUESS" -> {
                List<String> parts = splitExtraData(extraData, "|");
                int rangeMin = parts.size() > 0 ? parseInt(parts.get(0), 1) : 1;
                int rangeMax = parts.size() > 1 ? parseInt(parts.get(1), 5) : 5;
                yield new NumberGuessPuzzle(definition.id(), definition.name(), definition.narrative(),
                        definition.solution(), definition.maxAttempts(), definition.successMessage(),
                        definition.failureMessage(), definition.hint(), rangeMin, rangeMax);
            }
            case "RPS" -> new RPSPuzzle(definition.id(), definition.name(), definition.narrative(),
                    definition.solution(), definition.maxAttempts(), definition.successMessage(),
                    definition.failureMessage(), definition.hint(),
                    extraData.trim().isBlank() ? "SCISSORS" : extraData.trim());
            case "CARD_POKER" -> new PokerPuzzle(definition.id(), definition.name(), definition.narrative(),
                    definition.solution(), definition.maxAttempts(), definition.successMessage(),
                    definition.failureMessage(), definition.hint());
            case "CARD_DICE" -> new DicePuzzle(definition.id(), definition.name(), definition.narrative(),
                    definition.solution(), definition.maxAttempts(), definition.successMessage(),
                    definition.failureMessage(), definition.hint(), parseInt(extraData, 6));
            case "RIDDLE" -> new RiddlePuzzle(definition.id(), definition.name(), definition.narrative(),
                    definition.solution(), definition.maxAttempts(), definition.successMessage(),
                    definition.failureMessage(), definition.hint(), String.join("\n", splitExtraData(extraData, "|")));
            case "SELECTION" -> new SelectionPuzzle(definition.id(), definition.name(), definition.narrative(),
                    definition.solution(), definition.maxAttempts(), definition.successMessage(),
                    definition.failureMessage(), definition.hint(), splitExtraData(extraData, "|"));
            default -> null;
        };
    }

    private static List<String> splitExtraData(String extraData, String separator) {
        if (extraData == null || extraData.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(extraData.split(separator, -1)).map(String::trim).toList();
    }

    private static int getIntValue(Row row, String columnName, int fallback) {
        try {
            return row.getInt(columnName);
        } catch (Exception e) {
            try {
                return parseInt(row.getString(columnName), fallback);
            } catch (Exception ignored) {
                return fallback;
            }
        }
    }

    private static int parseInt(String text, int fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
