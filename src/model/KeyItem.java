package model;

public class KeyItem extends Item {
    private final String unlockTarget;
    private boolean consumed;

    public KeyItem(String name) {
        this(name, "");
    }

    public KeyItem(String name, String unlockTarget) {
        super(name, "Key Item", "Mythical", "A special item used to unlock something.",
                "Unlocks an area or puzzle", "Usually one-time use");
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

    @Override
    public String getInfo() {
        return super.getInfo() + "\nUnlock Target: " + unlockTarget;
    }

    @Override
    public void use(Player player) {
        if (!consumed) {
            consumed = true;
        }
    }
}