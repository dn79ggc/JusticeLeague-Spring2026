package model;

public class KeyItem extends Item {
    private String unlockTarget;
    private boolean consumed;

    public KeyItem(String name) {
        this(name, "");
    }

    public KeyItem(String name, String unlockTarget) {
        super(name);
        this.unlockTarget = unlockTarget == null ? "" : unlockTarget;
        this.consumed = false;
    }

    public String getUnlockTarget() {
        return unlockTarget;
    }

    public void consume() {
        consumed = true;
    }

    public boolean isConsumed() {
        return consumed;
    }
}
