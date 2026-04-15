package model;

public class Puzzle {
    private String hint;

    public Puzzle() {
        this.hint = "";
    }

    public Puzzle(String hint) {
        this.hint = hint;
    }

    public boolean hasHint() {
        return hint != null && !hint.isBlank();
    }

    public String getHint() {
        return hasHint() ? hint : "No hint available";
    }
}
