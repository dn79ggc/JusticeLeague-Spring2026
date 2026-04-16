package model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RPSPuzzle extends Puzzle {
    private static final List<String> VALID_CHOICES = Arrays.asList("ROCK", "PAPER", "SCISSORS");
    private final String ghostHandConfig;
    private String ghostHand;

    public RPSPuzzle(String id, String name, String narrative, String solution, int maxAttempts,
            String successMessage, String failureMessage, String hint, String ghostHandConfig) {
        super(id, name, narrative, solution, maxAttempts, successMessage, failureMessage, hint);
        this.ghostHandConfig = ghostHandConfig != null ? ghostHandConfig.toUpperCase().trim() : "SCISSORS";
    }

    @Override
    public String start() {
        if ("RANDOM".equals(ghostHandConfig)) {
            List<String> choices = VALID_CHOICES;
            ghostHand = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
        } else {
            ghostHand = ghostHandConfig;
        }
        return "The ghost shows: " + ghostHand + ". What do you play? (rock / paper / scissors)";
    }

    @Override
    protected ValidationResult validateAnswer(String answer) {
        String cleaned = normalize(answer);
        if (!VALID_CHOICES.contains(cleaned)) {
            return new ValidationResult(false, false);
        }

        if (cleaned.equals(ghostHand)) {
            return new ValidationResult(true, false);
        }

        return new ValidationResult(true, winsAgainst(cleaned).equals(ghostHand));
    }

    private String winsAgainst(String choice) {
        return switch (choice) {
            case "ROCK" -> "SCISSORS";
            case "PAPER" -> "ROCK";
            case "SCISSORS" -> "PAPER";
            default -> "";
        };
    }
}
