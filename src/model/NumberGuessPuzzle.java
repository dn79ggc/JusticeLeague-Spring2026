package model;

/**
 * Number-guessing puzzle variant.
 * Player guesses a random number within a given range.
 * 
 * @author Subhan Choudhry
 */
public class NumberGuessPuzzle extends Puzzle {
    private final int rangeMin;
    private final int rangeMax;
    private int correctNumber;

    public NumberGuessPuzzle(String id, String name, String narrative, String solution, int maxAttempts,
            String successMessage, String failureMessage, String hint,
            int rangeMin, int rangeMax) {
        super(id, name, narrative, solution, maxAttempts, successMessage, failureMessage, hint);
        this.rangeMin = Math.max(1, rangeMin);
        this.rangeMax = Math.max(this.rangeMin, rangeMax);
    }

    @Override
    public String start() {
        correctNumber = resolveCorrectNumber();
        StringBuilder display = new StringBuilder("Numbers written in blood on the wall: ");
        for (int i = rangeMin; i <= rangeMax; i++) {
            if (i > rangeMin) {
                display.append(", ");
            }
            display.append(i);
        }
        display.append(". Pick one.");
        return display.toString();
    }

    @Override
    protected ValidationResult validateAnswer(String answer) {
        String cleaned = answer == null ? "" : answer.trim();
        if (cleaned.isBlank()) {
            return new ValidationResult(false, false);
        }

        int guessed;
        try {
            guessed = Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return new ValidationResult(false, false);
        }

        if (guessed < rangeMin || guessed > rangeMax) {
            return new ValidationResult(false, false);
        }

        return new ValidationResult(true, guessed == correctNumber);
    }

    private int resolveCorrectNumber() {
        String normalized = solution == null ? "" : solution.trim();
        if (!normalized.isBlank()) {
            try {
                int parsed = Integer.parseInt(normalized);
                if (parsed >= rangeMin && parsed <= rangeMax) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Use deterministic fallback when puzzle data intentionally uses non-numeric
                // marker text.
            }
        }
        return rangeMin;
    }
}
