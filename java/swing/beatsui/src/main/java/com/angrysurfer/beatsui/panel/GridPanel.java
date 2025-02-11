package com.angrysurfer.beatsui.panel;

import java.awt.*;
import javax.swing.*;
import java.util.Random;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Timer;
import com.angrysurfer.beatsui.Utils;
import com.angrysurfer.beatsui.widget.GridButton;

public class GridPanel extends JPanel {

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
        COMBAT("Combat");

        private final String label;

        DisplayMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public GridPanel() {
        super(new GridLayout(GRID_ROWS, GRID_COLS, 2, 2));
        setupTimers();
        setup();
        setupAnimation();
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

    private void checkScreensaver() {
        long timeSinceLastInteraction = System.currentTimeMillis() - lastInteraction;
        if (!isScreensaverMode && timeSinceLastInteraction > SCREENSAVER_DELAY) {
            startScreensaver();
        }
    }

    private void startScreensaver() {
        isScreensaverMode = true;
        modeChangeTimer.start();
        setDisplayMode(DisplayMode.values()[random.nextInt(DisplayMode.values().length)]);
    }

    private void stopScreensaver() {
        isScreensaverMode = false;
        modeChangeTimer.stop();
        clearDisplay();
        currentMode = null; // Reset current mode
        lastInteraction = System.currentTimeMillis(); // Reset timer
    }

    private void setup() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons = new GridButton[GRID_ROWS][GRID_COLS];

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isScreensaverMode) {
                    stopScreensaver();
                }
                lastInteraction = System.currentTimeMillis();
            }
        };

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col] = new GridButton(row, col);
                buttons[row][col].addMouseListener(mouseHandler);
                add(buttons[row][col]);
            }
        }
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
        }
    }

    private void updateExplosion() {
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

    private void updateSpace() {
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
        for (int col = 0; col < GRID_COLS; col++) {
            int colorIndex = (col + rainbowOffset) % rainbowColors.length;
            for (int row = 0; row < GRID_ROWS; row++) {
                buttons[row][col].setBackground(rainbowColors[colorIndex]);
            }
        }
        rainbowOffset = (rainbowOffset + 1) % rainbowColors.length;
    }

    private void updateClock() {
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
}
