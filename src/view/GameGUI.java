package view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class GameGUI extends Application {

    private enum GameState {
        MAIN_MENU,
        EXPLORATION,
        COMBAT,
        PUZZLE_TEXT,
        PUZZLE_CARD,
        GAME_OVER
    }

    private static final String UI_FONT = "Segoe UI";
    private static final String UI_TEXT_COLOR = "#e5e7eb";
    private static final String UI_SECONDARY_COLOR = "#9ca3af";
    private static final String UI_PANEL_STYLE = "-fx-background-color: #1f2937; -fx-border-color: #374151; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;";

    private VBox saveQuitBox;
    private VBox mainMenuPanel;
    private VBox explorationPanel;
    private VBox combatPanel;
    private VBox puzzleTextPanel;
    private VBox puzzleCardPanel;
    private VBox gameOverPanel;
    private Button btnSaveMenu;
    private Button btnQuitMenu;
    private Button btnNewGame;
    private Button btnLoadGame;
    private Button btnQuitMainMenu;
    private Button btnNorth;
    private Button btnSouth;
    private Button btnEast;
    private Button btnWest;
    private Button btnExploreAction;
    private Button btnInventory;
    private Button btnStatus;
    private Button btnAttack;
    private Button btnDefend;
    private Button btnUseItem;
    private Button btnEquip;
    private Button btnFlee;
    private Button btnInspect;
    private Button btnHint;
    private Button btnExplorePuzzleText;
    private Button btnSubmit;
    private Button btnDraw;
    private Button btnStand;
    private Button btnExplorePuzzleCard;
    private Button btnLoadGameOver;
    private Button btnRestartGameOver;
    private Button btnQuitGameOver;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #111827;");

        root.setLeft(createLeftColumn());
        root.setCenter(createCenterColumn());
        root.setRight(createRightColumn());

        Scene scene = new Scene(root, 1320, 820, Color.web("#111827"));
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F5 && btnSaveMenu != null && isNodeVisible(btnSaveMenu)) {
                btnSaveMenu.fire();
            } else if (e.getCode() == KeyCode.Q) {
                if (btnQuitMainMenu != null && isNodeVisible(btnQuitMainMenu)) {
                    btnQuitMainMenu.fire();
                } else if (btnQuitGameOver != null && isNodeVisible(btnQuitGameOver)) {
                    btnQuitGameOver.fire();
                } else if (btnQuitMenu != null && isNodeVisible(btnQuitMenu)) {
                    btnQuitMenu.fire();
                }
            } else if (e.getCode() == KeyCode.DIGIT1) {
                if (btnNewGame != null && isNodeVisible(btnNewGame)) {
                    btnNewGame.fire();
                } else if (btnExploreAction != null && isNodeVisible(btnExploreAction)) {
                    btnExploreAction.fire();
                } else if (btnAttack != null && isNodeVisible(btnAttack)) {
                    btnAttack.fire();
                } else if (btnHint != null && isNodeVisible(btnHint)) {
                    btnHint.fire();
                } else if (btnDraw != null && isNodeVisible(btnDraw)) {
                    btnDraw.fire();
                } else if (btnLoadGameOver != null && isNodeVisible(btnLoadGameOver)) {
                    btnLoadGameOver.fire();
                }
            } else if (e.getCode() == KeyCode.DIGIT2) {
                if (btnLoadGame != null && isNodeVisible(btnLoadGame)) {
                    btnLoadGame.fire();
                } else if (btnInventory != null && isNodeVisible(btnInventory)) {
                    btnInventory.fire();
                } else if (btnDefend != null && isNodeVisible(btnDefend)) {
                    btnDefend.fire();
                } else if (btnExplorePuzzleText != null && isNodeVisible(btnExplorePuzzleText)) {
                    btnExplorePuzzleText.fire();
                } else if (btnStand != null && isNodeVisible(btnStand)) {
                    btnStand.fire();
                } else if (btnRestartGameOver != null && isNodeVisible(btnRestartGameOver)) {
                    btnRestartGameOver.fire();
                }
            } else if (e.getCode() == KeyCode.DIGIT3) {
                if (btnStatus != null && isNodeVisible(btnStatus)) {
                    btnStatus.fire();
                } else if (btnUseItem != null && isNodeVisible(btnUseItem)) {
                    btnUseItem.fire();
                } else if (btnExplorePuzzleCard != null && isNodeVisible(btnExplorePuzzleCard)) {
                    btnExplorePuzzleCard.fire();
                }
            } else if (e.getCode() == KeyCode.DIGIT4) {
                if (btnEquip != null && isNodeVisible(btnEquip)) {
                    btnEquip.fire();
                }
            } else if (e.getCode() == KeyCode.DIGIT5) {
                if (btnFlee != null && isNodeVisible(btnFlee)) {
                    btnFlee.fire();
                }
            } else if (e.getCode() == KeyCode.W) {
                if (btnNorth != null && isNodeVisible(btnNorth)) {
                    btnNorth.fire();
                }
            } else if (e.getCode() == KeyCode.A) {
                if (btnWest != null && isNodeVisible(btnWest)) {
                    btnWest.fire();
                }
            } else if (e.getCode() == KeyCode.D) {
                if (btnEast != null && isNodeVisible(btnEast)) {
                    btnEast.fire();
                }
            } else if (e.getCode() == KeyCode.S) {
                if (btnSouth != null && isNodeVisible(btnSouth)) {
                    btnSouth.fire();
                }
            } else if (e.getCode() == KeyCode.F) {
                if (btnInspect != null && isNodeVisible(btnInspect)) {
                    btnInspect.fire();
                }
            } else if (e.getCode() == KeyCode.ENTER) {
                if (btnSubmit != null && isNodeVisible(btnSubmit)) {
                    btnSubmit.fire();
                }
            }
        });

        stage.setTitle("Text Explorer UI Template");
        stage.setScene(scene);
        stage.show();

        setGameState(GameState.MAIN_MENU);
    }

    private VBox createLeftColumn() {
        VBox left = new VBox(12);
        left.setPrefWidth(340);
        left.setPadding(new Insets(0, 8, 0, 0));

        VBox playerInfoSection = createSectionBox("Player Information", createPlayerInfoSection());
        VBox roomInfoSection = createSectionBox("Room Information", createRoomInfoSection());
        VBox combatInfoSection = createSectionBox("Combat Information", createCombatInfoSection());
        VBox puzzleInfoSection = createSectionBox("Puzzle Information", createPuzzleInfoSection());

        left.getChildren().addAll(playerInfoSection, roomInfoSection, combatInfoSection, puzzleInfoSection);
        VBox.setVgrow(playerInfoSection, Priority.NEVER);
        VBox.setVgrow(roomInfoSection, Priority.NEVER);
        VBox.setVgrow(combatInfoSection, Priority.NEVER);
        VBox.setVgrow(puzzleInfoSection, Priority.NEVER);

        ScrollPane scrollPane = new ScrollPane(left);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-control-inner-background: transparent;");
        VBox wrapper = new VBox(scrollPane);
        wrapper.setPrefWidth(340);
        return wrapper;
    }

    private VBox createCenterColumn() {
        VBox centerColumn = new VBox(12);
        centerColumn.setPadding(new Insets(0, 12, 0, 12));
        centerColumn.setAlignment(Pos.TOP_CENTER);

        TextArea outputArea = new TextArea();
        outputArea.setText(
                "Welcome to Text Explorer. This output panel will display the current scene, story text, and game results.\n\nUse the buttons below to navigate game state and control the application.");
        outputArea.setWrapText(true);
        outputArea.setEditable(false);
        outputArea.setStyle(
                "-fx-control-inner-background: #1f2937; -fx-text-fill: #e5e7eb; -fx-highlight-fill: #2563eb; -fx-highlight-text-fill: white;");
        outputArea.setPrefHeight(520);

        StackPane statePanels = new StackPane();
        statePanels.setPrefHeight(260);
        statePanels.setMaxWidth(Double.MAX_VALUE);
        statePanels.setStyle(
                "-fx-background-color: #111827; -fx-border-color: #374151; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        mainMenuPanel = createMainMenuPanel();
        explorationPanel = createExplorationPanel();
        combatPanel = createCombatPanel();
        puzzleTextPanel = createPuzzleTextPanel();
        puzzleCardPanel = createPuzzleCardPanel();
        gameOverPanel = createGameOverPanel();

        statePanels.getChildren().addAll(mainMenuPanel, explorationPanel, combatPanel, puzzleTextPanel, puzzleCardPanel,
                gameOverPanel);

        Label title = new Label("Game Output");
        title.setFont(Font.font(UI_FONT, FontWeight.BOLD, 18));
        title.setTextFill(Color.web(UI_TEXT_COLOR));
        title.setAlignment(Pos.CENTER_LEFT);
        title.setPrefWidth(Double.MAX_VALUE);

        centerColumn.getChildren().addAll(title, outputArea, statePanels);
        VBox.setVgrow(outputArea, Priority.ALWAYS);
        return centerColumn;
    }

    private VBox createRightColumn() {
        VBox rightColumn = new VBox(12);
        rightColumn.setPrefWidth(320);

        Label mapTitle = new Label("Map Preview");
        mapTitle.setFont(Font.font(UI_FONT, FontWeight.BOLD, 18));
        mapTitle.setTextFill(Color.web(UI_TEXT_COLOR));

        GridPane mapGrid = new GridPane();
        mapGrid.setHgap(4);
        mapGrid.setVgap(4);
        mapGrid.setPadding(new Insets(6));
        mapGrid.setStyle(UI_PANEL_STYLE);

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                Label cell = new Label((row == 2 && col == 2) ? "You" : "▒");
                cell.setFont(Font.font("Consolas", 14));
                cell.setMinSize(52, 52);
                cell.setAlignment(Pos.CENTER);
                cell.setStyle((row == 2 && col == 2)
                        ? "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-border-color: #4b5563;"
                        : "-fx-background-color: #111827; -fx-text-fill: #9ca3af; -fx-border-color: #374151;");
                mapGrid.add(cell, col, row);
            }
        }

        Label mapHint = new Label("Current location updates automatically as the player moves.");
        mapHint.setTextFill(Color.web("#9ca3af"));
        mapHint.setWrapText(true);

        saveQuitBox = createSaveQuitBox();
        rightColumn.getChildren().addAll(mapTitle, mapGrid, mapHint, saveQuitBox);
        return rightColumn;
    }

    private VBox createPlayerInfoSection() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.setPadding(new Insets(0));

        Label lblPlayerName = createSectionLabel("Playing as: [name]");
        Label lblHP = createSectionLabel("HP:");
        ProgressBar pbPlayerHP = new ProgressBar(0.65);
        pbPlayerHP.setPrefWidth(180);
        Label lblHPValues = createSectionLabel("65 / 100");
        Label lblATK = createSectionLabel("ATK: 15");
        Label lblDEF = createSectionLabel("DEF: 10");
        Label lblBag = createSectionLabel("Bag: 5 / 7");
        Label lblEquippedWeapon = createSectionLabel("Weapon: Crude Dagger (+5 ATK)");
        Label lblEquippedArmor = createSectionLabel("Armor: Rusting Armor (+10 DEF)");
        Label lblStatusHeader = createSectionLabel("Status Effects:");

        ListView<String> lstStatusEffects = new ListView<>();
        lstStatusEffects.getItems().addAll("Poisoned", "Hasted");
        lstStatusEffects.setPrefHeight(112);
        lstStatusEffects.setStyle(
                "-fx-control-inner-background: #111827; -fx-background-color: #111827; -fx-border-color: #374151; -fx-border-width: 1;");
        lstStatusEffects.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setTextFill(Color.web(UI_TEXT_COLOR));
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        Label lblStatusDescription = createSectionLabel("Status Effect Details: None selected.");
        lblStatusDescription.setWrapText(true);
        lblStatusDescription.setFont(Font.font(UI_FONT, 12));
        lblStatusDescription.setTextFill(Color.web("#d1d5db"));

        lstStatusEffects.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                lblStatusDescription.setText("Status Effect Details: None selected.");
            } else {
                lblStatusDescription.setText("Status Effect Details: " + getStatusEffectDescription(newVal));
            }
        });

        grid.add(lblPlayerName, 0, 0, 2, 1);
        grid.add(lblHP, 0, 1);
        grid.add(pbPlayerHP, 1, 1);
        grid.add(lblHPValues, 1, 2);
        grid.add(lblATK, 0, 3);
        grid.add(lblDEF, 1, 3);
        grid.add(lblBag, 0, 4, 2, 1);
        grid.add(lblEquippedWeapon, 0, 5, 2, 1);
        grid.add(lblEquippedArmor, 0, 6, 2, 1);
        grid.add(lblStatusHeader, 0, 7, 2, 1);
        grid.add(lstStatusEffects, 0, 8, 2, 1);
        grid.add(lblStatusDescription, 0, 9, 2, 1);

        VBox content = new VBox(grid);
        content.setPadding(new Insets(0));
        return content;
    }

    private VBox createSectionBox(String titleText, Node content) {
        Label sectionTitle = createSectionLabel(titleText);
        sectionTitle.setFont(Font.font(UI_FONT, FontWeight.BOLD, 14));
        sectionTitle.setTextFill(Color.web(UI_TEXT_COLOR));

        VBox sectionBox = new VBox(10, sectionTitle, content);
        sectionBox.setPadding(new Insets(10));
        sectionBox.setStyle(UI_PANEL_STYLE);
        return sectionBox;
    }

    private VBox createRoomInfoSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        Label lblRoomName = createSectionLabel("Guard Post");
        lblRoomName.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        Label lblRoomID = createSectionLabel("GH-02 · Guardhouse");
        lblRoomID.setTextFill(Color.web("#9ca3af"));
        Label lblExits = createSectionLabel("Exits: N  E  W");
        Label lblDescHeader = createSectionLabel("Description:");
        Label lblRoomDescription = createSectionLabel("A watchful guard post overlooking the eastern road.");
        lblRoomDescription.setWrapText(true);

        box.getChildren().addAll(lblRoomName, lblRoomID, lblExits, new Separator(), lblDescHeader, lblRoomDescription);
        return box;
    }

    private VBox createCombatInfoSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        Label lblEnemyName = createSectionLabel("Banshee  Lv.2");
        Label lblEnemyHP = createSectionLabel("HP:");
        ProgressBar pbEnemyHP = new ProgressBar(0.8);
        pbEnemyHP.setPrefWidth(220);
        Label lblEnemyHPValues = createSectionLabel("40 / 50");
        Label lblWeakness = createSectionLabel("Weak: Ranged weapons");
        Label lblResistance = createSectionLabel("Resists: Melee weapons");
        Label lblMonsterStatus = createSectionLabel("Status: —");

        box.getChildren().addAll(lblEnemyName, lblEnemyHP, pbEnemyHP, lblEnemyHPValues, new Separator(), lblWeakness,
                lblResistance, lblMonsterStatus);
        return box;
    }

    private VBox createPuzzleInfoSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        Label lblPuzzleName = createSectionLabel("Ghost Riddle");
        Label lblAttemptsLabel = createSectionLabel("Attempts:");
        Label lblPuzzleAttempts = createSectionLabel("2");
        lblPuzzleAttempts.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        lblPuzzleAttempts.setTextFill(Color.web("#34d399"));
        Label lblWrongAnswers = createSectionLabel("Wrong so far: —");
        Label lblCardTotal = createSectionLabel("Total: 14");
        lblCardTotal.setVisible(false);

        box.getChildren().addAll(lblPuzzleName, lblAttemptsLabel, lblPuzzleAttempts, lblWrongAnswers, lblCardTotal);
        return box;
    }

    private VBox createSaveQuitBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.setStyle(UI_PANEL_STYLE);

        Label title = createSectionLabel("Main Menu");
        title.setFont(Font.font(UI_FONT, FontWeight.BOLD, 14));

        btnSaveMenu = new Button("Save [F5]");
        btnSaveMenu.setPrefWidth(Double.MAX_VALUE);
        btnQuitMenu = new Button("Quit [Q]");
        btnQuitMenu.setPrefWidth(Double.MAX_VALUE);

        btnSaveMenu.setOnAction(e -> System.out.println("Save action triggered."));
        btnQuitMenu.setOnAction(e -> {
            System.out.println("Quit from quick menu.");
            Platform.exit();
        });

        box.getChildren().addAll(title, btnSaveMenu, btnQuitMenu);
        return box;
    }

    private VBox createMainMenuPanel() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));

        btnNewGame = new Button("New Game [1]");
        btnLoadGame = new Button("Load Game [2]");
        btnQuitMainMenu = new Button("Quit [Q]");

        btnNewGame.setPrefWidth(240);
        btnLoadGame.setPrefWidth(240);
        btnQuitMainMenu.setPrefWidth(240);

        btnNewGame.setOnAction(e -> setGameState(GameState.EXPLORATION));
        btnLoadGame.setOnAction(e -> setGameState(GameState.EXPLORATION));
        btnQuitMainMenu.setOnAction(e -> Platform.exit());

        box.getChildren().addAll(btnNewGame, btnLoadGame, btnQuitMainMenu);
        return box;
    }

    private VBox createExplorationPanel() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));

        GridPane compass = new GridPane();
        compass.setHgap(10);
        compass.setVgap(10);
        compass.setAlignment(Pos.CENTER);

        btnNorth = new Button("North [W]");
        btnWest = new Button("West [A]");
        btnEast = new Button("East [D]");
        btnSouth = new Button("South [S]");
        btnNorth.setPrefWidth(140);
        btnWest.setPrefWidth(140);
        btnEast.setPrefWidth(140);
        btnSouth.setPrefWidth(140);

        compass.add(btnNorth, 1, 0);
        compass.add(btnWest, 0, 1);
        compass.add(btnEast, 2, 1);
        compass.add(btnSouth, 1, 2);
        GridPane.setHalignment(btnNorth, HPos.CENTER);
        GridPane.setHalignment(btnSouth, HPos.CENTER);

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER);
        btnExploreAction = new Button("Explore [1]");
        btnInventory = new Button("Inventory [2]");
        btnStatus = new Button("Status [3]");
        btnExploreAction.setPrefWidth(150);
        btnInventory.setPrefWidth(150);
        btnStatus.setPrefWidth(150);
        btnExploreAction.setOnAction(e -> System.out.println("Explore action triggered."));
        btnInventory.setOnAction(e -> System.out.println("Inventory opened."));
        btnStatus.setOnAction(e -> System.out.println("Status panel opened."));
        actionRow.getChildren().addAll(btnExploreAction, btnInventory, btnStatus);

        box.getChildren().addAll(compass, actionRow);
        return box;
    }

    private VBox createCombatPanel() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));

        btnAttack = new Button("Attack [1]");
        btnAttack.setPrefWidth(260);
        btnAttack.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        btnDefend = new Button("Defend [2]");
        btnUseItem = new Button("Use Item [3]");
        btnEquip = new Button("Equip [4]");
        btnFlee = new Button("Flee [5]");
        btnInspect = new Button("Inspect [F]");
        btnInspect.setPrefWidth(120);

        HBox row1 = new HBox(10, btnAttack);
        HBox row2 = new HBox(10, btnDefend, btnUseItem, btnEquip, btnFlee);
        row2.setAlignment(Pos.CENTER);
        HBox row3 = new HBox(btnInspect);
        row3.setAlignment(Pos.CENTER_RIGHT);

        btnAttack.setOnAction(e -> System.out.println("Attack selected."));
        btnDefend.setOnAction(e -> System.out.println("Defend selected."));
        btnUseItem.setOnAction(e -> System.out.println("Use Item selected."));
        btnEquip.setOnAction(e -> System.out.println("Equip selected."));
        btnFlee.setOnAction(e -> System.out.println("Flee selected."));
        btnInspect.setOnAction(e -> System.out.println("Inspect selected."));

        box.getChildren().addAll(row1, row2, row3);
        return box;
    }

    private VBox createPuzzleTextPanel() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));

        TextField inputField = new TextField();
        inputField.setPromptText("Type your answer here...");
        inputField.setPrefWidth(420);

        btnHint = new Button("Hint [1]");
        btnExplorePuzzleText = new Button("Explore [2]");
        btnSubmit = new Button("Submit [Enter]");
        btnSubmit.setPrefWidth(140);

        HBox row = new HBox(10, btnHint, btnExplorePuzzleText, btnSubmit);
        row.setAlignment(Pos.CENTER);

        btnSubmit.setOnAction(e -> System.out.println("Puzzle answer submitted."));
        btnHint.setOnAction(e -> System.out.println("Puzzle hint requested."));
        btnExplorePuzzleText.setOnAction(e -> System.out.println("Puzzle explore selected."));

        inputField.setOnAction(e -> btnSubmit.fire());

        box.getChildren().addAll(inputField, row);
        return box;
    }

    private VBox createPuzzleCardPanel() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));

        Label lblCardTotal = createSectionLabel("Total: 14");
        lblCardTotal.setFont(Font.font(UI_FONT, FontWeight.BOLD, 18));

        btnDraw = new Button("Draw [1]");
        btnStand = new Button("Stand [2]");
        btnExplorePuzzleCard = new Button("Explore [3]");
        HBox row = new HBox(10, btnDraw, btnStand, btnExplorePuzzleCard);
        row.setAlignment(Pos.CENTER);

        btnDraw.setOnAction(e -> System.out.println("Card draw selected."));
        btnStand.setOnAction(e -> System.out.println("Card stand selected."));
        btnExplorePuzzleCard.setOnAction(e -> System.out.println("Card explore selected."));

        box.getChildren().addAll(lblCardTotal, row);
        return box;
    }

    private VBox createGameOverPanel() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));

        btnLoadGameOver = new Button("Load [1]");
        btnRestartGameOver = new Button("Restart [2]");
        btnQuitGameOver = new Button("Quit [Q]");

        btnLoadGameOver.setPrefWidth(180);
        btnRestartGameOver.setPrefWidth(180);
        btnQuitGameOver.setPrefWidth(180);

        btnLoadGameOver.setOnAction(e -> System.out.println("Load from game over."));
        btnRestartGameOver.setOnAction(e -> setGameState(GameState.MAIN_MENU));
        btnQuitGameOver.setOnAction(e -> Platform.exit());

        box.getChildren().addAll(btnLoadGameOver, btnRestartGameOver, btnQuitGameOver);
        return box;
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web(UI_TEXT_COLOR));
        label.setFont(Font.font(UI_FONT, 13));
        return label;
    }

    private String getStatusEffectDescription(String effect) {
        return switch (effect) {
            case "Poisoned" -> "Deals damage over time and reduces healing effectiveness.";
            case "Hasted" -> "Increases action speed so your turn comes faster.";
            default -> "Displays the effect details when selected.";
        };
    }

    private boolean isNodeVisible(Node node) {
        Node current = node;
        while (current != null) {
            if (!current.isVisible()) {
                return false;
            }
            current = current.getParent();
        }
        return true;
    }

    private void setGameState(GameState state) {
        mainMenuPanel.setVisible(false);
        explorationPanel.setVisible(false);
        combatPanel.setVisible(false);
        puzzleTextPanel.setVisible(false);
        puzzleCardPanel.setVisible(false);
        gameOverPanel.setVisible(false);

        switch (state) {
            case MAIN_MENU -> {
                mainMenuPanel.setVisible(true);
                saveQuitBox.setVisible(false);
            }
            case EXPLORATION -> {
                explorationPanel.setVisible(true);
                saveQuitBox.setVisible(true);
            }
            case COMBAT -> {
                combatPanel.setVisible(true);
                saveQuitBox.setVisible(true);
            }
            case PUZZLE_TEXT -> {
                puzzleTextPanel.setVisible(true);
                saveQuitBox.setVisible(true);
            }
            case PUZZLE_CARD -> {
                puzzleCardPanel.setVisible(true);
                saveQuitBox.setVisible(true);
            }
            case GAME_OVER -> {
                gameOverPanel.setVisible(true);
                saveQuitBox.setVisible(false);
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
