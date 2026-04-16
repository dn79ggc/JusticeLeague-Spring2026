package model;

import java.util.concurrent.ThreadLocalRandom;

public class DicePuzzle extends Puzzle {
    private final int sides;
    private final int targetValue;
    private int lastRoll;

    public DicePuzzle(String id, String name, String narrative, String solution, int maxAttempts,
            String successMessage, String failureMessage, String hint, int sides) {
        super(id, name, narrative, solution, maxAttempts, successMessage, failureMessage, hint);
        this.sides = Math.max(1, sides);
        this.targetValue = Math.max(1, Math.min(this.sides, parseTarget(solution)));
    }

    @Override
    public String start() {
        lastRoll = 0;
        return "The imps are rolling dice. Roll to beat them. You need to roll a " + targetValue
                + " (the highest on a " + sides + "-sided die). Type 'roll'.";
    }

    @Override
    protected ValidationResult validateAnswer(String answer) {
        String cleaned = normalize(answer);
        if (!"ROLL".equals(cleaned)) {
            return new ValidationResult(false, false);
        }

        lastRoll = ThreadLocalRandom.current().nextInt(1, sides + 1);
        return new ValidationResult(true, lastRoll == targetValue);
    }

    private int parseTarget(String solution) {
        try {
            return Integer.parseInt(solution.trim());
        } catch (NumberFormatException e) {
            return sides;
        }
    }
}
