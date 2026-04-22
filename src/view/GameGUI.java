package view;

import controller.CombatSystem;
import javafx.beans.binding.Bindings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Game;
import model.Monster;
import model.Player;
import model.Room;
import model.Puzzle;
import model.PuzzleLoader;
import model.SaveManager;

public class GameGUI extends Application {

    private enum GameState {
        MAIN_MENU,
        EXPLORATION,
        COMBAT,
        PUZZLE_TEXT,
        PUZZLE_CARD,
        GAME_OVER
    }

    private enum PuzzleUIType {
        NONE,
        SCRAMBLE,
        NUMBER_GUESS,
        RPS,
        POKER,
        DICE,
        RIDDLE,
        SELECTION
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
    private VBox centerColumn;
    private Button btnSaveMenu;
    private Button btnQuitMenu;
    private Button btnNewGame;
    private Map<String, PuzzleLoader.PuzzleDefinition> puzzleDefinitions = new HashMap<>();
    private Button btnLoadGame;
    private Button btnQuitMainMenu;
    private Button btnNorth;
    private Button btnSouth;
    private Button btnEast;
    private Button btnWest;
    private Button btnSolvePuzzle;
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

    private Label lblPuzzleNarrative;
    private TextField puzzleInputField;
    private HBox letterBlocksRow;
    private HBox numberGuessRow;
    private HBox rpsButtonRow;
    private HBox selectionButtonsRow;
    private Label puzzleResultLabel;
    private Label diceFaceLabel;
    private Label puzzleCardResultLabel;
    private HBox cardRow;
    private VBox cardGhostPanel;
    private VBox cardPlayerPanel;
    private Puzzle activePuzzle;
    private PuzzleUIType activePuzzleType = PuzzleUIType.NONE;

    private Game game;
    private Player player;
    private int selectedRoomNumber;
    private GridPane mapGrid;
    private Map<String, StackPane> mapCells = new HashMap<>();
    private TextArea outputArea;
    private Label lblPlayerName;
    private ProgressBar pbPlayerHP;
    private Label lblHPValues;
    private Label lblATK;
    private Label lblDEF;
    private Label lblBag;
    private Label lblEquippedWeapon;
    private Label lblEquippedArmor;
    private Label lblRoomName;
    private Label lblRoomID;
    private Label lblExits;
    private Label lblRoomDescription;
    private Label lblPuzzleCardTitle;
    private Label lblPuzzleNameInfo;
    private Label lblPuzzleAttemptsInfo;
    private Label lblWrongAnswersInfo;
    private Label lblCardTotalInfo;
    private Label lblEnemyName;
    private ProgressBar pbEnemyHP;
    private Label lblEnemyHPValues;
    private Label lblWeakness;
    private Label lblResistance;
    private Label lblMonsterStatus;
    private VBox combatInfoSectionBox;
    private VBox puzzleInfoSectionBox;
    private ListView<String> lstStatusEffects;
    private Label lblStatusHeader;
    private Label lblStatusDescription;
    private ListView<String> lstInventory;
    private CombatSystem combatSystem;

    @Override
    public void start(Stage stage) {
        loadPuzzleDefinitions();

        game = new Game();
        if (!game.mapGenerate("rooms.csv")) {
            System.out.println("Failed to load rooms from data/rooms.csv.");
        }
        if (!game.loadPuzzles("puzzles.csv")) {
            System.out.println("Failed to load puzzles from data/puzzles.csv.");
        }
        if (!game.loadMonstersFromCsv("monsters.csv")) {
            System.out.println("Failed to load monsters from data/monsters.csv.");
        }
        player = Player.loadFromCsv("data/player_data.csv", game);
        selectedRoomNumber = player.getLocation();
        Room startingRoom = game.getRoomByNumber(player.getLocation());
        if (startingRoom != null) {
            startingRoom.setVisited();
            player.setCurrentRoom(startingRoom);
        }
        combatSystem = new CombatSystem(game, createCombatViewBridge());

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #111827;");

        VBox leftColumn = createLeftColumn();
        VBox center = createCenterColumn();
        VBox rightColumn = createRightColumn();

        root.setLeft(leftColumn);
        root.setCenter(center);
        root.setRight(rightColumn);

        Scene scene = new Scene(root, 1260, 920, Color.web("#111827"));
        stage.setMinWidth(1080);
        stage.setMinHeight(780);

        leftColumn.prefWidthProperty().bind(scene.widthProperty().multiply(0.30));
        leftColumn.minWidthProperty().bind(scene.widthProperty().multiply(0.24));
        rightColumn.prefWidthProperty().bind(scene.widthProperty().multiply(0.22));
        rightColumn.minWidthProperty().bind(scene.widthProperty().multiply(0.18));
        center.prefWidthProperty().bind(
                scene.widthProperty().subtract(leftColumn.prefWidthProperty()).subtract(rightColumn.prefWidthProperty())
                        .subtract(64));
        center.maxWidthProperty().bind(center.prefWidthProperty());
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
                } else if (btnSolvePuzzle != null && isNodeVisible(btnSolvePuzzle)) {
                    btnSolvePuzzle.fire();
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
        stage.centerOnScreen();
        stage.setAlwaysOnTop(true);
        stage.show();
        Platform.runLater(() -> {
            stage.toFront();
            stage.requestFocus();
            stage.setAlwaysOnTop(false);
        });

        updatePlayerInfo();
        updateRoomInfo();
        updateMapGrid();
        setGameState(GameState.MAIN_MENU);
    }

    private VBox createLeftColumn() {
        VBox left = new VBox(12);
        left.setPrefWidth(340);
        left.setMinWidth(280);
        left.setPadding(new Insets(0, 8, 0, 0));

        VBox playerInfoSection = createSectionBox("Player Information", createPlayerInfoSection());
        VBox roomInfoSection = createSectionBox("Room Information", createRoomInfoSection());
        combatInfoSectionBox = createSectionBox("Combat Information", createCombatInfoSection());
        puzzleInfoSectionBox = createSectionBox("Puzzle Information", createPuzzleInfoSection());

        left.getChildren().addAll(playerInfoSection, roomInfoSection, combatInfoSectionBox, puzzleInfoSectionBox);
        VBox.setVgrow(playerInfoSection, Priority.NEVER);
        VBox.setVgrow(roomInfoSection, Priority.NEVER);
        VBox.setVgrow(combatInfoSectionBox, Priority.NEVER);
        VBox.setVgrow(puzzleInfoSectionBox, Priority.NEVER);

        ScrollPane scrollPane = new ScrollPane(left);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-control-inner-background: transparent;");
        VBox wrapper = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        wrapper.setPrefWidth(340);
        wrapper.setMinWidth(280);
        return wrapper;
    }

