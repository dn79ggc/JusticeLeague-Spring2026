package model;

import java.util.concurrent.ThreadLocalRandom;

public class PokerPuzzle extends Puzzle {
    private int ghostCard;

    public PokerPuzzle(String id, String name, String narrative, String solution, int maxAttempts,
            String successMessage, String failureMessage, String hint) {
        super(id, name, narrative, solution, maxAttempts, successMessage, failureMessage, hint);
    }

    @Override
    public String start() {
        ghostCard = ThreadLocalRandom.current().nextInt(1, 14);
        return "The ghost draws a card. Its value is " + cardName(ghostCard)
                + ". Type 'draw' to draw your card and compare.";
    }

    @Override
    protected ValidationResult validateAnswer(String answer) {
        String cleaned = normalize(answer);
        if (!"DRAW".equals(cleaned)) {
            return new ValidationResult(false, false);
        }

        int playerCard = ThreadLocalRandom.current().nextInt(1, 14);
        return new ValidationResult(true, playerCard > ghostCard);
    }

    private String cardName(int value) {
        return switch (value) {
            case 1 -> "Ace";
            case 11 -> "Jack";
            case 12 -> "Queen";
            case 13 -> "King";
            default -> String.valueOf(value);
        };
    }
}
