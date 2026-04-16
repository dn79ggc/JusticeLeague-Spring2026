package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScramblePuzzle extends Puzzle {
    private final List<String> letters;

    public ScramblePuzzle(String id, String name, String narrative, String solution, int maxAttempts,
            String successMessage, String failureMessage, String hint, List<String> letters) {
        super(id, name, narrative, solution, maxAttempts, successMessage, failureMessage, hint);
        this.letters = new ArrayList<>(letters);
    }

    @Override
    public String start() {
        List<String> shuffled = new ArrayList<>(letters);
        Collections.shuffle(shuffled);
        return "The blocks show the letters: " + String.join("  ", shuffled);
    }

    @Override
    protected ValidationResult validateAnswer(String answer) {
        String cleaned = normalize(answer).replaceAll("\\s+", "");
        String expected = normalize(solution).replaceAll("\\s+", "");
        if (cleaned.isEmpty()) {
            return new ValidationResult(false, false);
        }
        return new ValidationResult(true, cleaned.equals(expected));
    }
}
