package view;

import controller.CombatSystem;
import javafx.beans.binding.Bindings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import model.Armor;
import model.Consumable;
import model.FlavorText;
import model.Game;
import model.Item;
import model.KeyItem;
import model.Monster;
import model.Player;
import model.Room;
import model.Puzzle;
import model.PuzzleLoader;
import model.SaveManager;
import model.Weapon;
import view.GameView.WinType;

/**
 * Full JavaFX GUI with a three-column layout:
 * Left panel: player and room information.
 * Center panel: narrative output log with state-driven button panels.
 * Right panel: interactive map preview and inventory list.
 * Includes save/load dialogs supporting up to three named save slots.
 * 
 * @author Subhan Choudhry
 */
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
    private static final double MAP_NODE_SIZE = 14;
    private static final double MAP_PADDING = 20;
    private static final String BGM_RESOURCE_PATH = "/sound/lavender_town_theme.mp3";
    private static final String[] BGM_RESOURCE_CANDIDATES = {
            BGM_RESOURCE_PATH,
            "/sound/lavender_town_theme.wav"
    };
    private static final double BGM_DEFAULT_VOLUME = 0.25;
    // Use 5% increments for finer volume control
    private static final double BGM_VOLUME_STEP = 0.05;

    private VBox saveQuitBox;
    private VBox mainMenuPanel;
    private VBox explorationPanel;
    private VBox combatPanel;
    private VBox puzzleTextPanel;
    private VBox puzzleCardPanel;
    private VBox gameOverPanel;
    private VBox centerColumn;
    private VBox audioControlsBox;
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
    private Button btnPickup;
    private Button btnUseFromBag;
    private Button btnEquipFromBag;
    private Button btnKick;
    // status button removed; status is shown under player info panel
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
    private Button btnToggleMute;
    private Button btnVolumeDown;
    private Button btnVolumeUp;
    private Button btnAudioRetry;
    private Button btnAudioTest;

    private Label lblPuzzleNarrative;
    private Label lblPuzzleAttemptsInline;
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
    private int activeSaveSlot = 0; // 0 = no active slot (base/new game)
    private int selectedRoomNumber;
    private Pane mapCanvas;
    private ListView<OutputLine> outputArea;
    private int outputLineCounter = 0;
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
    private Label lblGameOverCause;
    private Label lblGameOverRoom;
    private VBox combatInfoSectionBox;
    private VBox puzzleInfoSectionBox;
    private ListView<String> lstStatusEffects;
    private Label lblStatusHeader;
    private Label lblStatusDescription;
    private Label lblVolumeValue;
    private Label lblAudioStatus;
    private ListView<String> lstInventory;
    private CombatSystem combatSystem;
    private MediaPlayer bgmPlayer;
    private double cachedVolumeBeforeMute = BGM_DEFAULT_VOLUME;

    private record MapPoint(double x, double y, int rowIndex, int colIndex) {
    }

    private record MapEdge(int fromRoom, int toRoom) {
    }

    private record OutputLine(int number, String message) {
    }

    private record RoomEntrySnapshot(int roomNumber, int hp, int baseAttack, int baseDefense,
            List<Item> inventory, Weapon equippedWeapon, Armor equippedArmor) {
    }

    private RoomEntrySnapshot lastRoomEntrySnapshot;

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
        if (!game.loadItemsFromCsv("items.csv", "weapons.csv", "armor.csv", "consumables.csv")) {
            System.out.println("Failed to load items from data/items.csv.");
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
        root.setFocusTraversable(true);

        VBox leftColumn = createLeftColumn();
        VBox center = createCenterColumn();
        VBox rightColumn = createRightColumn();

        root.setLeft(leftColumn);
        root.setCenter(center);
        root.setRight(rightColumn);

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        boolean smallDisplay = visualBounds.getWidth() < 1280 || visualBounds.getHeight() < 720;

        double initialWidth = Math.min(1180, visualBounds.getWidth() * 0.92);
        double initialHeight = Math.min(680, visualBounds.getHeight() * 0.92);

        Scene scene = new Scene(root, initialWidth, initialHeight, Color.web("#111827"));
        stage.setMinWidth(960);
        stage.setMinHeight(600);

        leftColumn.setMinWidth(280);
        leftColumn.setPrefWidth(340);
        leftColumn.setMaxWidth(340);
        rightColumn.setMinWidth(320);
        rightColumn.setPrefWidth(320);
        rightColumn.setMaxWidth(320);
        center.prefWidthProperty().bind(
                scene.widthProperty().subtract(leftColumn.prefWidthProperty()).subtract(rightColumn.prefWidthProperty())
                        .subtract(64));
        center.maxWidthProperty().bind(center.prefWidthProperty());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleHotkeys);

        stage.setTitle("Text Explorer UI Template");
        stage.setScene(scene);
        if (smallDisplay) {
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("Press ESC to exit fullscreen");
        } else {
            stage.centerOnScreen();
        }
        stage.setAlwaysOnTop(true);
        stage.show();
        Platform.runLater(() -> {
            stage.toFront();
            stage.requestFocus();
            root.requestFocus();
            stage.setAlwaysOnTop(false);
        });

        initializeBgm();

        updatePlayerInfo();
        updateRoomInfo();
        updateMapGrid();
        setGameState(GameState.MAIN_MENU);
        restoreKeyboardFocus();
    }

    @Override
    public void stop() {
        stopBgm();
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
        saveQuitBox = createSaveQuitBox();
        audioControlsBox = createAudioControlsBox();

        VBox wrapper = new VBox(8, scrollPane, saveQuitBox, audioControlsBox);
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
        centerColumn.setFocusTraversable(true);
        centerColumn.setPrefWidth(520);
        centerColumn.setMinWidth(360);

        outputArea = new ListView<>();
        outputArea.setFocusTraversable(true);
        outputArea.setStyle("-fx-background-color: #1f2937; -fx-control-inner-background: #1f2937;"
                + "-fx-border-color: #374151; -fx-border-width: 1;");
        outputArea.setFixedCellSize(-1);
        outputArea.setMinHeight(220);
        outputArea.setMaxWidth(Double.MAX_VALUE);
        outputArea.prefHeightProperty().bind(Bindings.max(220.0, centerColumn.heightProperty().subtract(280.0)));
        outputArea.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OutputLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                Label index = new Label(String.format("%03d", item.number()));
                index.setTextFill(Color.web("#93c5fd"));
                index.setFont(Font.font(UI_FONT, FontWeight.BOLD, 12));
                index.setMinWidth(42);

                Label message = new Label(item.message());
                message.setTextFill(Color.web("#e5e7eb"));
                message.setFont(Font.font(UI_FONT, 12));
                message.setWrapText(true);
                message.setMaxWidth(Math.max(180, outputArea.getWidth() - 100));

                HBox row = new HBox(8, index, message);
                row.setAlignment(Pos.TOP_LEFT);
                row.setPadding(new Insets(4, 8, 4, 8));
                HBox.setHgrow(message, Priority.ALWAYS);

                setGraphic(row);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                boolean odd = (item.number() % 2) == 1;
                setStyle(odd
                        ? "-fx-background-color: #0f172a;"
                        : "-fx-background-color: #111827;");
            }
        });
        outputText(FlavorText.get("GAME_START",
                "Welcome to Text Explorer. This output panel will display the current scene, story text, and game results."));
        outputText("Use the buttons below to navigate game state and control the application.");

        StackPane statePanels = new StackPane();
        statePanels.setPrefHeight(290);
        statePanels.setMinHeight(250);
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
        rightColumn.setMinWidth(320);
        rightColumn.setMaxWidth(320);
        rightColumn.setAlignment(Pos.TOP_CENTER);

        Label mapTitle = new Label("Map Preview");
        mapTitle.setFont(Font.font(UI_FONT, FontWeight.BOLD, 18));
        mapTitle.setTextFill(Color.web(UI_TEXT_COLOR));
        mapTitle.setMaxWidth(Double.MAX_VALUE);
        mapTitle.setAlignment(Pos.CENTER);

        mapCanvas = new Pane();
        mapCanvas.setMinSize(280, 280);
        mapCanvas.setPrefSize(280, 280);
        mapCanvas.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        mapCanvas.widthProperty().addListener((obs, oldVal, newVal) -> updateMapGrid());
        mapCanvas.heightProperty().addListener((obs, oldVal, newVal) -> updateMapGrid());

        StackPane mapFrame = new StackPane(mapCanvas);
        mapFrame.setAlignment(Pos.CENTER);
        mapFrame.setPadding(new Insets(8));
        mapFrame.setMaxWidth(Double.MAX_VALUE);
        mapFrame.setMinHeight(280);
        mapFrame.setStyle(UI_PANEL_STYLE);

        Label mapHint = new Label(
                "Current location and visited rooms are shown on the map. Click a room to view details.");
        mapHint.setTextFill(Color.web(UI_SECONDARY_COLOR));
        mapHint.setWrapText(true);
        mapHint.setMaxWidth(Double.MAX_VALUE);
        mapHint.prefWidthProperty().bind(rightColumn.widthProperty().subtract(8));

        VBox inventorySection = createSectionBox("Inventory", createInventorySection());
        // Save/quit menu moved to left column per UI layout changes
        rightColumn.getChildren().addAll(mapTitle, mapFrame, mapHint, inventorySection);
        return rightColumn;
    }

    private void updateMapGrid() {
        if (game == null || mapCanvas == null) {
            return;
        }
        mapCanvas.getChildren().clear();

        List<Room> allRooms = game.getAllRooms();
        if (allRooms == null || allRooms.isEmpty()) {
            return;
        }

        double width = Math.max(240, mapCanvas.getWidth() > 0 ? mapCanvas.getWidth() : mapCanvas.getPrefWidth());
        double height = Math.max(240, mapCanvas.getHeight() > 0 ? mapCanvas.getHeight() : mapCanvas.getPrefHeight());

        Set<Integer> uniqueRows = new TreeSet<>();
        Set<Integer> uniqueCols = new TreeSet<>();
        Map<Integer, int[]> roomCoordsByNumber = new HashMap<>();

        for (Room room : allRooms) {
            int roomNumber = game.getRoomNumberById(room.getRoomId());
            int[] coord = game.getRoomCoordinates(room.getRoomId());
            if (roomNumber <= 0 || coord == null) {
                continue;
            }
            roomCoordsByNumber.put(roomNumber, coord);
            uniqueRows.add(coord[0]);
            uniqueCols.add(coord[1]);
        }

        if (roomCoordsByNumber.isEmpty()) {
            return;
        }

        List<Integer> rowList = new ArrayList<>(uniqueRows);
        List<Integer> colList = new ArrayList<>(uniqueCols);
        double xStep = rowList.size() <= 1 ? 1
                : Math.max(1, (height - (MAP_PADDING * 2)) / (rowList.size() - 1));
        double yStep = colList.size() <= 1 ? 1
                : Math.max(1, (width - (MAP_PADDING * 2)) / (colList.size() - 1));

        Map<Integer, MapPoint> points = new HashMap<>();
        for (Map.Entry<Integer, int[]> entry : roomCoordsByNumber.entrySet()) {
            int roomNumber = entry.getKey();
            int[] coord = entry.getValue();
            int rowIndex = rowList.indexOf(coord[0]);
            int colIndex = colList.indexOf(coord[1]);
            double x = MAP_PADDING + (colIndex * yStep);
            double y = MAP_PADDING + (rowIndex * xStep);
            points.put(roomNumber, new MapPoint(x, y, rowIndex, colIndex));
        }

        List<MapEdge> edges = new ArrayList<>();
        for (Room room : allRooms) {
            int fromNumber = game.getRoomNumberById(room.getRoomId());
            MapPoint fromPoint = points.get(fromNumber);
            if (fromPoint == null) {
                continue;
            }

            for (int dir = 0; dir < 4; dir++) {
                int toNumber = room.getExit(dir);
                if (toNumber <= 0 || toNumber <= fromNumber) {
                    continue;
                }
                if (!points.containsKey(toNumber)) {
                    continue;
                }
                edges.add(new MapEdge(fromNumber, toNumber));
            }
        }

        for (MapEdge edge : edges) {
            Room fromRoom = game.getRoomByNumber(edge.fromRoom());
            Room toRoom = game.getRoomByNumber(edge.toRoom());
            if (fromRoom == null || toRoom == null) {
                continue;
            }
            MapPoint a = points.get(edge.fromRoom());
            MapPoint b = points.get(edge.toRoom());
            if (a == null || b == null) {
                continue;
            }

            boolean fromVisible = fromRoom.isVisited() || (player != null && player.getLocation() == edge.fromRoom());
            boolean toVisible = toRoom.isVisited() || (player != null && player.getLocation() == edge.toRoom());
            boolean barricadedEdge = isEdgeBarricaded(fromRoom, edge.toRoom())
                    || isEdgeBarricaded(toRoom, edge.fromRoom());
            Color color = barricadedEdge
                    ? Color.web("#3b0764")
                    : (fromVisible || toVisible) ? Color.web("#64748b") : Color.web("#1e293b");
            double stroke = barricadedEdge ? 3.0 : (fromVisible || toVisible) ? 2.0 : 1.0;

            // Keep links strictly straight so map paths do not render with divets.
            if (a.rowIndex() == b.rowIndex()) {
                MapPoint left = a.colIndex() <= b.colIndex() ? a : b;
                MapPoint right = a.colIndex() <= b.colIndex() ? b : a;
                drawHorizontalEdge(left, right, color, stroke);
            } else if (a.colIndex() == b.colIndex()) {
                MapPoint top = a.rowIndex() <= b.rowIndex() ? a : b;
                MapPoint bottom = a.rowIndex() <= b.rowIndex() ? b : a;
                drawVerticalEdge(top, bottom, color, stroke);
            } else {
                drawLine(a.x(), a.y(), b.x(), b.y(), color, stroke);
            }
        }

        for (Room room : allRooms) {
            int roomNumber = game.getRoomNumberById(room.getRoomId());
            MapPoint point = points.get(roomNumber);
            if (point == null) {
                continue;
            }

            boolean current = player != null && player.getLocation() == roomNumber;
            boolean visited = room.isVisited();

            Rectangle node = new Rectangle(MAP_NODE_SIZE, MAP_NODE_SIZE);
            node.setArcWidth(4);
            node.setArcHeight(4);
            node.setStroke(Color.web("#0f172a"));
            node.setStrokeWidth(1.0);
            node.setFill(current ? Color.web("#2563eb") : visited ? Color.web("#60a5fa") : Color.web("#1f2937"));

            node.setLayoutX(point.x() - (MAP_NODE_SIZE / 2));
            node.setLayoutY(point.y() - (MAP_NODE_SIZE / 2));

            if (visited || current) {
                final int selected = roomNumber;
                node.setOnMouseClicked(e -> {
                    selectedRoomNumber = selected;
                    updateRoomInfo();
                    Room selectedRoom = game.getRoomByNumber(selected);
                    if (selectedRoom != null) {
                        outputText("Selected room: " + selectedRoom.getRoomId() + " - " + selectedRoom.getName());
                    }
                });
            }
            mapCanvas.getChildren().add(node);
        }
    }

    private boolean isEdgeBarricaded(Room fromRoom, int toRoomNumber) {
        if (fromRoom == null || toRoomNumber <= 0) {
            return false;
        }
        if (fromRoom.getExit(0) == toRoomNumber && fromRoom.isBarricaded("N")) {
            return true;
        }
        if (fromRoom.getExit(1) == toRoomNumber && fromRoom.isBarricaded("E")) {
            return true;
        }
        if (fromRoom.getExit(2) == toRoomNumber && fromRoom.isBarricaded("S")) {
            return true;
        }
        return fromRoom.getExit(3) == toRoomNumber && fromRoom.isBarricaded("W");
    }

    private void drawHorizontalEdge(MapPoint left, MapPoint right, Color color, double stroke) {
        double startX = left.x() + (MAP_NODE_SIZE / 2.0);
        double endX = right.x() - (MAP_NODE_SIZE / 2.0);
        double y = left.y();
        drawLine(startX, y, endX, y, color, stroke);
    }

    private void drawVerticalEdge(MapPoint top, MapPoint bottom, Color color, double stroke) {
        double startY = top.y() + (MAP_NODE_SIZE / 2.0);
        double endY = bottom.y() - (MAP_NODE_SIZE / 2.0);
        double x = top.x();
        drawLine(x, startY, x, endY, color, stroke);
    }

    private void drawLine(double x1, double y1, double x2, double y2, Color color, double stroke) {
        Line edge = new Line(x1, y1, x2, y2);
        edge.setStroke(color);
        edge.setStrokeWidth(stroke);
        mapCanvas.getChildren().add(edge);
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
        // Make inventory tall enough to display up to 7 distinct slots
        lstInventory.setPrefHeight(220);
        lstInventory.setStyle(
                "-fx-control-inner-background: #111827; -fx-background-color: #111827; -fx-border-color: #374151; -fx-border-width: 1; -fx-font-size: 14; -fx-padding: 0;");
        lstInventory.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 2) {
                handleInventoryListAction();
            }
        });
        lstInventory.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleInventoryListAction();
            }
        });
        // Display items with icons from resource folder
        lstInventory.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox itemBox = new HBox(8);
                    itemBox.setAlignment(Pos.CENTER_LEFT);

                    ImageView iconView = getItemIcon(item);
                    if (iconView != null) {
                        itemBox.getChildren().add(iconView);
                    }

                    Label label = new Label(item);
                    label.setStyle("-fx-text-fill: #e5e7eb; -fx-wrap-text: true;");
                    label.setWrapText(true);
                    itemBox.getChildren().add(label);

                    setGraphic(itemBox);
                }
            }
        });

        ContextMenu inventoryMenu = new ContextMenu();
        MenuItem inspectItem = new MenuItem("Inspect");
        MenuItem equipItem = new MenuItem("Equip");
        MenuItem useItem = new MenuItem("Use");
        MenuItem dropItem = new MenuItem("Drop");
        MenuItem unequipItem = new MenuItem("Unequip");
        inspectItem.setOnAction(e -> inventoryQuickAction("Inspect"));
        equipItem.setOnAction(e -> inventoryQuickAction("Equip"));
        useItem.setOnAction(e -> inventoryQuickAction("Use"));
        dropItem.setOnAction(e -> inventoryDropSelected());
        unequipItem.setOnAction(e -> inventoryUnequipSelected());
        inventoryMenu.getItems().addAll(inspectItem, useItem, equipItem, dropItem, unequipItem);

        lstInventory.setOnContextMenuRequested(e -> {
            String selectedLabel = lstInventory.getSelectionModel().getSelectedItem();
            Item selectedItem = selectedLabel == null ? null : player.getItemByName(labelToItemName(selectedLabel));
            if (selectedItem == null) {
                return;
            }
            equipItem.setDisable(!(selectedItem instanceof Weapon || selectedItem instanceof Armor));
            useItem.setDisable(!(selectedItem instanceof Consumable));
            inventoryMenu.show(lstInventory, e.getScreenX(), e.getScreenY());
        });

        lstInventory.setOnMousePressed(e -> {
            if (!e.isSecondaryButtonDown()) {
                inventoryMenu.hide();
            }
        });
        // Add quick-action buttons under the inventory list
        HBox invActions = new HBox(8);
        invActions.setAlignment(Pos.CENTER);
        Button invInspect = new Button("Inspect");
        Button invDrop = new Button("Drop");
        Button invUnequip = new Button("Unequip");
        invInspect.setOnAction(e -> inventoryInspectSelected());
        invDrop.setOnAction(e -> inventoryDropSelected());
        invUnequip.setOnAction(e -> inventoryUnequipSelected());
        invInspect.setPrefWidth(100);
        invDrop.setPrefWidth(100);
        invUnequip.setPrefWidth(100);
        invActions.getChildren().addAll(invInspect, invDrop, invUnequip);

        box.getChildren().addAll(lstInventory, invActions);
        return box;
    }

    private VBox createRoomInfoSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        lblRoomName = createSectionLabel("Guard Post");
        lblRoomName.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        lblRoomName.setWrapText(true);
        lblRoomID = createSectionLabel("GH-02 · Guardhouse");
        lblRoomID.setTextFill(Color.web("#9ca3af"));
        lblRoomID.setWrapText(true);
        lblExits = createSectionLabel("Exits: N  E  W");
        lblExits.setWrapText(true);
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
        lblEnemyName.setWrapText(true);
        Label lblEnemyHP = createSectionLabel("HP:");
        pbEnemyHP = new ProgressBar(0.0);
        pbEnemyHP.setPrefWidth(220);
        lblEnemyHPValues = createSectionLabel("0 / 0");
        lblEnemyHPValues.setWrapText(true);
        lblWeakness = createSectionLabel("Weak: None");
        lblWeakness.setWrapText(true);
        lblResistance = createSectionLabel("Resists: None");
        lblResistance.setWrapText(true);
        lblMonsterStatus = createSectionLabel("Status: —");
        lblMonsterStatus.setWrapText(true);

        box.getChildren().addAll(lblEnemyName, lblEnemyHP, pbEnemyHP, lblEnemyHPValues, new Separator(), lblWeakness,
                lblResistance, lblMonsterStatus);
        return box;
    }

    private VBox createPuzzleInfoSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        lblPuzzleNameInfo = createSectionLabel("—");
        lblPuzzleNameInfo.setFont(Font.font(UI_FONT, FontWeight.BOLD, 14));
        lblPuzzleNameInfo.setWrapText(true);
        Label lblAttemptsLabel = createSectionLabel("Attempts remaining:");
        lblAttemptsLabel.setWrapText(true);
        lblPuzzleAttemptsInfo = createSectionLabel("—");
        lblPuzzleAttemptsInfo.setFont(Font.font(UI_FONT, FontWeight.BOLD, 16));
        lblPuzzleAttemptsInfo.setTextFill(Color.web("#34d399"));
        lblWrongAnswersInfo = createSectionLabel("Wrong so far: —");
        lblWrongAnswersInfo.setWrapText(true);
        lblCardTotalInfo = createSectionLabel("");
        lblCardTotalInfo.setVisible(false);
        lblCardTotalInfo.setManaged(false);
        lblCardTotalInfo.setWrapText(true);

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
                    FlavorText.get("QUIT_CONFIRM",
                            "Return to the main menu? Use Quit on the main menu to exit the game."),
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Exit");
            confirm.setHeaderText("Exit to Main Menu");
            styleDialog(confirm);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    setGameState(GameState.MAIN_MENU);
                }
            });
            restoreKeyboardFocus();
        });

        box.getChildren().addAll(title, btnSaveMenu, btnQuitMenu);
        return box;
    }

    private VBox createAudioControlsBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.setStyle(UI_PANEL_STYLE);

        Label title = createSectionLabel("Audio");
        title.setFont(Font.font(UI_FONT, FontWeight.BOLD, 14));

        lblVolumeValue = createSectionLabel("Volume: --");
        lblAudioStatus = createSectionLabel("Audio: Initializing...");
        lblAudioStatus.setWrapText(true);

        btnToggleMute = new Button("Mute");
        btnToggleMute.setPrefWidth(Double.MAX_VALUE);
        btnToggleMute.setOnAction(e -> toggleMute());

        btnVolumeDown = new Button("Volume -5%");
        btnVolumeUp = new Button("Volume +5%");
        btnVolumeDown.setPrefWidth(120);
        btnVolumeUp.setPrefWidth(120);
        btnVolumeDown.setOnAction(e -> adjustBgmVolume(-BGM_VOLUME_STEP));
        btnVolumeUp.setOnAction(e -> adjustBgmVolume(BGM_VOLUME_STEP));

        HBox volumeButtons = new HBox(8, btnVolumeDown, btnVolumeUp);
        volumeButtons.setAlignment(Pos.CENTER);

        btnAudioRetry = new Button("Retry Audio Init");
        btnAudioTest = new Button("Test Play");
        btnAudioRetry.setPrefWidth(120);
        btnAudioTest.setPrefWidth(120);
        btnAudioRetry.setOnAction(e -> initializeBgm());
        btnAudioTest.setOnAction(e -> testBgmPlayback());

        HBox debugButtons = new HBox(8, btnAudioRetry, btnAudioTest);
        debugButtons.setAlignment(Pos.CENTER);

        box.getChildren().addAll(title, lblVolumeValue, lblAudioStatus, btnToggleMute, volumeButtons, debugButtons);
        updateAudioControls();
        return box;
    }

    private void initializeBgm() {
        if (bgmPlayer != null) {
            stopBgm();
        }

        try {
            java.net.URL resource = findBgmResource();
            if (resource == null) {
                logAudioDebug("BGM resource not found in candidates: " + String.join(", ", BGM_RESOURCE_CANDIDATES));
                setAudioStatus("Audio: resource missing");
                updateAudioControls();
                return;
            }

            logAudioDebug("Initializing BGM from: " + resource);
            Media media = new Media(resource.toExternalForm());
            media.setOnError(() -> {
                logAudioDebug("Media decode error: " + media.getError());
                setAudioStatus("Audio error: media decode failed");
                updateAudioControls();
            });

            bgmPlayer = new MediaPlayer(media);
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgmPlayer.setVolume(BGM_DEFAULT_VOLUME);
            cachedVolumeBeforeMute = BGM_DEFAULT_VOLUME;

            bgmPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                setAudioStatus("Audio status: " + newStatus);
                updateAudioControls();
            });

            bgmPlayer.setOnReady(() -> {
                logAudioDebug("BGM ready. Starting playback.");
                setAudioStatus("Audio status: READY");
                bgmPlayer.play();
                updateAudioControls();
            });
            bgmPlayer.setOnPlaying(() -> {
                setAudioStatus("Audio status: PLAYING");
                logAudioDebug("BGM is playing.");
            });
            bgmPlayer.setOnPaused(() -> setAudioStatus("Audio status: PAUSED"));
            bgmPlayer.setOnStopped(() -> {
                setAudioStatus("Audio status: STOPPED");
                // Restart BGM if it stops unexpectedly (unless muted)
                if (bgmPlayer != null && !bgmPlayer.isMute() && bgmPlayer.getCycleCount() != 0) {
                    Platform.runLater(() -> {
                        if (bgmPlayer != null) {
                            bgmPlayer.seek(Duration.ZERO);
                            bgmPlayer.play();
                            logAudioDebug("BGM restarted after stop.");
                        }
                    });
                }
            });
            bgmPlayer.setOnStalled(() -> {
                setAudioStatus("Audio status: STALLED");
                logAudioDebug("BGM stalled during playback.");
            });
            bgmPlayer.setOnHalted(() -> {
                setAudioStatus("Audio status: HALTED");
                logAudioDebug("BGM halted by media engine.");
                // Restart on halt unless muted
                if (bgmPlayer != null && !bgmPlayer.isMute()) {
                    Platform.runLater(() -> {
                        if (bgmPlayer != null) {
                            bgmPlayer.seek(Duration.ZERO);
                            bgmPlayer.play();
                            logAudioDebug("BGM restarted after halt.");
                        }
                    });
                }
            });
            bgmPlayer.setOnError(() -> {
                logAudioDebug("BGM playback error: " + bgmPlayer.getError());
                setAudioStatus("Audio error: playback failed");
                updateAudioControls();
            });

            setAudioStatus("Audio status: LOADING");
            updateAudioControls();
        } catch (Exception ex) {
            logAudioDebug("Unable to initialize BGM: " + ex.getMessage());
            setAudioStatus("Audio error: initialization failed");
            updateAudioControls();
        }
    }

    private java.net.URL findBgmResource() {
        for (String candidate : BGM_RESOURCE_CANDIDATES) {
            java.net.URL resource = getClass().getResource(candidate);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    private void stopBgm() {
        if (bgmPlayer == null) {
            return;
        }
        bgmPlayer.stop();
        bgmPlayer.dispose();
        bgmPlayer = null;
        setAudioStatus("Audio status: STOPPED");
        updateAudioControls();
    }

    private void testBgmPlayback() {
        if (bgmPlayer == null) {
            logAudioDebug("Test Play requested but audio player is unavailable.");
            setAudioStatus("Audio unavailable: use Retry Audio Init");
            updateAudioControls();
            return;
        }

        bgmPlayer.setMute(false);
        if (bgmPlayer.getVolume() <= 0.0) {
            bgmPlayer.setVolume(Math.max(BGM_VOLUME_STEP, cachedVolumeBeforeMute));
        }
        bgmPlayer.seek(Duration.ZERO);
        bgmPlayer.play();
        setAudioStatus("Audio test: restarting playback from 0s");
        logAudioDebug("Audio test playback started.");
        updateAudioControls();
    }

    private void toggleMute() {
        if (bgmPlayer == null) {
            return;
        }

        if (bgmPlayer.isMute()) {
            bgmPlayer.setMute(false);
            if (bgmPlayer.getVolume() <= 0) {
                bgmPlayer.setVolume(Math.max(BGM_VOLUME_STEP, cachedVolumeBeforeMute));
            }
        } else {
            cachedVolumeBeforeMute = Math.max(BGM_VOLUME_STEP, bgmPlayer.getVolume());
            bgmPlayer.setMute(true);
        }

        if (bgmPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
            bgmPlayer.seek(Duration.ZERO);
            bgmPlayer.play();
        }
        updateAudioControls();
    }

    private void adjustBgmVolume(double delta) {
        if (bgmPlayer == null) {
            return;
        }

        double volume = clampVolume(bgmPlayer.getVolume() + delta);
        bgmPlayer.setMute(false);
        bgmPlayer.setVolume(volume);
        if (volume > 0) {
            cachedVolumeBeforeMute = volume;
        }
        updateAudioControls();
    }

    private double clampVolume(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private void updateAudioControls() {
        if (btnToggleMute == null || btnVolumeDown == null || btnVolumeUp == null || lblVolumeValue == null) {
            return;
        }

        boolean available = bgmPlayer != null;
        btnToggleMute.setDisable(!available);
        btnVolumeDown.setDisable(!available);
        btnVolumeUp.setDisable(!available);
        if (btnAudioRetry != null) {
            btnAudioRetry.setDisable(false);
        }
        if (btnAudioTest != null) {
            btnAudioTest.setDisable(!available);
        }

        if (!available) {
            lblVolumeValue.setText("Volume: N/A");
            btnToggleMute.setText("Mute");
            return;
        }

        int percent = (int) Math.round(bgmPlayer.getVolume() * 100.0);
        lblVolumeValue.setText("Volume: " + percent + "%" + (bgmPlayer.isMute() ? " (muted)" : ""));
        btnToggleMute.setText(bgmPlayer.isMute() ? "Unmute" : "Mute");
    }

    private void setAudioStatus(String status) {
        if (lblAudioStatus != null) {
            lblAudioStatus.setText(status);
        }
    }

    private void logAudioDebug(String message) {
        String line = "[AUDIO] " + message;
        System.out.println(line);
        if (outputArea != null) {
            outputText(line);
        }
    }

    private VBox createMainMenuPanel() {
        VBox box = new VBox();
        box.setFillWidth(true);
        box.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(14);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(18));
        content.setStyle("-fx-background-color: #111827;");

        btnNewGame = new Button("New Game [N]");
        btnLoadGame = new Button("Load Game [L]");
        btnQuitMainMenu = new Button("Quit [Q]");

        btnNewGame.setPrefWidth(240);
        btnLoadGame.setPrefWidth(240);
        btnQuitMainMenu.setPrefWidth(240);

        btnNewGame.setOnAction(e -> resetGame());
        btnLoadGame.setOnAction(e -> showLoadDialog());
        btnQuitMainMenu.setOnAction(e -> Platform.exit());

        content.getChildren().addAll(btnNewGame, btnLoadGame, btnQuitMainMenu);

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
        HBox itemRow = new HBox(10);
        itemRow.setAlignment(Pos.CENTER);
        btnSolvePuzzle = new Button("Solve Puzzle [R]");
        btnExploreAction = new Button("Explore [E]");
        btnInventory = new Button("Inventory [I]");
        btnPickup = new Button("Pick Up [P]");
        btnUseFromBag = new Button("Use Item [U]");
        btnEquipFromBag = new Button("Equip [G]");
        btnKick = new Button("Breach [K]");
        btnSolvePuzzle.setPrefWidth(150);
        btnExploreAction.setPrefWidth(150);
        btnInventory.setPrefWidth(150);
        btnPickup.setPrefWidth(150);
        btnUseFromBag.setPrefWidth(150);
        btnEquipFromBag.setPrefWidth(150);
        btnKick.setPrefWidth(150);
        btnKick.setVisible(false);
        btnKick.setManaged(false);
        btnSolvePuzzle.setOnAction(e -> attemptPuzzle());
        btnExploreAction.setOnAction(e -> {
            Room currentRoom = game.getRoomByNumber(player.getLocation());
            if (currentRoom != null) {
                outputText(buildRoomDetails(currentRoom));
            }
        });
        btnInventory.setOnAction(e -> outputInventorySummary());
        btnPickup.setOnAction(e -> pickUpSelectedRoomItem());
        btnUseFromBag.setOnAction(e -> useConsumableFromInventory());
        btnEquipFromBag.setOnAction(e -> equipItemFromInventory());
        btnKick.setOnAction(e -> breachBarricadedExit());
        actionRow.getChildren().addAll(btnSolvePuzzle, btnExploreAction, btnInventory);
        itemRow.getChildren().addAll(btnPickup, btnUseFromBag, btnEquipFromBag, btnKick);

        box.getChildren().addAll(compass, actionRow, itemRow);
        return box;
    }

    private VBox createCombatPanel() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));

        btnAttack = new Button("Attack [A]");
        btnAttack.setPrefWidth(260);
        btnAttack.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        btnDefend = new Button("Defend [D]");
        btnUseItem = new Button("Use Item [U]");
        btnEquip = new Button("Equip [G]");
        btnFlee = new Button("Flee [F]");
        btnInspect = new Button("Inspect [I]");
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
        box.setStyle(
                "-fx-background-color: #1e293b; -fx-border-color: #475569; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");

        lblPuzzleNarrative = createSectionLabel(
                "Puzzle narrative appears here. Use typed input or click a letter block.");
        lblPuzzleNarrative.setWrapText(true);
        lblPuzzleNarrative.setPrefWidth(520);
        lblPuzzleNarrative.setStyle("-fx-text-fill: #e2e8f0; -fx-font-style: italic; -fx-font-size: 13px;");

        lblPuzzleAttemptsInline = createSectionLabel("Attempts left: —");
        lblPuzzleAttemptsInline.setStyle("-fx-text-fill: #facc15; -fx-font-size: 15px; -fx-font-weight: bold;");
        lblPuzzleAttemptsInline.setMaxWidth(520);

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

        btnHint = new Button("Hint [H]");
        btnExplorePuzzleText = new Button("Cancel [Esc]");
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
        puzzleResultLabel.setWrapText(true);
        puzzleResultLabel.setPrefWidth(520);
        puzzleResultLabel.setMaxWidth(Double.MAX_VALUE);
        puzzleResultLabel.setLineSpacing(1.2);
        puzzleResultLabel.setMinHeight(Region.USE_PREF_SIZE);
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

        box.getChildren().addAll(lblPuzzleNarrative, lblPuzzleAttemptsInline, letterBlocksRow, numberGuessRow,
                rpsButtonRow, puzzleInputField,
                actionRow, puzzleResultLabel);
        return box;
    }

    private VBox createPuzzleCardPanel() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(18));
        box.setStyle(
                "-fx-background-color: #1e293b; -fx-border-color: #475569; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");

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

        btnDraw = new Button("Draw Card [Space]");
        btnStand = new Button("Roll Dice [Space]");
        btnExplorePuzzleCard = new Button("Cancel [Esc]");
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
        puzzleCardResultLabel.setWrapText(true);
        puzzleCardResultLabel.setPrefWidth(520);
        puzzleCardResultLabel.setMaxWidth(Double.MAX_VALUE);
        puzzleCardResultLabel.setLineSpacing(1.2);
        puzzleCardResultLabel.setMinHeight(Region.USE_PREF_SIZE);
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

        Label title = createSectionLabel("Game Over");
        title.setFont(Font.font(UI_FONT, FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f87171"));

        Label subtitle = createSectionLabel("Choose what to do next:");
        subtitle.setFont(Font.font(UI_FONT, 14));
        subtitle.setTextFill(Color.web("#e5e7eb"));

        btnLoadGameOver = new Button("Load [L]");
        btnRestartGameOver = new Button("Restart [R]");
        btnQuitGameOver = new Button("Quit [Q]");

        btnLoadGameOver.setPrefWidth(180);
        btnRestartGameOver.setPrefWidth(180);
        btnQuitGameOver.setPrefWidth(180);

        btnLoadGameOver.setOnAction(e -> showLoadDialog());
        btnRestartGameOver.setOnAction(e -> resetGame());
        btnQuitGameOver.setOnAction(e -> Platform.exit());

        box.getChildren().addAll(title, subtitle, btnLoadGameOver, btnRestartGameOver, btnQuitGameOver);
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

        // Preserve puzzle attempts across cancels; only final failure resets attempts

        puzzleResultLabel.setText("");
        puzzleResultLabel.setVisible(false);
        puzzleResultLabel.setManaged(false);
        puzzleCardResultLabel.setText("");
        puzzleCardResultLabel.setVisible(false);
        puzzleCardResultLabel.setManaged(false);

        puzzleInputField.clear();
        resetScrambleLetters();
        String narrativeText = formatPuzzleText(puzzle.getNarrative());
        String startText = formatPuzzleText(puzzle.start());
        if (activePuzzleType == PuzzleUIType.POKER || activePuzzleType == PuzzleUIType.DICE) {
            diceFaceLabel.setText(startText);
            lblPuzzleNarrative.setText(narrativeText);
        } else if (activePuzzleType == PuzzleUIType.SELECTION) {
            lblPuzzleNarrative.setText(narrativeText);
        } else {
            lblPuzzleNarrative.setText(narrativeText + "\n\n" + startText);
        }

        StringBuilder puzzleIntro = new StringBuilder();
        puzzleIntro.append("Puzzle: ").append(puzzle.getName())
                .append("\n").append(narrativeText);
        if (startText != null && !startText.isBlank()) {
            puzzleIntro.append("\n\n").append(startText);
        }
        if (puzzle.hasHint()) {
            puzzleIntro.append("\n\nPress H for a hint.");
        }
        outputText(puzzleIntro.toString());

        // Update left-panel puzzle info section
        if (lblPuzzleNameInfo != null) {
            lblPuzzleNameInfo.setText(puzzle.getName());
        }
        if (lblPuzzleAttemptsInfo != null) {
            lblPuzzleAttemptsInfo.setText(String.valueOf(puzzle.getAttempts()));
            lblPuzzleAttemptsInfo.setTextFill(Color.web("#34d399"));
        }
        if (lblPuzzleAttemptsInline != null) {
            lblPuzzleAttemptsInline.setText("Attempts left: " + puzzle.getAttempts());
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

        Platform.runLater(() -> {
            if (activePuzzleType == PuzzleUIType.SCRAMBLE || activePuzzleType == PuzzleUIType.RIDDLE) {
                puzzleInputField.requestFocus();
                puzzleInputField.selectAll();
            } else if (centerColumn != null) {
                centerColumn.requestFocus();
            }
        });
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

    private ImageView getItemIcon(String itemDisplay) {
        // Map specific items to unique icon variants based on rarity and item name
        if (itemDisplay == null || itemDisplay.isBlank()) {
            return null;
        }
        String upper = itemDisplay.toUpperCase();
        String iconPath = null;

        // Specific weapon mappings - higher tiers get more ornate icons
        if (upper.contains("ASSASSIN'S DAGGER") || upper.contains("ASSASSINS DAGGER")) {
            // Legendary dagger
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/ToolsAndWeapons/Weapons/NoShadow/Dagger5.png";
        } else if (upper.contains("CRUDE DAGGER")) {
            // Uncommon dagger - basic
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/ToolsAndWeapons/Weapons/NoShadow/Dagger5.png";
        } else if (upper.contains("DAGGER") || upper.contains("SPEAR") || upper.contains("AXE")) {
            // Other daggers/melee
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/ToolsAndWeapons/Weapons/NoShadow/Dagger5.png";
        }
        // Swords
        else if (upper.contains("STEEL SWORD")) {
            // Rare sword
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/ToolsAndWeapons/Weapons/NoShadow/ShortSword10.png";
        } else if (upper.contains("SWORD")) {
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/ToolsAndWeapons/Weapons/NoShadow/ShortSword1.png";
        }
        // Ranged weapons
        else if (upper.contains("OLD RELIABLE") || upper.contains("SHOTGUN")) {
            // Legendary shotgun - fancy ranged
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/ToolsAndWeapons/Weapons/NoShadow/Bows20.png";
        } else if (upper.contains("CROSSBOW")) {
            // Epic crossbow
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/ToolsAndWeapons/Weapons/NoShadow/Bows12.png";
        } else if (upper.contains("BOW")) {
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/ToolsAndWeapons/Weapons/NoShadow/Bows1.png";
        } else if (upper.contains("GUN")) {
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/ToolsAndWeapons/Weapons/NoShadow/Bows15.png";
        }
        // Armor - specific by name and rarity
        else if (upper.contains("MILITARY ARMOR")) {
            // Legendary armor - most ornate
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/Armour/Chestplate/NoShadow/Chestplate15.png";
        } else if (upper.contains("POLICE ARMOR")) {
            // Rare armor
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/Armour/Chestplate/NoShadow/Chestplate8.png";
        } else if (upper.contains("RUSTING ARMOR")) {
            // Common armor - basic worn
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/Armour/Chestplate/NoShadow/Chestplate2.png";
        } else if (upper.contains("ARMOR") || upper.contains("PLATE") || upper.contains("MAIL")) {
            // Other armor
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/Armour/Chestplate/NoShadow/Chestplate1.png";
        }
        // Consumables - Health/Potions
        else if (upper.contains("POTION") || upper.contains("ELIXIR")) {
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/PotionAndDrinks/NoShadow/Potions5.png";
        }
        // Consumables - Food
        else if (upper.contains("MEAL") || upper.contains("BREAD") || upper.contains("FOOD")) {
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/FruitsAndVegis/NoShadow/Apple1.png";
        }
        // Consumables - Alcohol
        else if (upper.contains("JACK") || upper.contains("WHISKEY") || upper.contains("BRANDY")
                || upper.contains("WINE")) {
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/PotionAndDrinks/NoShadow/WineCup1.png";
        }
        // Key Items
        else if (upper.contains("KEY")) {
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/Other/NoShadow/Keys1.png";
        }
        // Chips/Coins
        else if (upper.contains("CHIP")) {
            iconPath = "/icons/600+ RPGAdventure Items Asset Pack/CoinsGemsAndIngots/NoShadow/Coins1.png";
        }

        if (iconPath != null) {
            try {
                Image image = new Image(getClass().getResourceAsStream(iconPath));
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(24);
                imageView.setFitHeight(24);
                imageView.setPreserveRatio(true);
                return imageView;
            } catch (Exception e) {
                logAudioDebug("Failed to load icon: " + iconPath);
            }
        }
        return null;
    }

    private void updateRoomInfo() {
        Room room = game.getRoomByNumber(selectedRoomNumber);
        if (room == null) {
            return;
        }
        lblRoomName.setText(room.getName());
        lblRoomID.setText(room.getRoomId() + " · " + room.getName());
        lblExits.setText("Exits: " + buildExitString(room));
        lblRoomDescription.setText(buildRoomDetails(room));
        updateBreachButtonForCurrentRoom();
    }

    private String buildRoomDetails(Room room) {
        StringBuilder details = new StringBuilder(room.getRoomDescription());
        if (room.hasItems()) {
            String roomItems = room.getItems().stream()
                    .map(Item::getName)
                    .collect(Collectors.joining(", "));
            details.append("\nItems: ").append(roomItems);
        }
        if (room.hasPuzzle() && (room.getPuzzle() == null || !room.getPuzzle().isSolved())) {
            details.append("\nPuzzle available.");
        }
        if (room.hasMonster() && room.getMonster().isAlive()) {
            details.append("\nEnemy present: ").append(room.getMonster().getName());
        }
        return details.toString();
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
        if (outputArea == null || text == null || text.isBlank()) {
            return;
        }
        outputLineCounter++;
        outputArea.getItems().add(new OutputLine(outputLineCounter, formatPuzzleText(text)));
        forceOutputScrollToBottom();
    }

    private void forceOutputScrollToBottom() {
        if (outputArea == null || outputArea.getItems().isEmpty()) {
            return;
        }
        int lastIndex = outputArea.getItems().size() - 1;
        outputArea.scrollTo(lastIndex);
        // A second pass after layout keeps wrapped multi-line entries fully visible.
        Platform.runLater(() -> {
            if (outputArea != null && !outputArea.getItems().isEmpty()) {
                int latestLastIndex = outputArea.getItems().size() - 1;
                outputArea.scrollTo(latestLastIndex);
            }
        });
    }

    private String formatPuzzleText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("|", "\n").replace("\r\n", "\n").replace("\r", "\n");
        normalized = normalized.replaceAll("\n{3,}", "\n\n");
        return normalized.trim();
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

        if (currentRoom.isBarricaded(direction)) {
            outputText("That path is barricaded. Breach it first.");
            updateBreachButtonForCurrentRoom();
            return;
        }

        int destination = currentRoom.getExit(directionIndex);
        if (destination > 0) {
            player.setLocation(destination);
            Room nextRoom = game.getRoomByNumber(destination);
            if (nextRoom != null) {
                player.setCurrentRoom(nextRoom);
                boolean firstVisit = !nextRoom.isVisited();
                nextRoom.setVisited();
                selectedRoomNumber = destination;
                Map<String, String> params = new HashMap<>();
                params.put("roomName", nextRoom.getName());
                outputText(firstVisit
                        ? FlavorText.get("MOVE_NEW_ROOM", "You step into {roomName}.", params)
                        : FlavorText.get("MOVE_VISITED_ROOM", "You are back in {roomName}.", params));
                startCombatIfPresent(nextRoom);
            }
            updatePlayerInfo();
            updateRoomInfo();
            updateMapGrid();
            checkEscapeEnding();
        } else {
            outputText(FlavorText.get("NO_EXIT", "There is no exit in that direction."));
        }
    }

    private String findBarricadedDirection(Room room) {
        if (room == null) {
            return null;
        }
        for (String direction : new String[] { "N", "E", "S", "W" }) {
            if (room.isBarricaded(direction)) {
                return direction;
            }
        }
        return null;
    }

    private void updateBreachButtonForCurrentRoom() {
        if (btnKick == null || player == null || game == null) {
            return;
        }

        Room currentRoom = game.getRoomByNumber(player.getLocation());
        String blockedDirection = findBarricadedDirection(currentRoom);
        boolean show = blockedDirection != null && (combatSystem == null || !combatSystem.isInCombat());

        btnKick.setVisible(show);
        btnKick.setManaged(show);
        if (show) {
            btnKick.setText("Breach [K] (" + blockedDirection + ")");
        }
    }

    private void breachBarricadedExit() {
        if (player == null || game == null) {
            return;
        }
        if (combatSystem != null && combatSystem.isInCombat()) {
            outputText("You cannot breach while in combat.");
            return;
        }

        Room currentRoom = game.getRoomByNumber(player.getLocation());
        String blockedDirection = findBarricadedDirection(currentRoom);
        if (currentRoom == null || blockedDirection == null) {
            outputText("There is no barricaded path here.");
            updateBreachButtonForCurrentRoom();
            return;
        }

        boolean success = player.breach(blockedDirection);
        if (success) {
            outputText("You breach the barricade toward " + blockedDirection + " and take damage.");
            outputText("Current HP: " + player.getCurrentHP());
        } else {
            outputText("You fail to breach the barricade.");
        }

        updatePlayerInfo();
        updateRoomInfo();
        updateMapGrid();
    }

    private void checkEscapeEnding() {
        if (combatSystem == null || player == null) {
            return;
        }
        combatSystem.checkEscapeWinCondition(player);
    }

    private void outputInventorySummary() {
        if (player == null) {
            return;
        }
        List<String> lines = player.showInventory();
        outputText("Inventory:");
        for (String line : lines) {
            outputText("- " + line);
        }
        if (player.getEquippedWeapon() != null) {
            outputText("Equipped weapon: " + player.getEquippedWeapon().getName());
        }
        if (player.getEquippedArmor() != null) {
            outputText("Equipped armor: " + player.getEquippedArmor().getName());
        }
    }

    private void pickUpSelectedRoomItem() {
        if (combatSystem != null && combatSystem.isInCombat()) {
            outputText("You cannot pick up items during combat.");
            return;
        }
        Room room = game.getRoomByNumber(player.getLocation());
        if (room == null || !room.hasItems()) {
            outputText("There are no items in this room.");
            return;
        }

        Item selected = chooseItem("Pick Up Item", "Select an item on the floor.", room.getItems());
        if (selected == null) {
            return;
        }

        if (player.pickupItem(selected)) {
            Map<String, String> params = new HashMap<>();
            params.put("itemName", selected.getName());
            outputText(FlavorText.get("PICKUP_OK", "Picked up {itemName}.", params));
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("itemName", selected.getName());
            outputText(FlavorText.get("PICKUP_FULL", "Could not pick up {itemName} (bag may be full).", params));
        }
        updatePlayerInfo();
        updateRoomInfo();
        updateMapGrid();
    }

    private void useConsumableFromInventory() {
        if (player == null) {
            return;
        }
        List<Item> usable = player.getInventory().stream()
                .filter(item -> item instanceof Consumable)
                .collect(Collectors.toList());

        if (usable.isEmpty()) {
            outputText("You have no consumables to use.");
            return;
        }

        Item selected = chooseItem("Use Item", "Select a consumable to use.", usable);
        if (selected == null) {
            return;
        }

        int hpBefore = player.getCurrentHP();
        if (player.useItem(selected)) {
            int hpAfter = player.getCurrentHP();
            outputText("Used " + selected.getName() + ". HP: " + hpBefore + " -> " + hpAfter);
        } else {
            outputText("You cannot use that item right now.");
        }

        updatePlayerInfo();
        updateRoomInfo();
    }

    private void equipItemFromInventory() {
        if (player == null) {
            return;
        }

        List<Item> equippable = player.getInventory().stream()
                .filter(item -> item instanceof Weapon || item instanceof Armor)
                .collect(Collectors.toList());

        if (equippable.isEmpty()) {
            outputText("You have no equippable items in your bag.");
            return;
        }

        Item selected = chooseItem("Equip Item", "Select a weapon or armor piece to equip.", equippable);
        if (selected == null) {
            return;
        }

        if (selected instanceof Weapon weapon) {
            Weapon old = player.getEquippedWeapon();
            if (old != null && !player.addToInventory(old)) {
                outputText("Cannot swap weapons because your bag is full.");
                return;
            }
            player.equipWeapon(weapon);
            player.getInventory().remove(weapon);
            outputText("Equipped weapon: " + weapon.getName());
        } else if (selected instanceof Armor armor) {
            Armor old = player.getEquippedArmor();
            if (old != null && !player.addToInventory(old)) {
                outputText("Cannot swap armor because your bag is full.");
                return;
            }
            player.equipArmor(armor);
            player.getInventory().remove(armor);
            outputText("Equipped armor: " + armor.getName());
        }

        updatePlayerInfo();
        updateRoomInfo();
    }

    private Item chooseItem(String title, String header, List<Item> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        List<String> options = new java.util.ArrayList<>();
        Map<String, Item> optionToItem = new HashMap<>();
        int index = 1;
        for (Item item : items) {
            String option = index + ". " + item.getName() + " [" + item.getItemType() + "]";
            options.add(option);
            optionToItem.put(option, item);
            index++;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Item:");
        styleDialog(dialog);

        Optional<String> selected = dialog.showAndWait();
        restoreKeyboardFocus();
        if (selected.isEmpty()) {
            return null;
        }
        return optionToItem.get(selected.get());
    }

    private void handleInventoryListAction() {
        if (player == null || lstInventory == null) {
            return;
        }
        String selectedLabel = lstInventory.getSelectionModel().getSelectedItem();
        if (selectedLabel == null || selectedLabel.isBlank() || selectedLabel.equals("Your inventory is empty.")) {
            return;
        }

        Item selectedItem = player.getItemByName(labelToItemName(selectedLabel));
        if (selectedItem == null) {
            outputText("Select a valid item from your inventory list.");
            return;
        }

        List<String> actions = new java.util.ArrayList<>();
        actions.add("Inspect");
        actions.add("Drop");
        actions.add("Unequip");
        if (selectedItem instanceof Consumable) {
            actions.add("Use");
        } else if (selectedItem instanceof Weapon || selectedItem instanceof Armor) {
            actions.add("Equip");
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(actions.get(0), actions);
        dialog.setTitle("Inventory Item");
        dialog.setHeaderText(selectedItem.getName() + " (" + selectedItem.getItemType() + ")");
        dialog.setContentText("Action:");
        styleDialog(dialog);

        Optional<String> selectedAction = dialog.showAndWait();
        restoreKeyboardFocus();
        if (selectedAction.isEmpty()) {
            return;
        }

        String action = selectedAction.get();
        switch (action) {
            case "Inspect" -> outputText(selectedItem.getInfo());
            case "Drop" -> {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("Drop Item");
                a.setHeaderText("Drop " + selectedItem.getName() + "?");
                styleDialog(a);
                Optional<ButtonType> res = a.showAndWait();
                restoreKeyboardFocus();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    if (player.dropItem(selectedItem)) {
                        outputText("Dropped " + selectedItem.getName() + " into the room.");
                    } else {
                        outputText("Could not drop that item.");
                    }
                }
            }
            case "Unequip" -> {
                List<String> opts = new ArrayList<>();
                if (player.getEquippedWeapon() != null)
                    opts.add("Weapon: " + player.getEquippedWeapon().getName());
                if (player.getEquippedArmor() != null)
                    opts.add("Armor: " + player.getEquippedArmor().getName());
                if (opts.isEmpty()) {
                    outputText("You have nothing equipped to unequip.");
                    break;
                }
                ChoiceDialog<String> dlg = new ChoiceDialog<>(opts.get(0), opts);
                dlg.setTitle("Unequip");
                dlg.setHeaderText("Choose equipment to unequip");
                styleDialog(dlg);
                Optional<String> sel = dlg.showAndWait();
                restoreKeyboardFocus();
                if (sel.isEmpty())
                    break;
                String choice = sel.get();
                if (choice.startsWith("Weapon") && player.getEquippedWeapon() != null) {
                    if (player.unequipWeapon()) {
                        outputText("Unequipped weapon.");
                    } else {
                        outputText("Cannot unequip weapon (bag may be full).");
                    }
                } else if (choice.startsWith("Armor") && player.getEquippedArmor() != null) {
                    if (player.unequipArmor()) {
                        outputText("Unequipped armor.");
                    } else {
                        outputText("Cannot unequip armor (bag may be full).");
                    }
                }
            }
            case "Use" -> {
                if (selectedItem instanceof Consumable) {
                    int hpBefore = player.getCurrentHP();
                    if (player.useItem(selectedItem)) {
                        int hpAfter = player.getCurrentHP();
                        outputText("Used " + selectedItem.getName() + ". HP: " + hpBefore + " -> " + hpAfter);
                    } else {
                        outputText("You cannot use that item right now.");
                    }
                }
            }
            case "Equip" -> {
                if (selectedItem instanceof Weapon weapon) {
                    Weapon old = player.getEquippedWeapon();
                    if (old != null && !player.addToInventory(old)) {
                        outputText("Cannot swap weapons because your bag is full.");
                        break;
                    }
                    player.equipWeapon(weapon);
                    player.getInventory().remove(weapon);
                    outputText("Equipped weapon: " + weapon.getName());
                } else if (selectedItem instanceof Armor armor) {
                    Armor old = player.getEquippedArmor();
                    if (old != null && !player.addToInventory(old)) {
                        outputText("Cannot swap armor because your bag is full.");
                        break;
                    }
                    player.equipArmor(armor);
                    player.getInventory().remove(armor);
                    outputText("Equipped armor: " + armor.getName());
                }
            }
        }

        updatePlayerInfo();
        updateRoomInfo();
        updateMapGrid();
    }

    private void inventoryQuickAction(String action) {
        if (player == null || lstInventory == null) {
            return;
        }

        String selectedLabel = lstInventory.getSelectionModel().getSelectedItem();
        if (selectedLabel == null || selectedLabel.isBlank() || selectedLabel.equals("Your inventory is empty.")) {
            return;
        }

        Item selectedItem = player.getItemByName(labelToItemName(selectedLabel));
        if (selectedItem == null) {
            outputText("Select a valid item from your inventory list.");
            return;
        }

        if ("Inspect".equals(action)) {
            outputText(selectedItem.getInfo());
            return;
        }

        if ("Use".equals(action) && selectedItem instanceof Consumable) {
            int hpBefore = player.getCurrentHP();
            if (player.useItem(selectedItem)) {
                int hpAfter = player.getCurrentHP();
                outputText("Used " + selectedItem.getName() + ". HP: " + hpBefore + " -> " + hpAfter);
            } else {
                outputText("You cannot use that item right now.");
            }
            updatePlayerInfo();
            updateRoomInfo();
            return;
        }

        if ("Equip".equals(action)) {
            if (selectedItem instanceof Weapon weapon) {
                Weapon old = player.getEquippedWeapon();
                if (old != null && !player.addToInventory(old)) {
                    outputText("Cannot swap weapons because your bag is full.");
                    return;
                }
                player.equipWeapon(weapon);
                player.getInventory().remove(weapon);
                outputText("Equipped weapon: " + weapon.getName());
            } else if (selectedItem instanceof Armor armor) {
                Armor old = player.getEquippedArmor();
                if (old != null && !player.addToInventory(old)) {
                    outputText("Cannot swap armor because your bag is full.");
                    return;
                }
                player.equipArmor(armor);
                player.getInventory().remove(armor);
                outputText("Equipped armor: " + armor.getName());
            }
            updatePlayerInfo();
            updateRoomInfo();
        }
    }

    private String labelToItemName(String label) {
        if (label == null)
            return null;
        return label.replaceAll("\\s+x\\d+$", "").trim();
    }

    private void inventoryInspectSelected() {
        if (lstInventory == null || player == null)
            return;
        String selectedLabel = lstInventory.getSelectionModel().getSelectedItem();
        if (selectedLabel == null || selectedLabel.isBlank() || selectedLabel.equals("Your inventory is empty."))
            return;
        Item item = player.getItemByName(labelToItemName(selectedLabel));
        if (item == null) {
            outputText("Select a valid item to inspect.");
            return;
        }
        outputText(item.getInfo());
    }

    private void inventoryDropSelected() {
        if (player == null || lstInventory == null)
            return;
        String selectedLabel = lstInventory.getSelectionModel().getSelectedItem();
        if (selectedLabel == null || selectedLabel.isBlank() || selectedLabel.equals("Your inventory is empty."))
            return;
        Item item = player.getItemByName(labelToItemName(selectedLabel));
        if (item == null) {
            outputText("Select a valid item to drop.");
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Drop Item");
        a.setHeaderText("Drop " + item.getName() + "?");
        styleDialog(a);
        Optional<ButtonType> res = a.showAndWait();
        restoreKeyboardFocus();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            if (player.dropItem(item)) {
                outputText("Dropped " + item.getName() + " into the room.");
                updatePlayerInfo();
                updateRoomInfo();
                updateMapGrid();
            } else {
                outputText("Could not drop that item.");
            }
        }
    }

    private void inventoryUnequipSelected() {
        if (player == null)
            return;
        List<String> opts = new ArrayList<>();
        if (player.getEquippedWeapon() != null)
            opts.add("Weapon: " + player.getEquippedWeapon().getName());
        if (player.getEquippedArmor() != null)
            opts.add("Armor: " + player.getEquippedArmor().getName());
        if (opts.isEmpty()) {
            outputText("You have nothing equipped to unequip.");
            return;
        }
        ChoiceDialog<String> dlg = new ChoiceDialog<>(opts.get(0), opts);
        dlg.setTitle("Unequip");
        dlg.setHeaderText("Choose equipment to unequip");
        styleDialog(dlg);
        Optional<String> sel = dlg.showAndWait();
        restoreKeyboardFocus();
        if (sel.isEmpty())
            return;
        String choice = sel.get();
        if (choice.startsWith("Weapon") && player.getEquippedWeapon() != null) {
            if (player.unequipWeapon()) {
                outputText("Unequipped weapon.");
                updatePlayerInfo();
            } else {
                outputText("Cannot unequip weapon (bag may be full).");
            }
        } else if (choice.startsWith("Armor") && player.getEquippedArmor() != null) {
            if (player.unequipArmor()) {
                outputText("Unequipped armor.");
                updatePlayerInfo();
            } else {
                outputText("Cannot unequip armor (bag may be full).");
            }
        }
    }

    private void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(
                "-fx-background-color: #111827;"
                        + "-fx-border-color: #374151;"
                        + "-fx-border-width: 1;"
                        + "-fx-padding: 14;"
                        + "-fx-font-family: 'Segoe UI';"
                        + "-fx-text-fill: #e5e7eb;");

        Node header = pane.lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: #0f172a;");
        }

        Platform.runLater(() -> {
            for (Node labelNode : pane.lookupAll(".label")) {
                if (labelNode instanceof Label) {
                    Label label = (Label) labelNode;
                    label.setWrapText(true);
                }
                labelNode.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: 600;");
            }
            Node contentLabel = pane.lookup(".content");
            if (contentLabel != null) {
                if (contentLabel instanceof Label) {
                    Label label = (Label) contentLabel;
                    label.setWrapText(true);
                }
                contentLabel.setStyle("-fx-text-fill: #f8fafc;");
            }
        });

        for (ButtonType buttonType : pane.getButtonTypes()) {
            Node button = pane.lookupButton(buttonType);
            if (button != null) {
                button.setStyle(
                        "-fx-background-color: #2563eb;"
                                + "-fx-text-fill: white;"
                                + "-fx-font-weight: bold;"
                                + "-fx-background-radius: 8;"
                                + "-fx-border-radius: 8;");
            }
        }
    }

    private void fireIfVisible(Button button) {
        if (button != null && isNodeVisible(button)) {
            button.fire();
            restoreKeyboardFocus();
        }
    }

    private void restoreKeyboardFocus() {
        Platform.runLater(() -> {
            if (centerColumn != null) {
                centerColumn.requestFocus();
                return;
            }
            if (outputArea != null) {
                outputArea.requestFocus();
            }
        });
    }

    private void handleHotkeys(KeyEvent e) {
        if (e.getCode() == KeyCode.F5) {
            fireIfVisible(btnSaveMenu);
            return;
        }

        if (e.getCode() == KeyCode.Q) {
            if (btnQuitMainMenu != null && isNodeVisible(btnQuitMainMenu)) {
                btnQuitMainMenu.fire();
                return;
            }
            if (btnQuitGameOver != null && isNodeVisible(btnQuitGameOver)) {
                btnQuitGameOver.fire();
                return;
            }
            fireIfVisible(btnQuitMenu);
            return;
        }

        // Inventory-specific hotkeys when the inventory list is visible.
        // Require Ctrl to avoid conflicting with movement / other UI keys.
        if ((lstInventory != null && isNodeVisible(lstInventory)) && e.isControlDown()) {
            if (e.getCode() == KeyCode.D) {
                inventoryDropSelected();
                e.consume();
                return;
            } else if (e.getCode() == KeyCode.I) {
                inventoryInspectSelected();
                e.consume();
                return;
            } else if (e.getCode() == KeyCode.U) {
                inventoryUnequipSelected();
                e.consume();
                return;
            }
        }

        if (btnNewGame != null && isNodeVisible(btnNewGame)) {
            if (e.getCode() == KeyCode.N) {
                btnNewGame.fire();
            } else if (e.getCode() == KeyCode.L) {
                fireIfVisible(btnLoadGame);
            }
            return;
        }

        if (btnLoadGameOver != null && isNodeVisible(btnLoadGameOver)) {
            if (e.getCode() == KeyCode.L) {
                btnLoadGameOver.fire();
            } else if (e.getCode() == KeyCode.R) {
                btnRestartGameOver.fire();
            }
            return;
        }

        if (btnAttack != null && isNodeVisible(btnAttack)) {
            if (e.getCode() == KeyCode.A) {
                btnAttack.fire();
            } else if (e.getCode() == KeyCode.D) {
                btnDefend.fire();
            } else if (e.getCode() == KeyCode.U) {
                btnUseItem.fire();
            } else if (e.getCode() == KeyCode.G) {
                btnEquip.fire();
            } else if (e.getCode() == KeyCode.F) {
                btnFlee.fire();
            } else if (e.getCode() == KeyCode.I) {
                btnInspect.fire();
            }
            return;
        }

        if (btnHint != null && isNodeVisible(btnHint)) {
            if (e.getCode() == KeyCode.H) {
                btnHint.fire();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                btnExplorePuzzleText.fire();
            } else if (e.getCode() == KeyCode.ENTER) {
                // If the text input field has focus, let its own action handler
                // trigger the submit to avoid double-submission (scene filter
                // + textfield action both firing). Only fire here when the
                // input field is not focused.
                if (puzzleInputField != null && puzzleInputField.isFocused()) {
                    return;
                }
                btnSubmit.fire();
            } else if (activePuzzleType == PuzzleUIType.NUMBER_GUESS
                    || activePuzzleType == PuzzleUIType.RPS
                    || activePuzzleType == PuzzleUIType.SELECTION) {
                String guess = switch (e.getCode()) {
                    case DIGIT1, NUMPAD1 -> "1";
                    case DIGIT2, NUMPAD2 -> "2";
                    case DIGIT3, NUMPAD3 -> "3";
                    case DIGIT4, NUMPAD4 -> "4";
                    case DIGIT5, NUMPAD5 -> "5";
                    default -> null;
                };
                if (guess != null) {
                    submitPuzzleAnswer(guess);
                }
            }
            return;
        }

        if (btnExplorePuzzleCard != null && isNodeVisible(btnExplorePuzzleCard)) {
            if (e.getCode() == KeyCode.ESCAPE) {
                btnExplorePuzzleCard.fire();
            } else if (e.getCode() == KeyCode.SPACE) {
                if (btnDraw != null && isNodeVisible(btnDraw)) {
                    btnDraw.fire();
                } else if (btnStand != null && isNodeVisible(btnStand)) {
                    btnStand.fire();
                }
            }
            return;
        }

        if (btnNorth != null && isNodeVisible(btnNorth)) {
            if (e.getCode() == KeyCode.W || e.getCode() == KeyCode.UP) {
                btnNorth.fire();
            } else if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) {
                btnWest.fire();
            } else if (e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN) {
                btnSouth.fire();
            } else if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) {
                btnEast.fire();
            } else if (e.getCode() == KeyCode.R) {
                btnSolvePuzzle.fire();
            } else if (e.getCode() == KeyCode.E) {
                btnExploreAction.fire();
            } else if (e.getCode() == KeyCode.I) {
                btnInventory.fire();
            } else if (e.getCode() == KeyCode.P) {
                btnPickup.fire();
            } else if (e.getCode() == KeyCode.U) {
                btnUseFromBag.fire();
            } else if (e.getCode() == KeyCode.G) {
                btnEquipFromBag.fire();
            } else if (e.getCode() == KeyCode.K) {
                fireIfVisible(btnKick);
            } else if (e.getCode() == KeyCode.T) {
                // Status button removed; refresh player info display instead
                updatePlayerInfo();
            }
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
        if (activePuzzle != null && activePuzzle.isSolved()) {
            outputText("This puzzle has already been solved.");
            return;
        }
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
                if ("Front Gate Key".equalsIgnoreCase(activePuzzle.getName()) && !player.hasKeyItem("Front Gate Key")) {
                    player.addToInventory(new KeyItem("Front Gate Key", "GH-01"));
                    outputText("You obtained the Front Gate Key.");
                    updatePlayerInfo();
                }
                Room currentRoom = game != null ? game.getRoomByNumber(player.getLocation()) : null;
                // record solved puzzle id in game registry so saves persist puzzle solved state
                try {
                    if (game != null && activePuzzle != null)
                        game.addSolvedPuzzle(activePuzzle.getId());
                } catch (Exception ignored) {
                }
                if (currentRoom != null && currentRoom.hasPuzzle()) {
                    currentRoom.removePuzzle();
                }
                clearActivePuzzle();
                checkEscapeEnding();
                showPuzzleOutcomePopup(true, message + "\n\nPuzzle completed.");
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
                            : Color.web("#facc15"));
                }
                if (lblPuzzleAttemptsInline != null) {
                    lblPuzzleAttemptsInline.setText("Attempts left: " + activePuzzle.getAttempts());
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
                if (player != null) {
                    player.takeDamage(5);
                    updatePlayerInfo();
                    if (player.getCurrentHP() <= 0) {
                        activePuzzle = null;
                        showGameOver(FlavorText.get("DEATH_CAUSE_PUZZLE", "You have been defeated."));
                        return;
                    }
                }
                activePuzzle.resetAttempts();
                clearActivePuzzle();
                showPuzzleOutcomePopup(false, message + "\n\nYou lose 5 HP.");
            }
            case INVALID_INPUT -> {
                String message = "Invalid puzzle input. Try a different action or value.";
                System.out.println(message);
                displayPuzzleResult(message);
            }
        }
    }

    private void displayPuzzleResult(String message) {
        outputText(message);
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
        if (lblPuzzleAttemptsInline != null) {
            lblPuzzleAttemptsInline.setText("Attempts left: —");
        }
        puzzleResultLabel.setText("");
        puzzleResultLabel.setVisible(false);
        puzzleResultLabel.setManaged(false);
        puzzleCardResultLabel.setText("");
        puzzleCardResultLabel.setVisible(false);
        puzzleCardResultLabel.setManaged(false);
        setGameState(GameState.EXPLORATION);
        updateRoomInfo();
        updateMapGrid();
        restoreKeyboardFocus();
    }

    private void showPuzzleOutcomePopup(boolean passed, String message) {
        Alert popup = new Alert(passed ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, message,
                ButtonType.OK);
        popup.setTitle(passed ? "Puzzle Passed" : "Puzzle Failed");
        popup.setHeaderText(passed ? "Success" : "Failure");
        styleDialog(popup);
        popup.showAndWait();
        restoreKeyboardFocus();
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
        restoreKeyboardFocus();
    }

    /** Open a modal slot-selection dialog and load from the chosen slot. */
    private void showLoadDialog() {
        Stage dialog = buildSlotDialog("Load Game – Choose a Slot", false);
        dialog.showAndWait();
        restoreKeyboardFocus();
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
        if (ok) {
            activeSaveSlot = slot;
        }
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
        activeSaveSlot = slot;
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
        if (!game.loadItemsFromCsv("items.csv", "weapons.csv", "armor.csv", "consumables.csv")) {
            System.out.println("Failed to load items.csv during reset.");
        }
        player = Player.loadFromCsv(
                SaveManager.SAVES_DIR + SaveManager.BASE_SLOT + "/player.csv", game);
        activeSaveSlot = 0;
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
        if (player != null && player.getCurrentHP() <= 0) {
            showGameOver("You have been defeated.");
            return;
        }
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
            if (!combatSystem.isInCombat() && player != null && player.getCurrentHP() > 0) {
                setGameState(GameState.EXPLORATION);
                updatePlayerInfo();
                updateRoomInfo();
                updateMapGrid();
            }
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
                showGameOver("You have been defeated.");
            }

            @Override
            public void showWinScreen(WinType winType) {
                // Present a victory dialog offering options to continue, restart,
                // load, or quit instead of immediately closing the game.
                outputText(winType == WinType.CLEANSE ? "Cleanse ending achieved." : "Escape ending achieved.");
                showVictoryPopup(winType);
            }
        };
    }

    private void showVictoryPopup(WinType winType) {
        Alert dialog = new Alert(Alert.AlertType.NONE);
        dialog.setTitle("Victory!");
        dialog.setHeaderText(winType == WinType.CLEANSE ? "Cleanse ending achieved." : "Escape ending achieved.");
        ButtonType btnContinue = new ButtonType("Continue Exploring", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnRestart = new ButtonType("Restart Game", ButtonBar.ButtonData.APPLY);
        ButtonType btnLoad = new ButtonType("Load Game", ButtonBar.ButtonData.OTHER);
        ButtonType btnQuit = new ButtonType("Quit", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getButtonTypes().setAll(btnContinue, btnRestart, btnLoad, btnQuit);
        styleDialog(dialog);

        Optional<ButtonType> res = dialog.showAndWait();
        restoreKeyboardFocus();
        if (res.isEmpty())
            return;

        ButtonType chosen = res.get();
        if (chosen == btnContinue) {
            setGameState(GameState.EXPLORATION);
            return; // leave current world as-is
        }
        if (chosen == btnRestart) {
            resetGame();
            return;
        }
        if (chosen == btnLoad) {
            showLoadDialog();
            return;
        }
        if (chosen == btnQuit) {
            Platform.exit();
        }
    }

    private void showGameOver(String message) {
        setGameState(GameState.GAME_OVER);
        if (message != null && !message.isBlank()) {
            outputText(message);
        }
        restoreKeyboardFocus();
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
