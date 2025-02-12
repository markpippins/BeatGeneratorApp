package com.angrysurfer.beatsui.panel;

import java.awt.Color;
import java.util.Objects;
import java.util.Random;

import javax.swing.Timer;

import com.angrysurfer.beatsui.Utils;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.widget.GridButton;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GridSaver {

    private static final int GRID_ROWS = 5;
    private static final int GRID_COLS = 36;

    private GridButton[][] buttons;
    private Timer animationTimer;
    private DisplayMode currentMode = null; // Changed from TEXT to null

    private Random random = new Random();

    private Color[] rainbowColors = {
            Color.RED, Color.ORANGE, Color.YELLOW,
            Color.GREEN, Color.BLUE, new Color(75, 0, 130)
    };
    private double angle = 0; // For wave animation
    private int bouncePos = 1; // Initialize to first usable row
    private boolean bounceUp = true;

    private int spiralX = GRID_COLS / 2;
    private int spiralY = GRID_ROWS / 2;
    private double spiralAngle = 0;
    private double spiralRadius = 0;

    private double pulseSize = 0;
    private int rainbowOffset = 0;
    private double heartBeat = 0;

    private int ballX = GRID_COLS / 2;
    private int ballY = 2;
    private int ballDX = 1;
    private int ballDY = 1;

    private int[] levels = new int[GRID_COLS];

    private Timer screensaverTimer;
    private Timer modeChangeTimer;
    private long lastInteraction;
    private static final int SCREENSAVER_DELAY = 30000; // 30 seconds
    private static final int MODE_CHANGE_DELAY = 10000; // 10 seconds
    private boolean isScreensaverMode = false;

    // Add new instance variables
    private double[] starX = new double[50];
    private double[] starY = new double[50];
    private double[] starZ = new double[50];
    private int mazeX = 0, mazeY = 0;
    private boolean[][] visited;
    private Ant ant;
    private double t = 0.0; // Time variable for plasma

    // Add inner class for Langton's Ant
    private class Ant {
        int x, y, direction;
        private static final int[] dx = { 0, 1, 0, -1 }; // N, E, S, W
        private static final int[] dy = { -1, 0, 1, 0 };

        Ant(int x, int y) {
            this.x = x;
            this.y = y;
            this.direction = 0;
        }

        void move(boolean turnRight) {
            direction = (direction + (turnRight ? 1 : -1) + 4) % 4;
            x = Math.floorMod(x + dx[direction], GRID_COLS);
            y = Math.floorMod(y + dy[direction], GRID_ROWS);
        }
    }

    public enum DisplayMode {
        // Removed TEXT mode, start with EXPLOSION
        EXPLOSION("Explosion"),
        SPACE("Space"),
        GAME("Game of Life"),
        RAIN("Matrix Rain"),
        WAVE("Wave"),
        BOUNCE("Bounce"),
        SNAKE("Snake"),
        SPIRAL("Spiral"),
        FIREWORKS("Fireworks"),
        PULSE("Pulse"),
        RAINBOW("Rainbow"),
        CLOCK("Clock"),
        CONFETTI("Confetti"),
        MATRIX("Matrix"),
        HEART("Heart Beat"),
        DNA("DNA Helix"),
        PING_PONG("Ping Pong"),
        EQUALIZER("Equalizer"),
        TETRIS("Tetris"),
        COMBAT("Combat"),
        STARFIELD("Starfield"),
        RIPPLE("Ripple"),
        MAZE("Maze Generator"),
        LIFE_SOUP("Life Soup"),
        PLASMA("Plasma"),
        MANDELBROT("Mandelbrot"),
        BINARY("Binary Rain"),
        KALEIDOSCOPE("Kaleidoscope"),
        CELLULAR("Cellular"),
        BROWNIAN("Brownian Motion"),
        CRYSTAL("Crystal Growth"),
        LANGTON("Langton's Ant");

        private final String label;

        DisplayMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private StatusConsumer statusConsumer;

    public GridSaver(StatusConsumer statusConsumer, GridButton[][] buttons) {
        this.statusConsumer = statusConsumer;
        this.buttons = buttons;
        setupTimers();
        setupAnimation();
        additionalSetup();
    }

    private void setupTimers() {
        lastInteraction = System.currentTimeMillis();

        screensaverTimer = new Timer(1000, e -> checkScreensaver());
        screensaverTimer.start();

        modeChangeTimer = new Timer(MODE_CHANGE_DELAY, e -> {
            if (isScreensaverMode) {
                setDisplayMode(DisplayMode.values()[random.nextInt(DisplayMode.values().length)]);
            }
        });
    }

    public void checkScreensaver() {
        long timeSinceLastInteraction = System.currentTimeMillis() - lastInteraction;
        if (!isScreensaverMode && timeSinceLastInteraction > SCREENSAVER_DELAY) {
            startScreensaver();
        }
    }

    public void startScreensaver() {
        isScreensaverMode = true;
        modeChangeTimer.start();
        setDisplayMode(DisplayMode.values()[random.nextInt(DisplayMode.values().length)]);
    }

    public void stopScreensaver() {
        isScreensaverMode = false;
        modeChangeTimer.stop();
        clearDisplay();
        currentMode = null; // Reset current mode
        lastInteraction = System.currentTimeMillis(); // Reset timer
    }
   

    private void setupAnimation() {
        animationTimer = new Timer(100, e -> updateDisplay());
        animationTimer.start();
    }

    private void setDisplayMode(DisplayMode mode) {
        currentMode = mode;
        clearDisplay();
    }

    private void clearDisplay() {
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col].reset();
            }
        }
    }

    private void additionalSetup() {
        // Initialize stars
        for (int i = 0; i < starX.length; i++) {
            starX[i] = random.nextDouble() * 2 - 1;
            starY[i] = random.nextDouble() * 2 - 1;
            starZ[i] = random.nextDouble();
        }

        // Initialize maze
        visited = new boolean[GRID_ROWS][GRID_COLS];

        // Initialize ant
        ant = new Ant(GRID_COLS / 2, GRID_ROWS / 2);
    }

    public void updateDisplay() {
        if (!isScreensaverMode)
            return;

        switch (currentMode) {
            case EXPLOSION -> updateExplosion();
            case SPACE -> updateSpace();
            case GAME -> updateGameOfLife();
            case RAIN -> updateRain();
            case WAVE -> updateWave();
            case BOUNCE -> updateBounce();
            case SNAKE -> updateSnake();
            case SPIRAL -> updateSpiral();
            case FIREWORKS -> updateFireworks();
            case PULSE -> updatePulse();
            case RAINBOW -> updateRainbow();
            case CLOCK -> updateClock();
            case CONFETTI -> updateConfetti();
            case MATRIX -> updateMatrix();
            case HEART -> updateHeart();
            case DNA -> updateDNA();
            case PING_PONG -> updatePingPong();
            case EQUALIZER -> updateEqualizer();
            case TETRIS -> updateTetrisRain();
            case COMBAT -> updateCombat();
            case STARFIELD -> updateStarfield();
            case RIPPLE -> updateRipple();
            case MAZE -> updateMaze();
            case LIFE_SOUP -> updateLifeSoup();
            case PLASMA -> updatePlasma();
            case MANDELBROT -> updateMandelbrot();
            case BINARY -> updateBinaryRain();
            case KALEIDOSCOPE -> updateKaleidoscope();
            case CELLULAR -> updateCellular();
            case BROWNIAN -> updateBrownian();
            case CRYSTAL -> updateCrystal();
            case LANGTON -> updateLangton();
        }
    }

    private void updateExplosion() {
        setStatus("Explosion");
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (random.nextInt(100) < 10) {
                    buttons[row][col].setBackground(new Color(
                            random.nextInt(156) + 100, // Warmer colors
                            random.nextInt(50), // Less green
                            0 // No blue
                    ));
                } else {
                    buttons[row][col].setBackground(Utils.darkGray);
                }
            }
        }
    }

    private void setStatus(String string) {
        if (Objects.nonNull(statusConsumer))
            statusConsumer.setStatus(string);
    }

    private void updateSpace() {
        setStatus("Space");
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (random.nextInt(100) < 2) {
                    buttons[row][col].setBackground(Color.WHITE);
                } else {
                    buttons[row][col].setBackground(Utils.darkGray);
                }
            }
        }
    }

    private void updateGameOfLife() {
        setStatus("Game of Life");
        boolean[][] nextGen = new boolean[GRID_ROWS][GRID_COLS];

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int neighbors = countLiveNeighbors(row, col);
                boolean isAlive = !buttons[row][col].getBackground().equals(Utils.darkGray);

                if (isAlive && (neighbors == 2 || neighbors == 3)) {
                    nextGen[row][col] = true;
                } else if (!isAlive && neighbors == 3) {
                    nextGen[row][col] = true;
                }
            }
        }

        // Update grid
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col].setBackground(nextGen[row][col] ? Utils.fadedLime : Utils.darkGray);
            }
        }

        // Randomly seed new cells
        if (random.nextInt(100) < 5) {
            int row = random.nextInt(GRID_ROWS);
            int col = random.nextInt(GRID_COLS);
            buttons[row][col].setBackground(Utils.fadedLime);
        }
    }

    private int countLiveNeighbors(int row, int col) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0)
                    continue;
                int r = (row + i + GRID_ROWS) % GRID_ROWS;
                int c = (col + j + GRID_COLS) % GRID_COLS;
                if (!buttons[r][c].getBackground().equals(Utils.darkGray)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void updateRain() {
        setStatus("Matrix Rain");
        // Shift everything down
        for (int row = GRID_ROWS - 1; row > 0; row--) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col].setBackground(buttons[row - 1][col].getBackground());
            }
        }
        // New drops at top
        for (int col = 0; col < GRID_COLS; col++) {
            if (random.nextInt(100) < 15) {
                buttons[0][col].setBackground(Color.GREEN);
            } else {
                buttons[0][col].setBackground(Utils.darkGray);
            }
        }
    }

    private void updateWave() {
        setStatus("Wave");
        for (int col = 0; col < GRID_COLS; col++) {
            int row = (int) (2 + Math.sin(angle + col * 0.3) * 1.5);
            clearDisplay();
            if (row >= 0 && row < GRID_ROWS) {
                buttons[row][col].setBackground(Color.CYAN);
            }
        }
        angle += 0.1;
    }

    private void updateBounce() {
        setStatus("Bounce");
        clearDisplay();
        for (int col = 0; col < GRID_COLS; col++) {
            buttons[bouncePos][col].setBackground(Color.YELLOW);
        }
        if (bounceUp) {
            bouncePos--;
            if (bouncePos < 0) { // Fix bounce bounds
                bouncePos = 0;
                bounceUp = false;
            }
        } else {
            bouncePos++;
            if (bouncePos >= GRID_ROWS) { // Fix bounce bounds
                bouncePos = GRID_ROWS - 1;
                bounceUp = true;
            }
        }
    }

    private void updateSnake() {
        setStatus("Snake");
        // Simple snake pattern
        clearDisplay();
        int row = 2 + (int) (Math.sin(angle) * 1.5);
        int col = (int) (angle * 2) % GRID_COLS;
        if (row >= 0 && row < GRID_ROWS && col >= 0 && col < GRID_COLS) {
            buttons[row][col].setBackground(Color.GREEN);
        }
        angle += 0.2;
    }

    private void updateSpiral() {
        setStatus("Spiral");
        clearDisplay();
        spiralAngle += 0.2;
        spiralRadius = (spiralAngle % 10) / 2;
        int x = (int) (spiralX + Math.cos(spiralAngle) * spiralRadius);
        int y = (int) (spiralY + Math.sin(spiralAngle) * spiralRadius);
        if (y >= 0 && y < GRID_ROWS && x >= 0 && x < GRID_COLS) {
            buttons[y][x].setBackground(Color.MAGENTA);
        }
    }

    private void updateFireworks() {
        setStatus("Fireworks");
        // Fade existing colors
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                Color c = buttons[row][col].getBackground();
                if (c != Utils.darkGray) {
                    buttons[row][col].setBackground(new Color(
                            Math.max(c.getRed() - 20, 0),
                            Math.max(c.getGreen() - 20, 0),
                            Math.max(c.getBlue() - 20, 0)));
                }
            }
        }
        // Random new fireworks
        if (random.nextInt(100) < 10) {
            int centerX = random.nextInt(GRID_COLS);
            Color color = new Color(random.nextInt(128) + 127,
                    random.nextInt(128) + 127,
                    random.nextInt(128) + 127);
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int x = centerX + i;
                    int y = 2 + j;
                    if (y >= 0 && y < GRID_ROWS && x >= 0 && x < GRID_COLS) {
                        buttons[y][x].setBackground(color);
                    }
                }
            }
        }
    }

    private void updatePulse() {
        setStatus("Pulse");
        clearDisplay();
        int centerX = GRID_COLS / 2;
        int centerY = 2;
        pulseSize += 0.2;
        if (pulseSize > 5)
            pulseSize = 0;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                double distance = Math.sqrt(Math.pow(col - centerX, 2) + Math.pow(row - centerY, 2));
                if (Math.abs(distance - pulseSize) < 0.5) {
                    buttons[row][col].setBackground(Color.ORANGE);
                }
            }
        }
    }

    private void updateRainbow() {
        setStatus("Rainbow");
        for (int col = 0; col < GRID_COLS; col++) {
            int colorIndex = (col + rainbowOffset) % rainbowColors.length;
            for (int row = 0; row < GRID_ROWS; row++) {
                buttons[row][col].setBackground(rainbowColors[colorIndex]);
            }
        }
        rainbowOffset = (rainbowOffset + 1) % rainbowColors.length;
    }

    private void updateClock() {
        setStatus("Clock");
        clearDisplay();
        double time = System.currentTimeMillis() / 1000.0;
        // Hour hand
        drawClockHand(2, time / 3600 % 12 * Math.PI / 6, Color.RED);
        // Minute hand
        drawClockHand(3, time / 60 % 60 * Math.PI / 30, Color.GREEN);
        // Second hand
        drawClockHand(3, time % 60 * Math.PI / 30, Color.BLUE);
    }

    private void drawClockHand(int length, double angle, Color color) {
        int centerX = GRID_COLS / 2;
        int centerY = 2;
        int endX = centerX + (int) (Math.sin(angle) * length);
        int endY = centerY + (int) (Math.cos(angle) * length);
        if (endY >= 0 && endY < GRID_ROWS && endX >= 0 && endX < GRID_COLS) {
            buttons[endY][endX].setBackground(color);
        }
    }

    private void updateConfetti() {
        setStatus("Confetti");
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (random.nextInt(100) < 5) {
                    buttons[row][col].setBackground(rainbowColors[random.nextInt(rainbowColors.length)]);
                } else if (random.nextInt(100) < 10) {
                    buttons[row][col].setBackground(Utils.darkGray);
                }
            }
        }
    }

    private void updateMatrix() {
        setStatus("Matrix");
        // Shift everything down
        for (int row = GRID_ROWS - 1; row > 0; row--) {
            for (int col = 0; col < GRID_COLS; col++) {
                Color above = buttons[row - 1][col].getBackground();
                if (above.equals(Utils.fadedLime)) {
                    buttons[row][col].setBackground(Utils.mutedOlive);
                } else if (!above.equals(Utils.darkGray)) {
                    buttons[row][col].setBackground(new Color(0,
                            Math.max(above.getGreen() - 20, 0), 0));
                } else {
                    buttons[row][col].setBackground(Utils.darkGray);
                }
            }
        }
        // New matrix symbols at top
        for (int col = 0; col < GRID_COLS; col++) {
            if (random.nextInt(100) < 15) {
                buttons[0][col].setBackground(Utils.fadedLime);
            }
        }
    }

    private void updateHeart() {
        setStatus("Heart Beat");
        clearDisplay();
        int centerX = GRID_COLS / 2;
        int centerY = 2;
        heartBeat += 0.1;
        double size = 1 + Math.sin(heartBeat) * 0.5;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                double dx = (col - centerX) / size;
                double dy = (row - centerY) / size;
                if (isHeart(dx, dy)) {
                    buttons[row][col].setBackground(Color.RED);
                }
            }
        }
    }

    private boolean isHeart(double x, double y) {
        x = Math.abs(x);
        return Math.pow((x * x + y * y - 1), 3) <= x * x * y * y * y;
    }

    private void updateDNA() {
        setStatus("DNA Helix");
        clearDisplay();
        for (int col = 0; col < GRID_COLS; col++) {
            double offset = angle + col * 0.3;
            int row1 = (int) (2 + Math.sin(offset) * 1.2);
            int row2 = (int) (2 + Math.sin(offset + Math.PI) * 1.2);

            if (row1 >= 0 && row1 < GRID_ROWS) {
                buttons[row1][col].setBackground(Color.BLUE);
            }
            if (row2 >= 0 && row2 < GRID_ROWS) {
                buttons[row2][col].setBackground(Color.RED);
            }
            // Draw connecting lines
            if (col % 4 == 0) {
                int middle = (row1 + row2) / 2;
                if (middle >= 0 && middle < GRID_ROWS) {
                    buttons[middle][col].setBackground(Color.GREEN);
                }
            }
        }
        angle += 0.1;
    }

    private void updatePingPong() {
        setStatus("Ping Pong");
        clearDisplay();
        // Update ball position
        ballX += ballDX;
        ballY += ballDY;

        // Bounce off walls
        if (ballX <= 0 || ballX >= GRID_COLS - 1)
            ballDX *= -1;
        if (ballY <= 0 || ballY >= GRID_ROWS - 1)
            ballDY *= -1;

        // Draw ball
        if (ballY >= 0 && ballY < GRID_ROWS && ballX >= 0 && ballX < GRID_COLS) {
            buttons[ballY][ballX].setBackground(Color.WHITE);
        }
    }

    private void updateEqualizer() {
        setStatus("Equalizer");
        clearDisplay();
        // Update levels
        for (int col = 0; col < GRID_COLS; col++) {
            // Randomly adjust levels
            if (random.nextInt(100) < 30) {
                levels[col] += random.nextInt(3) - 1;
                levels[col] = Math.max(0, Math.min(GRID_ROWS - 1, levels[col]));
            }

            // Draw columns
            for (int row = GRID_ROWS - 1; row >= GRID_ROWS - levels[col] && row >= 0; row--) {
                Color color = new Color(
                        50 + (205 * (GRID_ROWS - row) / (GRID_ROWS - 1)),
                        255 - (205 * (GRID_ROWS - row) / (GRID_ROWS - 1)),
                        0);
                buttons[row][col].setBackground(color);
            }
        }
    }

    private void updateTetrisRain() {
        setStatus("Tetris Rain");
        // Shift everything down
        for (int row = GRID_ROWS - 1; row > 0; row--) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col].setBackground(buttons[row - 1][col].getBackground());
            }
        }

        // Add new pieces at top
        if (random.nextInt(100) < 30) {
            Color[] colors = { Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.RED, Color.GREEN, Color.BLUE };
            int pieceWidth = random.nextInt(3) + 1;
            int startCol = random.nextInt(GRID_COLS - pieceWidth);
            Color color = colors[random.nextInt(colors.length)];

            for (int i = 0; i < pieceWidth; i++) {
                buttons[0][startCol + i].setBackground(color);
            }
        }
    }

    private void updateCombat() {
        setStatus("Combat");
        clearDisplay();

        // Update tank positions and shots
        // ... implement tank movement and collision logic ...

        // For now, just show some moving "tanks" and "shots"
        for (int i = 0; i < 2; i++) {
            int row = random.nextInt(GRID_ROWS);
            int col = random.nextInt(GRID_COLS);
            buttons[row][col].setBackground(Color.YELLOW); // Tank

            // Add some "shots"
            int shotRow = row + random.nextInt(3) - 1;
            int shotCol = col + random.nextInt(3) - 1;
            if (shotRow >= 0 && shotRow < GRID_ROWS && shotCol >= 0 && shotCol < GRID_COLS) {
                buttons[shotRow][shotCol].setBackground(Color.RED);
            }
        }
    }

    private void updateStarfield() {
        setStatus("Starfield");
        clearDisplay();
        for (int i = 0; i < starX.length; i++) {
            starZ[i] -= 0.02;
            if (starZ[i] <= 0) {
                starX[i] = random.nextDouble() * 2 - 1;
                starY[i] = random.nextDouble() * 2 - 1;
                starZ[i] = 1;
            }
            int x = (int) (GRID_COLS / 2 + (starX[i] / starZ[i]) * GRID_COLS / 2);
            int y = (int) (GRID_ROWS / 2 + (starY[i] / starZ[i]) * GRID_ROWS / 2);
            if (x >= 0 && x < GRID_COLS && y >= 0 && y < GRID_ROWS) {
                int brightness = (int) (255 * (1 - starZ[i]));
                buttons[y][x].setBackground(new Color(brightness, brightness, brightness));
            }
        }
    }

    private void updateRipple() {
        setStatus("Ripple");
        double cx = GRID_COLS / 2.0;
        double cy = GRID_ROWS / 2.0;
        t += 0.1;

        for (int y = 0; y < GRID_ROWS; y++) {
            for (int x = 0; x < GRID_COLS; x++) {
                double dx = x - cx;
                double dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double val = Math.sin(dist - t);
                int rgb = (int) ((val + 1) * 127);
                buttons[y][x].setBackground(new Color(0, rgb, rgb));
            }
        }
    }

    private void updateMaze() {
        setStatus("Maze Generator");
        if (!visited[mazeY][mazeX]) {
            visited[mazeY][mazeX] = true;
            buttons[mazeY][mazeX].setBackground(Color.WHITE);

            int[] directions = { 0, 1, 2, 3 };
            for (int i = 0; i < 4; i++) {
                int j = random.nextInt(4);
                int temp = directions[i];
                directions[i] = directions[j];
                directions[j] = temp;
            }

            for (int dir : directions) {
                int nextX = mazeX + (dir == 1 ? 1 : dir == 3 ? -1 : 0);
                int nextY = mazeY + (dir == 0 ? -1 : dir == 2 ? 1 : 0);
                if (nextX >= 0 && nextX < GRID_COLS && nextY >= 0 && nextY < GRID_ROWS && !visited[nextY][nextX]) {
                    mazeX = nextX;
                    mazeY = nextY;
                    return;
                }
            }
        }
        // Reset when maze is complete
        if (random.nextInt(100) < 5) {
            clearDisplay();
            visited = new boolean[GRID_ROWS][GRID_COLS];
            mazeX = random.nextInt(GRID_COLS);
            mazeY = random.nextInt(GRID_ROWS);
        }
    }

    private void updateLifeSoup() {
        setStatus("Life Soup");
        boolean[][] nextGen = new boolean[GRID_ROWS][GRID_COLS];
        Color[][] nextColors = new Color[GRID_ROWS][GRID_COLS];

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int neighbors = countLiveNeighbors(row, col);
                Color currentColor = buttons[row][col].getBackground();
                boolean isAlive = !currentColor.equals(Utils.darkGray);

                if (isAlive) {
                    if (neighbors == 2 || neighbors == 3) {
                        nextGen[row][col] = true;
                        // Evolve color
                        nextColors[row][col] = new Color(
                                (currentColor.getRed() + 10) % 256,
                                (currentColor.getGreen() + 5) % 256,
                                (currentColor.getBlue() + 15) % 256);
                    }
                } else if (neighbors == 3) {
                    nextGen[row][col] = true;
                    nextColors[row][col] = new Color(
                            random.nextInt(256),
                            random.nextInt(256),
                            random.nextInt(256));
                }
            }
        }

        // Update grid
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col].setBackground(
                        nextGen[row][col] ? nextColors[row][col] : Utils.darkGray);
            }
        }

        // Random new cells
        if (random.nextInt(100) < 5) {
            int row = random.nextInt(GRID_ROWS);
            int col = random.nextInt(GRID_COLS);
            buttons[row][col].setBackground(new Color(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)));
        }
    }

    private void updatePlasma() {
        setStatus("Plasma");
        t += 0.1;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                double value = Math.sin(col * 0.2 + t) +
                        Math.sin(row * 0.1 + t) +
                        Math.sin((col + row) * 0.15 + t) +
                        Math.sin(Math.sqrt(col * col + row * row) * 0.15);
                value = value * 0.25 + 0.5; // Normalize to 0-1
                int red = (int) (Math.sin(value * Math.PI * 2) * 127 + 128);
                int green = (int) (Math.sin(value * Math.PI * 2 + 2 * Math.PI / 3) * 127 + 128);
                int blue = (int) (Math.sin(value * Math.PI * 2 + 4 * Math.PI / 3) * 127 + 128);
                buttons[row][col].setBackground(new Color(red, green, blue));
            }
        }
    }

    private void updateMandelbrot() {
        setStatus("Mandelbrot");
        double zoom = 1.5 + Math.sin(t * 0.1) * 0.5;
        double centerX = -0.5 + Math.sin(t * 0.05) * 0.2;
        double centerY = Math.cos(t * 0.05) * 0.2;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                double x0 = (col - GRID_COLS / 2.0) * zoom / GRID_COLS + centerX;
                double y0 = (row - GRID_ROWS / 2.0) * zoom / GRID_ROWS + centerY;

                double x = 0, y = 0;
                int iteration = 0;
                while (x * x + y * y < 4 && iteration < 20) {
                    double xtemp = x * x - y * y + x0;
                    y = 2 * x * y + y0;
                    x = xtemp;
                    iteration++;
                }

                int hue = (iteration * 13) % 360;
                buttons[row][col].setBackground(Color.getHSBColor(hue / 360f, 0.8f, iteration < 20 ? 1f : 0f));
            }
        }
        t += 0.1;
    }

    private void updateBinaryRain() {
        setStatus("Binary Rain");
        // Shift existing content down
        for (int row = GRID_ROWS - 1; row > 0; row--) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col].setBackground(buttons[row - 1][col].getBackground());
                buttons[row][col].setText(buttons[row - 1][col].getText());
            }
        }

        // Add new binary digits at top
        for (int col = 0; col < GRID_COLS; col++) {
            if (random.nextInt(100) < 15) {
                buttons[0][col].setBackground(Utils.fadedLime);
                buttons[0][col].setText(random.nextInt(2) + "");
            } else {
                buttons[0][col].setBackground(Utils.darkGray);
                buttons[0][col].setText("");
            }
        }
    }

    private void updateKaleidoscope() {
        setStatus("Kaleidoscope");
        int centerX = GRID_COLS / 2;
        int centerY = GRID_ROWS / 2;
        t += 0.1;

        // Generate a pattern in one sector
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS / 2; col++) {
                if (row < GRID_ROWS / 2) {
                    double angle = t + Math.sqrt(col * col + row * row) * 0.2;
                    int red = (int) (Math.sin(angle) * 127 + 128);
                    int green = (int) (Math.sin(angle + 2 * Math.PI / 3) * 127 + 128);
                    int blue = (int) (Math.sin(angle + 4 * Math.PI / 3) * 127 + 128);
                    Color color = new Color(red, green, blue);

                    // Mirror across all quadrants
                    if (col < GRID_COLS / 2 && row < GRID_ROWS / 2) {
                        setSymmetricPixels(col, row, color);
                    }
                }
            }
        }
    }

    private void setSymmetricPixels(int x, int y, Color color) {
        int centerX = GRID_COLS / 2;
        int centerY = GRID_ROWS / 2;

        // Set pixels in all quadrants
        if (y + centerY < GRID_ROWS && x + centerX < GRID_COLS)
            buttons[y + centerY][x + centerX].setBackground(color);
        if (y + centerY < GRID_ROWS && centerX - x - 1 >= 0)
            buttons[y + centerY][centerX - x - 1].setBackground(color);
        if (centerY - y - 1 >= 0 && x + centerX < GRID_COLS)
            buttons[centerY - y - 1][x + centerX].setBackground(color);
        if (centerY - y - 1 >= 0 && centerX - x - 1 >= 0)
            buttons[centerY - y - 1][centerX - x - 1].setBackground(color);
    }

    private void updateCellular() {
        setStatus("Cellular Automaton");
        Color[][] nextGen = new Color[GRID_ROWS][GRID_COLS];

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                Color current = buttons[row][col].getBackground();
                int[] neighborCounts = countColorNeighbors(row, col);

                if (current.equals(Utils.darkGray)) {
                    if (neighborCounts[0] == 3)
                        nextGen[row][col] = Color.RED;
                    else if (neighborCounts[1] == 3)
                        nextGen[row][col] = Color.BLUE;
                    else
                        nextGen[row][col] = Utils.darkGray;
                } else {
                    int total = neighborCounts[0] + neighborCounts[1];
                    if (total < 2 || total > 3)
                        nextGen[row][col] = Utils.darkGray;
                    else
                        nextGen[row][col] = current;
                }
            }
        }

        // Update grid and randomly add new cells
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col].setBackground(nextGen[row][col]);
            }
        }

        if (random.nextInt(100) < 5) {
            int row = random.nextInt(GRID_ROWS);
            int col = random.nextInt(GRID_COLS);
            buttons[row][col].setBackground(random.nextBoolean() ? Color.RED : Color.BLUE);
        }
    }

    private int[] countColorNeighbors(int row, int col) {
        int[] counts = new int[2]; // [red, blue]
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0)
                    continue;
                int r = (row + i + GRID_ROWS) % GRID_ROWS;
                int c = (col + j + GRID_COLS) % GRID_COLS;
                Color neighbor = buttons[r][c].getBackground();
                if (neighbor.equals(Color.RED))
                    counts[0]++;
                else if (neighbor.equals(Color.BLUE))
                    counts[1]++;
            }
        }
        return counts;
    }

    private void updateBrownian() {
        setStatus("Brownian Motion");
        if (random.nextInt(100) < 30) {
            // Add new particle
            int x = random.nextInt(GRID_COLS);
            int y = random.nextInt(GRID_ROWS);
            buttons[y][x].setBackground(new Color(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)));
        }

        // Move existing particles
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (!buttons[row][col].getBackground().equals(Utils.darkGray)) {
                    int newRow = row + random.nextInt(3) - 1;
                    int newCol = col + random.nextInt(3) - 1;

                    if (newRow >= 0 && newRow < GRID_ROWS &&
                            newCol >= 0 && newCol < GRID_COLS &&
                            buttons[newRow][newCol].getBackground().equals(Utils.darkGray)) {
                        buttons[newRow][newCol].setBackground(buttons[row][col].getBackground());
                        buttons[row][col].setBackground(Utils.darkGray);
                    }
                }
            }
        }
    }

    private void updateCrystal() {
        setStatus("Crystal Growth");
        // Initialize center crystal if needed
        if (buttons[GRID_ROWS / 2][GRID_COLS / 2].getBackground().equals(Utils.darkGray)) {
            buttons[GRID_ROWS / 2][GRID_COLS / 2].setBackground(Color.WHITE);
        }

        // Random walk particles until they stick to crystal
        for (int i = 0; i < 3; i++) {
            int x = random.nextInt(GRID_COLS);
            int y = random.nextInt(GRID_ROWS);
            Color particleColor = new Color(
                    200 + random.nextInt(56),
                    200 + random.nextInt(56),
                    255);

            while (true) {
                // Check for adjacent crystal
                boolean stuck = false;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && nx < GRID_COLS && ny >= 0 && ny < GRID_ROWS) {
                            Color neighbor = buttons[ny][nx].getBackground();
                            if (!neighbor.equals(Utils.darkGray)) {
                                stuck = true;
                                break;
                            }
                        }
                    }
                    if (stuck)
                        break;
                }

                if (stuck) {
                    buttons[y][x].setBackground(particleColor);
                    break;
                }

                // Random walk
                x = Math.floorMod(x + random.nextInt(3) - 1, GRID_COLS);
                y = Math.floorMod(y + random.nextInt(3) - 1, GRID_ROWS);
            }
        }
    }

    private void updateLangton() {
        setStatus("Langton's Ant");
        // Implement Langton's Ant cellular automaton
        Color current = buttons[ant.y][ant.x].getBackground();
        boolean isWhite = current.equals(Color.WHITE);
        buttons[ant.y][ant.x].setBackground(isWhite ? Utils.darkGray : Color.WHITE);
        ant.move(!isWhite);
    }
}
