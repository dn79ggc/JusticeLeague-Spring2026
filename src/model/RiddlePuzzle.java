package model;

public class RiddlePuzzle extends Puzzle {
    private final String riddleText;

    public RiddlePuzzle(String id, String name, String narrative, String solution, int maxAttempts,
            String successMessage, String failureMessage, String hint, String riddleText) {
        super(id, name, narrative, solution, maxAttempts, successMessage, failureMessage, hint);
        this.riddleText = riddleText != null ? riddleText : "";
    }

    @Override
    public String start() {
        return riddleText;
    }

    @Override
    protected ValidationResult validateAnswer(String answer) {
        String cleaned = normalize(answer);
        if (cleaned.isBlank()) {
            return new ValidationResult(false, false);
        }
        return new ValidationResult(true, cleaned.equals(normalize(solution)));
    }
}
