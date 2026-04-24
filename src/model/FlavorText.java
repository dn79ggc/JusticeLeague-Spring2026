package model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

public final class FlavorText {
    private static final String DATA_FILE = "data/flavor_text.csv";
    private static final Map<String, String> TEXT_BY_KEY = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private FlavorText() {
    }

    public static String get(String key, String fallback) {
        ensureLoaded();
        if (key == null || key.isBlank()) {
            return fallback;
        }
        return TEXT_BY_KEY.getOrDefault(key, fallback);
    }

    public static String get(String key, String fallback, Map<String, String> replacements) {
        String text = get(key, fallback);
        if (replacements == null || replacements.isEmpty()) {
            return text;
        }

        String resolved = text;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            resolved = resolved.replace(placeholder, entry.getValue() == null ? "" : entry.getValue());
        }
        return resolved;
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        try {
            Table table = Table.read().csv(DATA_FILE);
            for (int i = 0; i < table.rowCount(); i++) {
                Row row = table.row(i);
                String key = row.getString("Key");
                String text = row.getString("Text");
                if (key != null && !key.isBlank()) {
                    TEXT_BY_KEY.put(key.trim(), text == null ? "" : text.trim());
                }
            }
        } catch (Exception ignored) {
            // Use fallbacks when the file is missing or malformed.
        }

        loaded = true;
    }
}
