package model;

import java.util.ArrayList;
import java.util.List;

public abstract class Puzzle {
    protected final String id;
    protected final String name;
    protected final String narrative;
    protected final String solution;
    protected final int maxAttempts;
    protected int attemptsLeft;
    protected final String successMessage;
    protected final String failureMessage;
    protected final String hint;
    protected boolean solved;
    protected final List<String> wrongAnswers;

    public enum PuzzleResult {
        CORRECT,
        WRONG_RETRY,
        WRONG_FINAL,
        INVALID_INPUT
    }

    protected static class ValidationResult {
        public final boolean valid;
        public final boolean correct;

        public ValidationResult(boolean valid, boolean correct) {
            this.valid = valid;
            this.correct = correct;
        }
    }

    public Puzzle(String id, String name, String narrative, String solution, int maxAttempts,
            String successMessage, String failureMessage, String hint) {
        this.id = id;
        this.name = name != null ? name : "";
        this.narrative = narrative != null ? narrative : "";
        this.solution = solution != null ? solution.trim() : "";
        this.maxAttempts = Math.max(0, maxAttempts);
        this.attemptsLeft = this.maxAttempts;
        this.successMessage = successMessage != null ? successMessage : "";
        this.failureMessage = failureMessage != null ? failureMessage : "";
        this.hint = hint != null ? hint : "";
        this.solved = false;
        this.wrongAnswers = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getNarrative() {
        return narrative;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public int getAttempts() {
        return attemptsLeft;
    }

    public boolean isSolved() {
        return solved;
    }

    /**
     * Mark this puzzle as solved without going through the normal attempt flow
     * (used by load).
     */
    public void markSolved() {
        this.solved = true;
    }

    public boolean hasHint() {
        return hint != null && !hint.isBlank();
    }

    public String getHint() {
        return hasHint() ? hint : "No hint available";
    }

    public List<String> getWrongAnswers() {
        return new ArrayList<>(wrongAnswers);
    }

    public void resetAttempts() {
        this.attemptsLeft = this.maxAttempts;
        this.wrongAnswers.clear();
    }

    public abstract String start();

    protected abstract ValidationResult validateAnswer(String answer);

    public PuzzleResult checkSolution(String answer) {
        if (answer == null || answer.isBlank()) {
            return PuzzleResult.INVALID_INPUT;
        }

        ValidationResult validation = validateAnswer(answer);
        if (!validation.valid) {
            return PuzzleResult.INVALID_INPUT;
        }

        if (validation.correct) {
            solved = true;
            return PuzzleResult.CORRECT;
        }

        wrongAnswers.add(answer.trim());
        if (attemptsLeft > 1) {
            attemptsLeft--;
            return PuzzleResult.WRONG_RETRY;
        }

        attemptsLeft = Math.max(0, attemptsLeft - 1);

        return PuzzleResult.WRONG_FINAL;
    }

    protected String normalize(String answer) {
        return answer == null ? "" : answer.trim().toUpperCase();
    }
}
