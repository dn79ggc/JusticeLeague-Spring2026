package model;

public class Monster {
    private String name;
    private boolean alive;

    public Monster() {
        this.name = "Monster";
        this.alive = true;
    }

    public Monster(String name) {
        this.name = name;
        this.alive = true;
    }

    public String getName() {
        return name;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