    private VBox createCenterColumn() {
        centerColumn = new VBox(12);
        centerColumn.setPadding(new Insets(0, 12, 0, 12));
        centerColumn.setAlignment(Pos.TOP_CENTER);
        centerColumn.setFillWidth(true);
        centerColumn.setPrefWidth(520);
        centerColumn.setMinWidth(360);

        outputArea = new TextArea();
        outputArea.setText(
                "Welcome to Text Explorer. This output panel will display the current scene, story text, and game results.\n\nUse the buttons below to navigate game state and control the application.");
        outputArea.setWrapText(true);
        outputArea.setEditable(false);
        outputArea.setStyle(
                "-fx-control-inner-background: #1f2937; -fx-text-fill: #e5e7eb; -fx-highlight-fill: #2563eb; -fx-highlight-text-fill: white;");
        outputArea.setMinHeight(220);
        outputArea.setMaxWidth(Double.MAX_VALUE);
        outputArea.prefHeightProperty().bind(Bindings.max(220.0, centerColumn.heightProperty().subtract(280.0)));

        StackPane statePanels = new StackPane();
        statePanels.setPrefHeight(210);
        statePanels.setMinHeight(190);
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
        rightColumn.setPrefWidth(280);
        rightColumn.setMinWidth(220);

        Label mapTitle = new Label("Map Preview");
        mapTitle.setFont(Font.font(UI_FONT, FontWeight.BOLD, 18));
        mapTitle.setTextFill(Color.web(UI_TEXT_COLOR));

        mapGrid = new GridPane();
        mapGrid.setHgap(4);
        mapGrid.setVgap(4);
        mapGrid.setPadding(new Insets(6));
        mapGrid.setStyle(UI_PANEL_STYLE);

        buildMapGrid();
        updateMapGrid();

        Label mapHint = new Label(
                "Current location and visited rooms are shown on the map. Click a room to view details.");
        mapHint.setTextFill(Color.web("#9ca3af"));
        mapHint.setWrapText(true);
        mapHint.setMaxWidth(Double.MAX_VALUE);
        mapHint.prefWidthProperty().bind(rightColumn.widthProperty().subtract(8));

        VBox inventorySection = createSectionBox("Inventory", createInventorySection());

        saveQuitBox = createSaveQuitBox();
        rightColumn.getChildren().addAll(mapTitle, mapGrid, mapHint, inventorySection, saveQuitBox);
        return rightColumn;
    }

    private StackPane createMapCellPane(int row, int col) {
        StackPane cell = new StackPane();
        cell.setMinSize(46, 46);
        cell.setPrefSize(46, 46);
        cell.setStyle(
                "-fx-background-color: #0f172a; -fx-border-color: #000000; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        mapCells.put(cellKey(row, col), cell);
        return cell;
    }

    private String cellKey(int row, int col) {
        return row + "," + col;
    }

    private void buildMapGrid() {
        mapCells.clear();
        mapGrid.getChildren().clear();
        if (game == null) {
            return;
        }

        int rows = game.getMapRows();
        int cols = game.getMapCols();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                StackPane cell = createMapCellPane(row, col);
                mapGrid.add(cell, col, row);
            }
        }
    }

    private void updateMapGrid() {
        if (game == null) {
            return;
        }

        int rows = game.getMapRows();
        int cols = game.getMapCols();
        mapGrid.getChildren().clear();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int roomNumber = getRoomNumberAtPosition(row, col);
                StackPane cell = mapCells.get(cellKey(row, col));
                if (cell == null) {
                    cell = createMapCellPane(row, col);
                }
                cell.getChildren().clear();
                mapGrid.add(cell, col, row);
                if (roomNumber == 0) {
                    cell.setStyle(
                            "-fx-background-color: #0f172a; -fx-border-color: #0f172a; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
                    cell.setOnMouseClicked(null);
                    continue;
                }

                Room room = game.getRoomByNumber(roomNumber);
                boolean visited = room != null && room.isVisited();
                boolean isCurrent = player != null && player.getLocation() == roomNumber;
                if (!visited && !isCurrent) {
                    cell.setStyle(
                            "-fx-background-color: #0f172a; -fx-border-color: #0f172a; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
                    cell.setOnMouseClicked(null);
                    continue;
                }

                String backgroundColor = isCurrent ? "#2563eb" : "#111827";
                String textColor = isCurrent ? "white" : "#9ca3af";
                String displayText = isCurrent ? "You" : room.getRoomId();

                String topBorder = borderColor(room, 0);
                String rightBorder = borderColor(room, 1);
                String bottomBorder = borderColor(room, 2);
                String leftBorder = borderColor(room, 3);
                String topWidth = borderWidth(room, 0);
                String rightWidth = borderWidth(room, 1);
                String bottomWidth = borderWidth(room, 2);
                String leftWidth = borderWidth(room, 3);
                cell.setStyle(String.format(
                        "-fx-background-color: %s; -fx-border-color: %s %s %s %s; -fx-border-width: %s %s %s %s; -fx-border-radius: 8; -fx-background-radius: 8;",
                        backgroundColor, topBorder, rightBorder, bottomBorder, leftBorder,
                        topWidth, rightWidth, bottomWidth, leftWidth));

                Label cellLabel = new Label(displayText);
                cellLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
                cellLabel.setTextFill(Color.web(textColor));
                cellLabel.setWrapText(true);
                cell.getChildren().add(cellLabel);

                cell.setOnMouseClicked(e -> {
                    selectedRoomNumber = roomNumber;
                    updateRoomInfo();
                    outputArea.appendText("\nSelected room: " + room.getRoomId() + " — " + room.getName() + "\n");
                });
            }
        }
    }

    /** Returns the CSS border colour for one side of a room cell. */
    private String borderColor(Room room, int dir) {
        int neighborNum = room.getExit(dir);
        if (neighborNum <= 0)
            return "#000000"; // wall
        if (game == null)
            return "transparent";
        Room neighbor = game.getRoomByNumber(neighborNum);
        if (neighbor == null)
            return "transparent";
        // Barricade from this room toward neighbor
        String bt = room.getBarricadedTo();
        if (bt != null && bt.equals(neighbor.getRoomId()))
            return "#8b5cf6";
        // Barricade from neighbor toward this room
        String nbt = neighbor.getBarricadedTo();
        if (nbt != null && nbt.equals(room.getRoomId()))
            return "#8b5cf6";
        return "transparent"; // open passage
    }

    /** Returns the CSS border width for one side of a room cell. */
    private String borderWidth(Room room, int dir) {
        int neighborNum = room.getExit(dir);
        if (neighborNum <= 0)
            return "3"; // wall
        if (game == null)
            return "1";
        Room neighbor = game.getRoomByNumber(neighborNum);
        if (neighbor == null)
            return "1";
        String bt = room.getBarricadedTo();
        if (bt != null && bt.equals(neighbor.getRoomId()))
            return "3";
        String nbt = neighbor.getBarricadedTo();
        if (nbt != null && nbt.equals(room.getRoomId()))
            return "3";
        return "1";
    }

    private int getRoomNumberAtPosition(int row, int col) {
        if (game == null) {
            return 0;
        }
        int actualRow = row + game.getMapMinRow();
        int actualCol = col + game.getMapMinCol();
        return game.getRoomNumberAt(actualRow, actualCol);
    }

    private boolean hasRoomAt(int row, int col) {
        return getRoomNumberAtPosition(row, col) > 0;
    }

    private int[] getRoomCoordinates(int roomNumber) {
        Room room = game.getRoomByNumber(roomNumber);
        if (room == null) {
            return null;
        }
        return game.getRoomCoordinates(room.getRoomId());
    }

    private void resetScrambleLetters() {
        for (javafx.scene.Node node : letterBlocksRow.getChildren()) {
            if (node instanceof Button button) {
                button.setDisable(false);
                button.setOpacity(1.0);
            }
        }
    }

