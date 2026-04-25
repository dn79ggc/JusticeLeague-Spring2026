package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiple-choice selection puzzle variant.
 * Player selects the correct answer from multiple options.
 * 
 * @author Subhan Choudhry
 */
public class SelectionPuzzle extends Puzzle {
    private final List<String> options;

    public SelectionPuzzle(String id, String name, String narrative, String solution, int maxAttempts,
            String successMessage, String failureMessage, String hint, List<String> options) {
        super(id, name, narrative, solution, maxAttempts, successMessage, failureMessage, hint);
        this.options = new ArrayList<>(options);
    }

    @Override
    public String start() {
        StringBuilder builder = new StringBuilder("Choose one:\n");
        for (int i = 0; i < options.size(); i++) {
            builder.append("[").append(i + 1).append("] ").append(options.get(i));
            if (i < options.size() - 1) {
                builder.append("  ");
            }
        }
        return builder.toString();
    }

    @Override
    protected ValidationResult validateAnswer(String answer) {
        String cleaned = normalize(answer);
        if (cleaned.isBlank()) {
            return new ValidationResult(false, false);
        }

        if (cleaned.equals(normalize(solution))) {
            return new ValidationResult(true, true);
        }

        try {
            int index = Integer.parseInt(cleaned) - 1;
            if (index >= 0 && index < options.size()) {
                return new ValidationResult(true, normalize(options.get(index)).equals(normalize(solution)));
            }
            return new ValidationResult(false, false);
        } catch (NumberFormatException e) {
            return new ValidationResult(false, false);
        }
    }
}
