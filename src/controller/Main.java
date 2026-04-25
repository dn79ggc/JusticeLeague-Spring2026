// Main.java
package controller;

/**
 * Entry point for the legacy text mode game.
 * Creates and runs the GameController.
 * 
 * @author Thang Pham
 */
public class Main {
    public static void main(String[] args) {
        GameController controller = new GameController();
        controller.run();
    }
}