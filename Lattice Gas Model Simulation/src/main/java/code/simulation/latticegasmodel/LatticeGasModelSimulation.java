package code.simulation.latticegasmodel;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.Parent;
import java.util.Random;

public class LatticeGasModelSimulation extends Application {
    private int latticeWidth = 50;
    private int latticeHeight = 50;
    private double particleDensity = 0.3;
    private double simulationSpeed = 10.0;
    private String collisionRule = "FHP";
    private boolean showVelocityField = false;
    private String boundaryCondition = "Periodic";
    // Lattice: [x][y][direction] -> boolean
    private boolean[][][] lattice;
    private boolean[][][] nextLattice;
    private Random random = new Random();
    private Canvas canvas;
    private AnimationTimer timer;
    private boolean isRunning = false;
    private final int[][] directions = {
            /// ungefähr sechseckig (approximate hexagonal)
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, -1}, {-1, 1}
    };
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 800, 612);
        applyDarkTheme(scene);
        root.setStyle("-fx-background-color: #000000;");
        canvas = new Canvas(500, 500);
        root.setCenter(canvas);
        VBox controls = createControlPanel();
        root.setLeft(controls);
        initializeLattice();
        timer = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (isRunning && (now - lastUpdate) >= 1_000_000_000 / simulationSpeed) {
                    stepSimulation();
                    drawLattice();
                    lastUpdate = now;
                }
            }
        };
        drawLattice();
        primaryStage.setTitle("Lattice Gas Model Simulation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    private void applyDarkTheme(Scene scene) {
        scene.getRoot().setStyle(
                "-fx-base: #2b2b2b;" +
                        "-fx-background: #2b2b2b;" +
                        "-fx-control-inner-background: #3c3f41;" +
                        "-fx-text-fill: #e0e0e0;" +
                        "-fx-accent: #2d6073;" +
                        "-fx-default-button: #3c3f41;"
        );
        applyStylesToNode(scene.getRoot());
    }
    private void applyStylesToNode(Node node) {
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                applyStylesToNode(child);
            }
        }
        if (node instanceof Label) {
            node.setStyle("-fx-text-fill: #e0e0e0;");
        } else if (node instanceof Button) {
            node.setStyle(
                    "-fx-background-color: #3c3f41;" +
                            "-fx-text-fill: #e0e0e0;" +
                            "-fx-border-color: #6d6d6d;"
            );
            node.setOnMouseEntered(e -> node.setStyle(
                    "-fx-background-color: #4d4f51;" +
                            "-fx-text-fill: #e0e0e0;" +
                            "-fx-border-color: #6d6d6d;"
            ));
            node.setOnMouseExited(e -> node.setStyle(
                    "-fx-background-color: #3c3f41;" +
                            "-fx-text-fill: #e0e0e0;" +
                            "-fx-border-color: #6d6d6d;"
            ));
        } else if (node instanceof Slider) {
            node.setStyle(
                    "-fx-control-inner-background: #3c3f41;" +
                            "-fx-slider-track-color: #3c3f41;" +
                            "-fx-slider-thumb-color: #2d6073;"
            );
        } else if (node instanceof ChoiceBox) {
            node.setStyle(
                    "-fx-background-color: #3c3f41;" +
                            "-fx-mark-color: #e0e0e0;" +
                            "-fx-text-fill: #e0e0e0;"
            );
        } else if (node instanceof CheckBox) {
            node.setStyle(
                    "-fx-text-fill: #e0e0e0;" +
                            "-fx-box-border: #6d6d6d;"
            );
        }
    }
    private VBox createControlPanel() {
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10));
        controls.setPrefWidth(250);
        controls.setStyle("-fx-background-color: #EEE8AA;");
        ChoiceBox<String> boundaryChoice = new ChoiceBox<>();
        boundaryChoice.getItems().addAll("Periodic", "Reflective");
        boundaryChoice.setValue(boundaryCondition);
        boundaryChoice.setOnAction(e -> boundaryCondition = boundaryChoice.getValue());
        Slider widthSlider = new Slider(10, 100, latticeWidth);
        widthSlider.setShowTickLabels(true);
        widthSlider.setShowTickMarks(true);
        widthSlider.setMajorTickUnit(10);
        Label widthLabel = new Label("Lattice Width: " + latticeWidth);
        widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            latticeWidth = newVal.intValue();
            widthLabel.setText("Lattice Width: " + latticeWidth);
            initializeLattice();
            drawLattice();
        });
        Slider heightSlider = new Slider(10, 100, latticeHeight);
        heightSlider.setShowTickLabels(true);
        heightSlider.setShowTickMarks(true);
        heightSlider.setMajorTickUnit(10);
        Label heightLabel = new Label("Lattice Height: " + latticeHeight);
        heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            latticeHeight = newVal.intValue();
            heightLabel.setText("Lattice Height: " + latticeHeight);
            initializeLattice();
            drawLattice();
        });
        Slider densitySlider = new Slider(0.0, 1.0, particleDensity);
        densitySlider.setShowTickLabels(true);
        densitySlider.setShowTickMarks(true);
        densitySlider.setMajorTickUnit(0.1);
        Label densityLabel = new Label(String.format("Particle Density: %.2f", particleDensity));
        densitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            particleDensity = newVal.doubleValue();
            densityLabel.setText(String.format("Particle Density: %.2f", particleDensity));
            initializeLattice();
            drawLattice();
        });
        Slider speedSlider = new Slider(1, 60, simulationSpeed);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(10);
        Label speedLabel = new Label("Speed (steps/s): " + simulationSpeed);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            simulationSpeed = newVal.doubleValue();
            speedLabel.setText("Speed (steps/s): " + simulationSpeed);
        });
        ChoiceBox<String> collisionChoice = new ChoiceBox<>();
        collisionChoice.getItems().addAll("FHP", "No Collisions");
        collisionChoice.setValue(collisionRule);
        collisionChoice.setOnAction(e -> collisionRule = collisionChoice.getValue());
        CheckBox velocityCheck = new CheckBox("Show Velocity Field");
        velocityCheck.setSelected(showVelocityField);
        velocityCheck.setOnAction(e -> {
            showVelocityField = velocityCheck.isSelected();
            drawLattice();
        });
        Button startButton = new Button("Start");
        startButton.setOnAction(e -> {
            isRunning = true;
            timer.start();
        });
        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> isRunning = false);
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            isRunning = false;
            initializeLattice();
            drawLattice();
        });
        Button stepButton = new Button("Step");
        stepButton.setOnAction(e -> {
            stepSimulation();
            drawLattice();
        });
        controls.getChildren().addAll(
                new Label("LATTICE SIZE"),
                widthLabel, widthSlider,
                heightLabel, heightSlider,
                new Label("PARTICLE DENSITY"),
                densityLabel, densitySlider,
                new Label("SIMULATION SPEED"),
                speedLabel, speedSlider,
                new Label("BOUNDARY CONDITION"),
                boundaryChoice,
                new Label("COLLISION RULE"),
                collisionChoice,
                velocityCheck,
                new HBox(10, startButton, pauseButton),
                new HBox(10, resetButton, stepButton)
        );
        return controls;
    }
    private void initializeLattice() {
        lattice = new boolean[latticeWidth][latticeHeight][6];
        nextLattice = new boolean[latticeWidth][latticeHeight][6];
        for (int x = 0; x < latticeWidth; x++) {
            for (int y = 0; y < latticeHeight; y++) {
                for (int d = 0; d < 6; d++) {
                    lattice[x][y][d] = random.nextDouble() < particleDensity;
                }
            }
        }
    }
    private void stepSimulation() {
        propagate();
        collide();
        boolean[][][] temp = lattice;
        lattice = nextLattice;
        nextLattice = temp;
    }
    private void propagate() {
        for (int x = 0; x < latticeWidth; x++) {
            for (int y = 0; y < latticeHeight; y++) {
                for (int d = 0; d < 6; d++) {
                    nextLattice[x][y][d] = false;
                }
            }
        }
        for (int x = 0; x < latticeWidth; x++) {
            for (int y = 0; y < latticeHeight; y++) {
                for (int d = 0; d < 6; d++) {
                    if (lattice[x][y][d]) {
                        int newX = x + directions[d][0];
                        int newY = y + directions[d][1];
                        if (boundaryCondition.equals("Periodic")) {
                            newX = (newX + latticeWidth) % latticeWidth;
                            newY = (newY + latticeHeight) % latticeHeight;
                            nextLattice[newX][newY][d] = true;
                        } else if (boundaryCondition.equals("Reflective")) {
                            if (newX >= 0 && newX < latticeWidth && newY >= 0 && newY < latticeHeight) {
                                nextLattice[newX][newY][d] = true;
                            } else {
                                int reflectedDir = (d + 3) % 6;
                                nextLattice[x][y][reflectedDir] = true;
                            }
                        }
                    }
                }
            }
        }
    }
    private void collide() {
        if (collisionRule.equals("No Collisions")) {
            for (int x = 0; x < latticeWidth; x++) {
                for (int y = 0; y < latticeHeight; y++) {
                    for (int d = 0; d < 6; d++) {
                        lattice[x][y][d] = nextLattice[x][y][d];
                    }
                }
            }
            return;
        }
        for (int x = 0; x < latticeWidth; x++) {
            for (int y = 0; y < latticeHeight; y++) {
                boolean[] state = nextLattice[x][y];
                boolean[] newState = new boolean[6];
                int count = 0;
                for (boolean b : state) if (b) count++;
                if (collisionRule.equals("FHP")) {
                    if (count == 2) {
                        /// 2-Körper-Kollision: Gegenüberliegende Partikel (frontal) rotieren um 60°
                        /// 2-body collision: opposite particles (head-on) rotate 60°
                        for (int d = 0; d < 3; d++) {
                            int opp = (d + 3) % 6;
                            if (state[d] && state[opp]) {
                                newState[(d + 1) % 6] = true;
                                newState[(opp + 1) % 6] = true;
                                break;
                            }
                        }
                    } else if (count == 3) {
                        /// 3-Körper-Streuung: Alle 3 Partikel im Uhrzeigersinn drehen
                        /// 3-body scatter: rotate all 3 particles clockwise
                        for (int d = 0; d < 6; d++) {
                            if (state[d]) newState[(d + 1) % 6] = true;
                        }
                    } else {
                        /// Keine Kollision oder mehr als 3 Partikel: Original beibehalten
                        /// No collision or more than 3 particles: retain original
                        System.arraycopy(state, 0, newState, 0, 6);
                    }
                }
                lattice[x][y] = newState;
            }
        }
    }
    private void drawLattice() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cellWidth = canvas.getWidth() / latticeWidth;
        double cellHeight = canvas.getHeight() / latticeHeight;
        gc.setFill(Color.rgb(30, 30, 30));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        for (int x = 0; x < latticeWidth; x++) {
            for (int y = 0; y < latticeHeight; y++) {
                int particleCount = 0;
                for (int d = 0; d < 6; d++) {
                    if (lattice[x][y][d]) particleCount++;
                }
                double intensity = particleCount / 6.0;
                Color color = Color.hsb(200 - (40 * intensity), 0.8, 0.7 + (0.3 * intensity));
                gc.setFill(color);
                gc.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
            }
        }
        if (showVelocityField) {
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(3);
            for (int x = 0; x < latticeWidth; x += 5) {
                for (int y = 0; y < latticeHeight; y += 5) {
                    double vx = 0, vy = 0;
                    for (int d = 0; d < 6; d++) {
                        if (lattice[x][y][d]) {
                            vx += directions[d][0];
                            vy += directions[d][1];
                        }
                    }
                    double mag = Math.sqrt(vx * vx + vy * vy);
                    if (mag > 0) {
                        vx /= mag; vy /= mag;
                        double startX = (x + 0.5) * cellWidth;
                        double startY = (y + 0.5) * cellHeight;
                        double endX = startX + vx * cellWidth * 0.4;
                        double endY = startY + vy * cellHeight * 0.4;
                        gc.strokeLine(startX, startY, endX, endY);
                    }
                }
            }
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}