    private VBox createPlayerInfoSection() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(10);
        grid.setPadding(new Insets(0));

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPercentWidth(33);
        col0.setHgrow(Priority.ALWAYS);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(34);
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1, col2);

        lblPlayerName = createSectionLabel("Playing as: [name]");
        Label lblHP = createSectionLabel("HP:");
        pbPlayerHP = new ProgressBar(0);
        pbPlayerHP.setPrefWidth(180);
        lblHPValues = createSectionLabel("0 / 0");
        lblATK = createSectionLabel("ATK: 0");
        lblDEF = createSectionLabel("DEF: 0");
        lblBag = createSectionLabel("Bag: 0 / 7");
        lblEquippedWeapon = createSectionLabel("Weapon: None");
        lblEquippedArmor = createSectionLabel("Armor: None");
        lblStatusHeader = createSectionLabel("Status Effects:");

        lstStatusEffects = new ListView<>();
        lstStatusEffects.setPrefHeight(64);
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

        lblStatusDescription = createSectionLabel("Status Effect Details: None selected.");
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

        grid.add(lblPlayerName, 0, 0, 3, 1);
        grid.add(lblHP, 0, 1);
        grid.add(pbPlayerHP, 1, 1, 2, 1);
        grid.add(lblHPValues, 1, 2, 2, 1);
        grid.add(lblATK, 0, 3);
        grid.add(lblDEF, 1, 3);
        grid.add(lblBag, 2, 3);
        grid.add(lblEquippedWeapon, 0, 4);
        grid.add(lblEquippedArmor, 1, 4, 2, 1);
        grid.add(lblStatusHeader, 0, 5, 3, 1);
        grid.add(lstStatusEffects, 0, 6, 3, 1);
        grid.add(lblStatusDescription, 0, 7, 3, 1);

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

    private VBox createInventorySection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8));
        lstInventory = new ListView<>();
        lstInventory.setPrefHeight(120);
        lstInventory.setStyle(
                "-fx-control-inner-background: #111827; -fx-background-color: #111827; -fx-border-color: #374151; -fx-border-width: 1;");
        box.getChildren().add(lstInventory);
        return box;
    }

    private VBox createRoomInfoSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        lblRoomName = createSectionLabel("Guard Post");
        lblRoomName.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        lblRoomID = createSectionLabel("GH-02 · Guardhouse");
        lblRoomID.setTextFill(Color.web("#9ca3af"));
        lblExits = createSectionLabel("Exits: N  E  W");
        Label lblDescHeader = createSectionLabel("Description:");
        lblRoomDescription = createSectionLabel("A watchful guard post overlooking the eastern road.");
        lblRoomDescription.setWrapText(true);

        box.getChildren().addAll(lblRoomName, lblRoomID, lblExits, new Separator(), lblDescHeader, lblRoomDescription);
        return box;
    }

    private VBox createCombatInfoSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        lblEnemyName = createSectionLabel("No enemy");
        Label lblEnemyHP = createSectionLabel("HP:");
        pbEnemyHP = new ProgressBar(0.0);
        pbEnemyHP.setPrefWidth(220);
        lblEnemyHPValues = createSectionLabel("0 / 0");
        lblWeakness = createSectionLabel("Weak: None");
        lblResistance = createSectionLabel("Resists: None");
        lblMonsterStatus = createSectionLabel("Status: —");

        box.getChildren().addAll(lblEnemyName, lblEnemyHP, pbEnemyHP, lblEnemyHPValues, new Separator(), lblWeakness,
                lblResistance, lblMonsterStatus);
        return box;
    }

    private VBox createPuzzleInfoSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        lblPuzzleNameInfo = createSectionLabel("—");
        lblPuzzleNameInfo.setFont(Font.font(UI_FONT, FontWeight.BOLD, 14));
        Label lblAttemptsLabel = createSectionLabel("Attempts remaining:");
        lblPuzzleAttemptsInfo = createSectionLabel("—");
        lblPuzzleAttemptsInfo.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        lblPuzzleAttemptsInfo.setTextFill(Color.web("#34d399"));
        lblWrongAnswersInfo = createSectionLabel("Wrong so far: —");
        lblWrongAnswersInfo.setWrapText(true);
        lblCardTotalInfo = createSectionLabel("");
        lblCardTotalInfo.setVisible(false);
        lblCardTotalInfo.setManaged(false);

        box.getChildren().addAll(lblPuzzleNameInfo, lblAttemptsLabel, lblPuzzleAttemptsInfo, lblWrongAnswersInfo,
                lblCardTotalInfo);
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

        btnSaveMenu.setOnAction(e -> showSaveDialog());
        btnQuitMenu.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Return to the main menu? Use Quit on the main menu to exit the game.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Exit");
            confirm.setHeaderText("Exit to Main Menu");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    setGameState(GameState.MAIN_MENU);
                }
            });
        });

        box.getChildren().addAll(title, btnSaveMenu, btnQuitMenu);
        return box;
    }

    private VBox createMainMenuPanel() {
        VBox box = new VBox();
        box.setFillWidth(true);
        box.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(14);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(18));
        content.setStyle("-fx-background-color: #111827;");

        btnNewGame = new Button("New Game [1]");
        btnLoadGame = new Button("Load Game [2]");
        btnQuitMainMenu = new Button("Quit [Q]");

        btnNewGame.setPrefWidth(240);
        btnLoadGame.setPrefWidth(240);
        btnQuitMainMenu.setPrefWidth(240);

        btnNewGame.setOnAction(e -> resetGame());
        btnLoadGame.setOnAction(e -> showLoadDialog());
        btnQuitMainMenu.setOnAction(e -> Platform.exit());

        Label lblTestHeader = createSectionLabel("Puzzle Test Launcher");
        lblTestHeader.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        lblTestHeader.setTextFill(Color.web("#fca5a5"));

        Label lblMonsterTestHeader = createSectionLabel("Monster Test Launcher");
        lblMonsterTestHeader.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        lblMonsterTestHeader.setTextFill(Color.web("#93c5fd"));

        GridPane puzzleTestGrid = new GridPane();
        puzzleTestGrid.setHgap(10);
        puzzleTestGrid.setVgap(10);
        puzzleTestGrid.setPadding(new Insets(10, 0, 0, 0));

        Button btnTestP01 = createPuzzleTestButton("P01 Scramble", "P01", GameState.PUZZLE_TEXT, PuzzleUIType.SCRAMBLE);
        Button btnTestP02 = createPuzzleTestButton("P02 Number", "P02", GameState.PUZZLE_TEXT,
                PuzzleUIType.NUMBER_GUESS);
        Button btnTestP03 = createPuzzleTestButton("P03 RPS", "P03", GameState.PUZZLE_TEXT, PuzzleUIType.RPS);
        Button btnTestP05 = createPuzzleTestButton("P05 Poker", "P05", GameState.PUZZLE_CARD, PuzzleUIType.POKER);
        Button btnTestP06 = createPuzzleTestButton("P06 Dice", "P06", GameState.PUZZLE_CARD, PuzzleUIType.DICE);
        Button btnTestP07 = createPuzzleTestButton("P07 Ghost", "P07", GameState.PUZZLE_TEXT, PuzzleUIType.RIDDLE);
        Button btnTestP08 = createPuzzleTestButton("P08 Vampire", "P08", GameState.PUZZLE_TEXT, PuzzleUIType.RIDDLE);
        Button btnTestP09 = createPuzzleTestButton("P09 Shadow", "P09", GameState.PUZZLE_TEXT, PuzzleUIType.RIDDLE);
        Button btnTestP10 = createPuzzleTestButton("P10 Wind", "P10", GameState.PUZZLE_TEXT, PuzzleUIType.RIDDLE);
        Button btnTestP11 = createPuzzleTestButton("P11 Key", "P11", GameState.PUZZLE_CARD, PuzzleUIType.SELECTION);

        puzzleTestGrid.add(btnTestP01, 0, 0);
        puzzleTestGrid.add(btnTestP02, 1, 0);
        puzzleTestGrid.add(btnTestP03, 0, 1);
        puzzleTestGrid.add(btnTestP05, 1, 1);
        puzzleTestGrid.add(btnTestP06, 0, 2);
        puzzleTestGrid.add(btnTestP07, 1, 2);
        puzzleTestGrid.add(btnTestP08, 0, 3);
        puzzleTestGrid.add(btnTestP09, 1, 3);
        puzzleTestGrid.add(btnTestP10, 0, 4);
        puzzleTestGrid.add(btnTestP11, 1, 4);

        GridPane monsterTestGrid = new GridPane();
        monsterTestGrid.setHgap(10);
        monsterTestGrid.setVgap(10);
        monsterTestGrid.setPadding(new Insets(10, 0, 0, 0));

        Button btnTestM01 = createMonsterTestButton("Banshee", "M01");
        Button btnTestM02 = createMonsterTestButton("Small Possessor", "M02");
        Button btnTestM04 = createMonsterTestButton("Medium Possessor", "M04");
        Button btnTestM05 = createMonsterTestButton("Poltergeist", "M05");
        Button btnTestM06 = createMonsterTestButton("Large Possessor", "M06");
        Button btnTestM07 = createMonsterTestButton("Shadow", "M07");
        Button btnTestM08 = createMonsterTestButton("The Freak", "M08");

        monsterTestGrid.add(btnTestM01, 0, 0);
        monsterTestGrid.add(btnTestM02, 1, 0);
        monsterTestGrid.add(btnTestM04, 0, 1);
        monsterTestGrid.add(btnTestM05, 1, 1);
        monsterTestGrid.add(btnTestM06, 0, 2);
        monsterTestGrid.add(btnTestM07, 1, 2);
        monsterTestGrid.add(btnTestM08, 0, 3);

        content.getChildren().addAll(btnNewGame, btnLoadGame, btnQuitMainMenu, new Separator(), lblTestHeader,
                puzzleTestGrid, new Separator(), lblMonsterTestHeader, monsterTestGrid);

        ScrollPane menuScrollPane = new ScrollPane(content);
        menuScrollPane.setFitToWidth(true);
        menuScrollPane.setPannable(true);
        menuScrollPane.setStyle(
                "-fx-background: transparent; -fx-background-color: #111827; -fx-control-inner-background: #111827; -fx-padding: 0;");
        VBox.setVgrow(menuScrollPane, Priority.ALWAYS);

        box.getChildren().add(menuScrollPane);
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

        btnNorth.setOnAction(e -> movePlayer("N"));
        btnSouth.setOnAction(e -> movePlayer("S"));
        btnEast.setOnAction(e -> movePlayer("E"));
        btnWest.setOnAction(e -> movePlayer("W"));

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER);
        btnSolvePuzzle = new Button("Solve Puzzle [1]");
        btnExploreAction = new Button("Explore [2]");
        btnInventory = new Button("Inventory [3]");
        btnStatus = new Button("Status [4]");
        btnSolvePuzzle.setPrefWidth(150);
        btnExploreAction.setPrefWidth(150);
        btnInventory.setPrefWidth(150);
        btnStatus.setPrefWidth(150);
        btnSolvePuzzle.setOnAction(e -> attemptPuzzle());
        btnExploreAction.setOnAction(e -> outputText("The area feels quiet. Try moving or solve a room puzzle."));
        btnInventory.setOnAction(e -> outputText("Inventory is not yet implemented in the preview UI."));
        btnStatus.setOnAction(e -> outputText("Status details are available when effects are active."));
        actionRow.getChildren().addAll(btnSolvePuzzle, btnExploreAction, btnInventory, btnStatus);

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

        btnAttack.setOnAction(e -> runCombatAction("attack"));
        btnDefend.setOnAction(e -> runCombatAction("defend"));
        btnUseItem.setOnAction(e -> runCombatAction("useitem"));
        btnEquip.setOnAction(e -> runCombatAction("equip"));
        btnFlee.setOnAction(e -> runCombatAction("flee"));
        btnInspect.setOnAction(e -> runCombatAction("inspect enemy"));

        box.getChildren().addAll(row1, row2, row3);
        return box;
    }

    private VBox createPuzzleTextPanel() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(18));

        lblPuzzleNarrative = createSectionLabel(
                "Puzzle narrative appears here. Use typed input or click a letter block.");
        lblPuzzleNarrative.setWrapText(true);
        lblPuzzleNarrative.setPrefWidth(520);
        lblPuzzleNarrative.setStyle("-fx-text-fill: #c7d2fe; -fx-font-style: italic;");

        letterBlocksRow = new HBox(8);
        letterBlocksRow.setAlignment(Pos.CENTER);
        letterBlocksRow.setPadding(new Insets(4));
        letterBlocksRow.setVisible(false);
        letterBlocksRow.setManaged(false);
        for (String letter : new String[] { "S", "K", "O", "O", "P", "Y" }) {
            Button letterButton = new Button(letter);
            letterButton.setPrefSize(46, 46);
            letterButton.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
            letterButton.setStyle(
                    "-fx-background-color: #374151; -fx-text-fill: #f8fafc; -fx-border-color: #6d28d9; -fx-border-radius: 8; -fx-background-radius: 8;");
            letterButton.setOnAction(e -> {
                if (!letterButton.isDisabled()) {
                    puzzleInputField.appendText(letterButton.getText());
                    letterButton.setDisable(true);
                    letterButton.setOpacity(0.45);
                }
            });
            letterBlocksRow.getChildren().add(letterButton);
        }

        numberGuessRow = new HBox(10);
        numberGuessRow.setAlignment(Pos.CENTER);
        numberGuessRow.setPadding(new Insets(4));
        numberGuessRow.setVisible(false);
        numberGuessRow.setManaged(false);
        for (int i = 1; i <= 5; i++) {
            Button numberButton = new Button(String.valueOf(i));
            numberButton.setPrefSize(50, 50);
            numberButton.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
            numberButton.setStyle(
                    "-fx-background-color: #7f1d1d; -fx-text-fill: #fee2e2; -fx-border-color: #fca5a5; -fx-border-radius: 8; -fx-background-radius: 8;");
            numberButton.setOnAction(e -> {
                puzzleInputField.setText(numberButton.getText());
                submitPuzzleAnswer(numberButton.getText());
            });
            numberGuessRow.getChildren().add(numberButton);
        }

        rpsButtonRow = new HBox(10);
        rpsButtonRow.setAlignment(Pos.CENTER);
        rpsButtonRow.setPadding(new Insets(4));
        rpsButtonRow.setVisible(false);
        rpsButtonRow.setManaged(false);
        for (String label : new String[] { "🪨 Rock", "📄 Paper", "✂ Scissors" }) {
            Button optionButton = new Button(label);
            optionButton.setPrefWidth(130);
            optionButton.setStyle(
                    "-fx-background-color: #111827; -fx-text-fill: #fef3c7; -fx-border-color: #f59e0b; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");
            optionButton.setOnAction(e -> submitPuzzleAnswer(optionButton.getText().split(" ")[1]));
            rpsButtonRow.getChildren().add(optionButton);
        }

        puzzleInputField = new TextField();
        puzzleInputField.setPromptText("Type or click letters to arrange the answer...");
        puzzleInputField.setPrefWidth(520);
        puzzleInputField.setVisible(false);
        puzzleInputField.setManaged(false);
        puzzleInputField.setStyle(
                "-fx-background-color: #111827; -fx-text-fill: #f8fafc; -fx-border-color: #374151; -fx-border-radius: 8; -fx-background-radius: 8;");

        btnHint = new Button("Hint [1]");
        btnExplorePuzzleText = new Button("Cancel [2]");
        btnSubmit = new Button("Submit [Enter]");
        btnSubmit.setPrefWidth(160);
        btnSubmit.setVisible(false);
        btnSubmit.setManaged(false);
        btnHint.setPrefWidth(120);
        btnExplorePuzzleText.setPrefWidth(120);

        HBox actionRow = new HBox(12, btnHint, btnExplorePuzzleText, btnSubmit);
        actionRow.setAlignment(Pos.CENTER);

        puzzleResultLabel = createSectionLabel("Result will appear here.");
        puzzleResultLabel.setFont(Font.font(UI_FONT, FontWeight.BOLD, 14));
        puzzleResultLabel.setTextFill(Color.web("#fda4af"));
        puzzleResultLabel.setVisible(false);
        puzzleResultLabel.setManaged(false);

        btnSubmit.setOnAction(e -> submitPuzzleAnswer(puzzleInputField.getText()));
        btnHint.setOnAction(e -> {
            if (activePuzzle != null) {
                String hintText = activePuzzle.getHint();
                System.out.println("Hint: " + hintText);
                displayPuzzleResult("Hint: " + hintText);
            } else {
                System.out.println("No active puzzle to hint.");
            }
        });
        btnExplorePuzzleText.setOnAction(e -> {
            System.out.println("Puzzle cancelled.");
            clearActivePuzzle();
        });

        puzzleInputField.setOnAction(e -> btnSubmit.fire());

        box.getChildren().addAll(lblPuzzleNarrative, letterBlocksRow, numberGuessRow, rpsButtonRow, puzzleInputField,
                actionRow, puzzleResultLabel);
        return box;
    }

    private VBox createPuzzleCardPanel() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(18));

        lblPuzzleCardTitle = createSectionLabel("Puzzle");
        lblPuzzleCardTitle.setFont(Font.font(UI_FONT, FontWeight.BOLD, 18));

        cardGhostPanel = createCardPanel("GHOST", "?");
        cardPlayerPanel = createCardPanel("YOU", "?");

        cardRow = new HBox(24, cardGhostPanel, cardPlayerPanel);
        cardRow.setAlignment(Pos.CENTER);

        diceFaceLabel = createSectionLabel("Roll result appears here.");
        diceFaceLabel.setTextFill(Color.web("#fef08a"));
        diceFaceLabel.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));

        selectionButtonsRow = new HBox(12);
        selectionButtonsRow.setAlignment(Pos.CENTER);
        selectionButtonsRow.setPadding(new Insets(6));
        for (String option : new String[] { "IRON", "BRASS", "GOLD" }) {
            Button choiceButton = new Button(option);
            choiceButton.setPrefSize(120, 80);
            choiceButton.setFont(Font.font(UI_FONT, FontWeight.BOLD, 14));
            choiceButton.setStyle(
                    "-fx-background-color: #111827; -fx-text-fill: #fde68a; -fx-border-color: #9d174d; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");
            choiceButton.setOnAction(e -> submitPuzzleAnswer(option));
            selectionButtonsRow.getChildren().add(choiceButton);
        }

        btnDraw = new Button("Draw Card [1]");
        btnStand = new Button("Roll Dice [2]");
        btnExplorePuzzleCard = new Button("Cancel [3]");
        btnDraw.setPrefWidth(130);
        btnStand.setPrefWidth(130);
        btnExplorePuzzleCard.setPrefWidth(130);

        HBox actionRow = new HBox(12, btnDraw, btnStand, btnExplorePuzzleCard);
        actionRow.setAlignment(Pos.CENTER);

        cardRow.setVisible(false);
        cardRow.setManaged(false);
        diceFaceLabel.setVisible(false);
        diceFaceLabel.setManaged(false);
        btnDraw.setVisible(false);
        btnDraw.setManaged(false);
        btnStand.setVisible(false);
        btnStand.setManaged(false);
        selectionButtonsRow.setVisible(false);
        selectionButtonsRow.setManaged(false);

        btnDraw.setOnAction(e -> submitPuzzleAnswer("draw"));
        btnStand.setOnAction(e -> submitPuzzleAnswer("roll"));
        btnExplorePuzzleCard.setOnAction(e -> {
            System.out.println("Puzzle cancelled.");
            clearActivePuzzle();
        });

        puzzleCardResultLabel = createSectionLabel("");
        puzzleCardResultLabel.setFont(Font.font(UI_FONT, FontWeight.BOLD, 14));
        puzzleCardResultLabel.setTextFill(Color.web("#fda4af"));
        puzzleCardResultLabel.setVisible(false);
        puzzleCardResultLabel.setManaged(false);

        box.getChildren().addAll(lblPuzzleCardTitle, cardRow, diceFaceLabel, actionRow, selectionButtonsRow,
                puzzleCardResultLabel);
        return box;
    }

    private VBox createCardPanel(String title, String value) {
        VBox panel = new VBox(6);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(10));
        panel.setStyle(
                "-fx-background-color: #111827; -fx-border-color: #4b5563; -fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16;");

        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        lblTitle.setTextFill(Color.web(UI_TEXT_COLOR));

        Label lblValue = new Label(value);
        lblValue.setFont(Font.font("Consolas", FontWeight.BOLD, 40));
        lblValue.setTextFill(Color.web("#ffffff"));

        panel.getChildren().addAll(lblTitle, lblValue);
        return panel;
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

        btnLoadGameOver.setOnAction(e -> showLoadDialog());
        btnRestartGameOver.setOnAction(e -> setGameState(GameState.MAIN_MENU));
        btnQuitGameOver.setOnAction(e -> Platform.exit());

        box.getChildren().addAll(btnLoadGameOver, btnRestartGameOver, btnQuitGameOver);
        return box;
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web(UI_TEXT_COLOR));
        label.setFont(Font.font(UI_FONT, 12));
        return label;
    }

    private Button createPuzzleTestButton(String label, String puzzleId, GameState state, PuzzleUIType puzzleType) {
        Button button = new Button(label);
        button.setPrefWidth(240);
        button.setOnAction(e -> {
            System.out.println("Test start puzzle " + label);
            PuzzleLoader.PuzzleDefinition definition = puzzleDefinitions.get(puzzleId);
            if (definition == null) {
                System.out.println("Puzzle definition not found: " + puzzleId);
                return;
            }
            activePuzzle = PuzzleLoader.createPuzzle(definition);
            setActivePuzzleType(puzzleType);
            setGameState(state);
            startPuzzleUI(activePuzzle);
        });
        return button;
    }

    private Button createMonsterTestButton(String label, String monsterId) {
        Button button = new Button(label);
        button.setPrefWidth(240);
        button.setOnAction(e -> startMonsterTest(monsterId));
        return button;
    }

    private void startPuzzleUI(Puzzle puzzle) {
        if (puzzle == null) {
            return;
        }

        puzzleResultLabel.setText("");
        puzzleResultLabel.setVisible(false);
        puzzleResultLabel.setManaged(false);
        puzzleCardResultLabel.setText("");
        puzzleCardResultLabel.setVisible(false);
        puzzleCardResultLabel.setManaged(false);

        puzzleInputField.clear();
        resetScrambleLetters();
        if (activePuzzleType == PuzzleUIType.POKER || activePuzzleType == PuzzleUIType.DICE) {
            diceFaceLabel.setText(puzzle.start());
            lblPuzzleNarrative.setText(puzzle.getNarrative());
        } else if (activePuzzleType == PuzzleUIType.SELECTION) {
            lblPuzzleNarrative.setText(puzzle.getNarrative());
        } else {
            lblPuzzleNarrative.setText(puzzle.getNarrative() + "\n\n" + puzzle.start());
        }

        // Update left-panel puzzle info section
        if (lblPuzzleNameInfo != null) {
            lblPuzzleNameInfo.setText(puzzle.getName());
        }
        if (lblPuzzleAttemptsInfo != null) {
            lblPuzzleAttemptsInfo.setText(String.valueOf(puzzle.getAttempts()));
            lblPuzzleAttemptsInfo.setTextFill(Color.web("#34d399"));
        }
        if (lblWrongAnswersInfo != null) {
            lblWrongAnswersInfo.setText("Wrong so far: —");
        }
        boolean isCard = activePuzzleType == PuzzleUIType.POKER || activePuzzleType == PuzzleUIType.DICE
                || activePuzzleType == PuzzleUIType.SELECTION;
        if (lblCardTotalInfo != null) {
            if (isCard) {
                lblCardTotalInfo.setText("Type: " + activePuzzleType.name().charAt(0)
                        + activePuzzleType.name().substring(1).toLowerCase());
                lblCardTotalInfo.setVisible(true);
                lblCardTotalInfo.setManaged(true);
            } else {
                lblCardTotalInfo.setVisible(false);
                lblCardTotalInfo.setManaged(false);
            }
        }

        System.out.println("Puzzle started: " + puzzle.getName());
        if (puzzle.hasHint()) {
            System.out.println("Hint available: type hint or press the hint button.");
        }
    }

    private void updatePlayerInfo() {
        if (player == null) {
            return;
        }
        lblPlayerName.setText("Playing as: " + player.getName());
        pbPlayerHP.setProgress(player.getCurrentHP() / (double) Math.max(1, player.getMaxHP()));
        lblHPValues.setText(player.getCurrentHP() + " / " + player.getMaxHP());
        lblATK.setText("ATK: " + player.getAttackValue());
        lblDEF.setText("DEF: " + player.getDefenseValue());
        lblBag.setText("Bag: " + player.getInventorySlots() + " / 7");
        lblEquippedWeapon.setText(
                "Weapon: " + (player.getEquippedWeapon() != null ? player.getEquippedWeapon().getName() : "None"));
        lblEquippedArmor.setText(
                "Armor: " + (player.getEquippedArmor() != null ? player.getEquippedArmor().getName() : "None"));
        updateStatusEffectsUI();
        updateInventoryPanel();
    }

    private void updateStatusEffectsUI() {
        if (player == null || lstStatusEffects == null || lblStatusHeader == null || lblStatusDescription == null) {
            return;
        }

        lstStatusEffects.getItems().clear();
        for (model.StatusEffect effect : player.getStatusEffects()) {
            lstStatusEffects.getItems().add(effect.getName());
        }

        int count = lstStatusEffects.getItems().size();
        boolean visible = count > 0;
        lblStatusHeader.setVisible(true);
        lblStatusHeader.setManaged(true);
        lstStatusEffects.setVisible(visible);
        lstStatusEffects.setManaged(visible);
        lblStatusDescription.setVisible(visible);
        lblStatusDescription.setManaged(visible);

        if (!visible) {
            lblStatusDescription.setText("Status Effect Details: None active.");
            return;
        }

        double rowHeight = 24.0;
        double height = Math.min(120.0, Math.max(36.0, count * rowHeight + 2.0));
        lstStatusEffects.setPrefHeight(height);
        lblStatusDescription.setText("Status Effect Details: Select an active effect.");
    }

    private void updateInventoryPanel() {
        if (player == null || lstInventory == null) {
            return;
        }
        lstInventory.getItems().setAll(player.showInventory());
    }

    private void updateRoomInfo() {
        Room room = game.getRoomByNumber(selectedRoomNumber);
        if (room == null) {
            return;
        }
        lblRoomName.setText(room.getName());
        lblRoomID.setText(room.getRoomId() + " · " + room.getName());
        lblExits.setText("Exits: " + buildExitString(room));
        lblRoomDescription.setText(room.getRoomDescription());
    }

    private String buildExitString(Room room) {
        StringBuilder exits = new StringBuilder();
        if (room.getExit(0) > 0) {
            exits.append("N ");
        }
        if (room.getExit(1) > 0) {
            exits.append("E ");
        }
        if (room.getExit(2) > 0) {
            exits.append("S ");
        }
        if (room.getExit(3) > 0) {
            exits.append("W ");
        }
        return exits.length() == 0 ? "None" : exits.toString().trim();
    }

    private void outputText(String text) {
        if (outputArea != null) {
            outputArea.appendText("\n" + text);
        }
    }

    private void movePlayer(String direction) {
        if (player == null || game == null) {
            return;
        }
        if (combatSystem != null && combatSystem.isInCombat()) {
            outputText("You cannot move while in combat.");
            return;
        }
        Room currentRoom = game.getRoomByNumber(player.getLocation());
        if (currentRoom == null) {
            outputText("No current room is loaded.");
            return;
        }
        int directionIndex = switch (direction) {
            case "N" -> 0;
            case "E" -> 1;
            case "S" -> 2;
            case "W" -> 3;
            default -> -1;
        };
        if (directionIndex < 0) {
            return;
        }

        int destination = currentRoom.getExit(directionIndex);
        System.out.println("DEBUG movePlayer from " + currentRoom.getRoomId() + " (" + player.getLocation()
                + ") direction " + direction + " -> " + destination);
        if (destination > 0) {
            player.setLocation(destination);
            Room nextRoom = game.getRoomByNumber(destination);
            if (nextRoom != null) {
                player.setCurrentRoom(nextRoom);
                nextRoom.setVisited();
                selectedRoomNumber = destination;
                outputText("Moved to " + nextRoom.getRoomId() + " - " + nextRoom.getName() + ".");
                startCombatIfPresent(nextRoom);
            }
            updatePlayerInfo();
            updateRoomInfo();
            updateMapGrid();
        } else {
            outputText("There is no exit in that direction.");
        }
    }

    private void attemptPuzzle() {
        if (player == null || game == null) {
            return;
        }
        if (combatSystem != null && combatSystem.isInCombat()) {
            outputText("You cannot focus on a puzzle while in combat.");
            return;
        }
        Room currentRoom = game.getRoomByNumber(player.getLocation());
        if (currentRoom == null) {
            outputText("Player is not in a valid room.");
            return;
        }
        if (!currentRoom.hasPuzzle()) {
            outputText("There is no puzzle to solve in this room.");
            return;
        }
        activePuzzle = currentRoom.getPuzzle();
        if (activePuzzle == null) {
            outputText("Room has no active puzzle.");
            return;
        }
        activePuzzleType = determinePuzzleUIType(activePuzzle);
        setActivePuzzleType(activePuzzleType);
        if (activePuzzleType == PuzzleUIType.POKER || activePuzzleType == PuzzleUIType.DICE
                || activePuzzleType == PuzzleUIType.SELECTION) {
            setGameState(GameState.PUZZLE_CARD);
        } else {
            setGameState(GameState.PUZZLE_TEXT);
        }
        startPuzzleUI(activePuzzle);
    }

    private PuzzleUIType determinePuzzleUIType(Puzzle puzzle) {
        String type = puzzle.getClass().getSimpleName().toUpperCase();
        if (type.contains("SCRAMBLE")) {
            return PuzzleUIType.SCRAMBLE;
        }
        if (type.contains("NUMBER")) {
            return PuzzleUIType.NUMBER_GUESS;
        }
        if (type.contains("RPS")) {
            return PuzzleUIType.RPS;
        }
        if (type.contains("POKER")) {
            return PuzzleUIType.POKER;
        }
        if (type.contains("DICE")) {
            return PuzzleUIType.DICE;
        }
        if (type.contains("RIDDLE")) {
            return PuzzleUIType.RIDDLE;
        }
        if (type.contains("SELECTION")) {
            return PuzzleUIType.SELECTION;
        }
        return PuzzleUIType.NONE;
    }

    private void loadPuzzleDefinitions() {
        try {
            puzzleDefinitions = PuzzleLoader.loadPuzzleDefinitions("data/puzzles.csv");
            if (puzzleDefinitions.isEmpty()) {
                System.out.println("No puzzle definitions loaded from data/puzzles.csv.");
            }
        } catch (Exception e) {
            System.out.println("Failed to load puzzle definitions: " + e.getMessage());
            puzzleDefinitions.clear();
        }
    }

    private void submitPuzzleAnswer(String answer) {
        if (activePuzzle == null) {
            System.out.println("No active puzzle to answer.");
            return;
        }

        Puzzle.PuzzleResult result = activePuzzle.checkSolution(answer);
        switch (result) {
            case CORRECT -> {
                String message = activePuzzle.getSuccessMessage();
                System.out.println("Puzzle solved: " + message);
                displayPuzzleResult(message);
                clearActivePuzzle();
            }
            case WRONG_RETRY -> {
                String message = activePuzzle.getFailureMessage() + " Attempts left: " + activePuzzle.getAttempts();
                System.out.println("Puzzle wrong: " + message);
                displayPuzzleResult(message);
                if (lblPuzzleAttemptsInfo != null) {
                    int remaining = activePuzzle.getAttempts();
                    lblPuzzleAttemptsInfo.setText(String.valueOf(remaining));
                    lblPuzzleAttemptsInfo.setTextFill(remaining <= 1
                            ? Color.web("#f87171")
                            : Color.web("#34d399"));
                }
                if (lblWrongAnswersInfo != null) {
                    List<String> wrong = activePuzzle.getWrongAnswers();
                    lblWrongAnswersInfo.setText("Wrong so far: "
                            + (wrong.isEmpty() ? "—" : String.join(", ", wrong)));
                }
            }
            case WRONG_FINAL -> {
                String message = activePuzzle.getFailureMessage() + " The puzzle has failed.";
                System.out.println(message);
                displayPuzzleResult(message);
                clearActivePuzzle();
            }
            case INVALID_INPUT -> {
                String message = "Invalid puzzle input. Try a different action or value.";
                System.out.println(message);
                displayPuzzleResult(message);
            }
        }
    }

    private void displayPuzzleResult(String message) {
        if (activePuzzleType == PuzzleUIType.POKER || activePuzzleType == PuzzleUIType.DICE
                || activePuzzleType == PuzzleUIType.SELECTION) {
            puzzleCardResultLabel.setText(message);
            puzzleCardResultLabel.setVisible(true);
            puzzleCardResultLabel.setManaged(true);
        } else {
            puzzleResultLabel.setText(message);
            puzzleResultLabel.setVisible(true);
            puzzleResultLabel.setManaged(true);
        }
    }

    private void clearActivePuzzle() {
        activePuzzle = null;
        puzzleInputField.clear();
        resetScrambleLetters();
        puzzleResultLabel.setText("");
        puzzleResultLabel.setVisible(false);
        puzzleResultLabel.setManaged(false);
        puzzleCardResultLabel.setText("");
        puzzleCardResultLabel.setVisible(false);
        puzzleCardResultLabel.setManaged(false);
        setGameState(GameState.EXPLORATION);
        updateMapGrid();
    }

    private void setActivePuzzleType(PuzzleUIType type) {
        activePuzzleType = type;

        letterBlocksRow.setVisible(type == PuzzleUIType.SCRAMBLE);
        letterBlocksRow.setManaged(type == PuzzleUIType.SCRAMBLE);
        numberGuessRow.setVisible(type == PuzzleUIType.NUMBER_GUESS);
        numberGuessRow.setManaged(type == PuzzleUIType.NUMBER_GUESS);
        rpsButtonRow.setVisible(type == PuzzleUIType.RPS);
        rpsButtonRow.setManaged(type == PuzzleUIType.RPS);
        selectionButtonsRow.setVisible(type == PuzzleUIType.SELECTION);
        selectionButtonsRow.setManaged(type == PuzzleUIType.SELECTION);
        puzzleInputField.setVisible(type == PuzzleUIType.SCRAMBLE || type == PuzzleUIType.RIDDLE);
        puzzleInputField.setManaged(type == PuzzleUIType.SCRAMBLE || type == PuzzleUIType.RIDDLE);
        btnSubmit.setVisible(type == PuzzleUIType.SCRAMBLE || type == PuzzleUIType.RIDDLE);
        btnSubmit.setManaged(type == PuzzleUIType.SCRAMBLE || type == PuzzleUIType.RIDDLE);
        puzzleResultLabel.setVisible(false);
        puzzleResultLabel.setManaged(false);

        cardRow.setVisible(type == PuzzleUIType.POKER);
        cardRow.setManaged(type == PuzzleUIType.POKER);
        cardGhostPanel.setVisible(type == PuzzleUIType.POKER);
        cardGhostPanel.setManaged(type == PuzzleUIType.POKER);
        cardPlayerPanel.setVisible(type == PuzzleUIType.POKER);
        cardPlayerPanel.setManaged(type == PuzzleUIType.POKER);
        diceFaceLabel
                .setVisible(type == PuzzleUIType.DICE || type == PuzzleUIType.POKER || type == PuzzleUIType.SELECTION);
        diceFaceLabel
                .setManaged(type == PuzzleUIType.DICE || type == PuzzleUIType.POKER || type == PuzzleUIType.SELECTION);
        btnDraw.setVisible(type == PuzzleUIType.POKER);
        btnDraw.setManaged(type == PuzzleUIType.POKER);
        btnStand.setVisible(type == PuzzleUIType.DICE);
        btnStand.setManaged(type == PuzzleUIType.DICE);

        if (type == PuzzleUIType.POKER) {
            diceFaceLabel.setText("Draw a card to compare against the ghost.");
        } else if (type == PuzzleUIType.DICE) {
            diceFaceLabel.setText("Roll the die. You need the highest value: 6.");
        } else {
            diceFaceLabel.setText("Roll result appears here.");
        }
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

        boolean showCombatInfo = state == GameState.COMBAT;
        if (combatInfoSectionBox != null) {
            combatInfoSectionBox.setVisible(showCombatInfo);
            combatInfoSectionBox.setManaged(showCombatInfo);
        }

        boolean showPuzzleInfo = state == GameState.PUZZLE_TEXT || state == GameState.PUZZLE_CARD;
        if (puzzleInfoSectionBox != null) {
            puzzleInfoSectionBox.setVisible(showPuzzleInfo);
            puzzleInfoSectionBox.setManaged(showPuzzleInfo);
        }
    }

    // ------------------------------------------------------------------
    // Save / Load dialog helpers
    // ------------------------------------------------------------------

    /** Open a modal slot-selection dialog and save to the chosen slot. */
    private void showSaveDialog() {
        Stage dialog = buildSlotDialog("Save Game – Choose a Slot", true);
        dialog.showAndWait();
    }

    /** Open a modal slot-selection dialog and load from the chosen slot. */
    private void showLoadDialog() {
        Stage dialog = buildSlotDialog("Load Game – Choose a Slot", false);
        dialog.showAndWait();
    }

    /**
     * Build a styled, modal slot-selection window.
     *
     * @param title  title bar text
     * @param isSave true → save mode; false → load mode
     */
    private Stage buildSlotDialog(String title, boolean isSave) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #111827;");

        Label header = new Label(title);
        header.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        header.setTextFill(Color.web(UI_TEXT_COLOR));

        root.getChildren().add(header);

        for (int slot = 1; slot <= SaveManager.NUM_SLOTS; slot++) {
            final int s = slot;
            String summary = SaveManager.getSlotSummary(slot);
            Button btn = new Button(summary);
            btn.setPrefWidth(400);
            btn.setStyle("-fx-background-color: #1f2937; -fx-text-fill: #e5e7eb;"
                    + " -fx-border-color: #374151; -fx-border-radius: 6; -fx-background-radius: 6;"
                    + " -fx-font-size: 13px;");
            btn.setOnAction(ev -> {
                dialog.close();
                if (isSave) {
                    performSave(s);
                } else {
                    performLoad(s);
                }
            });
            root.getChildren().add(btn);
        }

        Button btnCancel = new Button("Cancel");
        btnCancel.setPrefWidth(400);
        btnCancel.setStyle("-fx-background-color: #374151; -fx-text-fill: #9ca3af;"
                + " -fx-border-color: #4b5563; -fx-border-radius: 6; -fx-background-radius: 6;"
                + " -fx-font-size: 13px;");
        btnCancel.setOnAction(ev -> dialog.close());
        root.getChildren().add(btnCancel);

        dialog.setScene(new Scene(root, 460, 240, Color.web("#111827")));
        return dialog;
    }

    /** Save current game state to the given slot and show a brief confirmation. */
    private void performSave(int slot) {
        boolean ok = SaveManager.saveGame(slot, player, game);
        String msg = ok
                ? "Game saved to Slot " + slot + "."
                : "Save failed for Slot " + slot + " – check console for details.";
        outputText(msg);
    }

    /** Load game state from the given slot and refresh all UI panels. */
    private void performLoad(int slot) {
        Player loaded = SaveManager.loadGame(slot, game);
        if (loaded == null) {
            outputText("Load failed for Slot " + slot + " – check console for details.");
            return;
        }
        player = loaded;
        selectedRoomNumber = player.getLocation();
        Room startRoom = game.getRoomByNumber(selectedRoomNumber);
        if (startRoom != null) {
            startRoom.setVisited();
            player.setCurrentRoom(startRoom);
        }
        updatePlayerInfo();
        updateRoomInfo();
        updateMapGrid();
        combatSystem = new CombatSystem(game, createCombatViewBridge());
        setGameState(GameState.EXPLORATION);
        startCombatIfPresent(startRoom);
        outputText("Slot " + slot + " loaded. Welcome back, " + player.getName() + "!");
    }

    /** Reset game to factory defaults (equivalent to New Game). */
    private void resetGame() {
        game = new Game();
        if (!game.mapGenerate("rooms.csv")) {
            System.out.println("Failed to load rooms.csv during reset.");
        }
        if (!game.loadPuzzles("puzzles.csv")) {
            System.out.println("Failed to load puzzles.csv during reset.");
        }
        if (!game.loadMonstersFromCsv("monsters.csv")) {
            System.out.println("Failed to load monsters.csv during reset.");
        }
        player = Player.loadFromCsv(
                SaveManager.SAVES_DIR + SaveManager.BASE_SLOT + "/player.csv", game);
        selectedRoomNumber = player.getLocation();
        Room startRoom = game.getRoomByNumber(selectedRoomNumber);
        if (startRoom != null) {
            startRoom.setVisited();
            player.setCurrentRoom(startRoom);
        }
        combatSystem = new CombatSystem(game, createCombatViewBridge());
        updatePlayerInfo();
        updateRoomInfo();
        updateMapGrid();
        setGameState(GameState.EXPLORATION);
        startCombatIfPresent(startRoom);
    }

    private void runCombatAction(String action) {
        if (combatSystem == null || !combatSystem.isInCombat()) {
            outputText("You are not currently in combat.");
            return;
        }
        combatSystem.executeCombatCycle(action, player);
        updatePlayerInfo();
        updateRoomInfo();
        updateMapGrid();
        if (!combatSystem.isInCombat()) {
            setGameState(GameState.EXPLORATION);
        }
    }

    private void startCombatIfPresent(Room room) {
        if (room == null || combatSystem == null) {
            return;
        }
        Monster monster = room.getMonster();
        if (monster != null && monster.isAlive()) {
            combatSystem.startCombat(player, monster, room);
        }
    }

    private void startMonsterTest(String monsterId) {
        if (game == null || combatSystem == null || player == null) {
            return;
        }
        Monster template = game.getMonsterById(monsterId);
        if (template == null && game.loadMonstersFromCsv("monsters.csv")) {
            template = game.getMonsterById(monsterId);
        }
        if (template == null) {
            outputText("Monster test setup failed for " + monsterId + ".");
            return;
        }

        Monster testMonster = template.createCombatTestCopy();
        Room arena = new Room("TEST", "Combat Test Arena", "A safe arena for combat debugging.");
        arena.setMonster(testMonster);
        player.setCurrentRoom(
                player.getCurrentRoom() != null ? player.getCurrentRoom() : game.getRoomByNumber(player.getLocation()));
        outputText("Starting test combat against " + testMonster.getName() + ".");
        combatSystem.startCombat(player, testMonster, arena);
    }

    private GameView createCombatViewBridge() {
        return new GameView() {
            @Override
            public void showCombatStart(String monsterName, int level) {
                setGameState(GameState.COMBAT);
                outputText("Encountered " + monsterName + " (Lv." + level + ")");
                Monster active = combatSystem != null ? combatSystem.getActiveMonster() : null;
                if (active != null) {
                    lblEnemyName.setText(active.getName() + "  Lv." + active.getLevel());
                    lblWeakness.setText("Weak: " + formatTypeLabel(active.getWeakType(), active.getWeakModifier()));
                    lblResistance
                            .setText("Resists: " + formatTypeLabel(active.getResistType(), active.getResistModifier()));
                    lblMonsterStatus.setText("Status: Active");
                }
            }

            @Override
            public void updateMonsterStats(String name, int currentHp, int maxHp) {
                lblEnemyName.setText(name);
                lblEnemyHPValues.setText(currentHp + " / " + maxHp);
                pbEnemyHP.setProgress(maxHp <= 0 ? 0.0 : (currentHp / (double) maxHp));
                Monster active = combatSystem != null ? combatSystem.getActiveMonster() : null;
                if (active != null) {
                    lblMonsterStatus.setText("Status: " + buildMonsterStatusString(active));
                }
            }

            @Override
            public void displayMessage(String message, MessageType type) {
                outputText(message);
            }

            @Override
            public void updatePlayerStats(int currentHp, int maxHp, int atk, int def) {
                updatePlayerInfo();
            }

            @Override
            public void setCombatActionsEnabled(boolean enabled) {
                btnAttack.setDisable(!enabled);
                btnDefend.setDisable(!enabled);
                btnUseItem.setDisable(!enabled);
                btnEquip.setDisable(!enabled);
                btnFlee.setDisable(!enabled);
                btnInspect.setDisable(!enabled);
            }

            @Override
            public void showInspectEnemy(String info) {
                outputText(info);
            }

            @Override
            public void showGameOverScreen() {
                setGameState(GameState.GAME_OVER);
                outputText("You have been defeated.");
            }

            @Override
            public void showWinScreen(WinType winType) {
                setGameState(GameState.GAME_OVER);
                outputText(winType == WinType.CLEANSE ? "Cleanse ending achieved." : "Escape ending achieved.");
            }
        };
    }

    private String formatTypeLabel(String type, double modifier) {
        if (type == null || "NONE".equalsIgnoreCase(type)) {
            return "None";
        }
        int pct = (int) Math.round(Math.abs(modifier * 100.0));
        return type + " (" + pct + "%)";
    }

    private String buildMonsterStatusString(Monster monster) {
        if (monster == null) {
            return "\u2014";
        }
        List<String> parts = new java.util.ArrayList<>();
        if (monster.hasFortifyActive()) {
            parts.add("Fortified");
        }
        if (monster.hasBarricaded()) {
            parts.add("Barricaded");
        }
        if (monster.isSummoning()) {
            parts.add("Summoning\u2026");
        }
        if (monster.isSummonActive()) {
            parts.add("Summon active");
        }
        return parts.isEmpty() ? "Active" : String.join(", ", parts);
    }

    public static void main(String[] args) {
        launch();
    }
}
