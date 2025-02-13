package com.angrysurfer.beatsui.grid;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.Timer;

import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.grid.visualizer.ArpeggiatorVisualization;
import com.angrysurfer.beatsui.grid.visualizer.AsteroidsVisualization;
import com.angrysurfer.beatsui.grid.visualizer.BinaryRainVisualization;
import com.angrysurfer.beatsui.grid.visualizer.BounceVisualization;
import com.angrysurfer.beatsui.grid.visualizer.BreakoutVisualization;
import com.angrysurfer.beatsui.grid.visualizer.BrownianVisualization;
import com.angrysurfer.beatsui.grid.visualizer.CellularVisualization;
import com.angrysurfer.beatsui.grid.visualizer.ChordProgressionVisualization;
import com.angrysurfer.beatsui.grid.visualizer.ClockVisualization;
import com.angrysurfer.beatsui.grid.visualizer.ConfettiVisualization;
import com.angrysurfer.beatsui.grid.visualizer.CrystalVisualization;
import com.angrysurfer.beatsui.grid.visualizer.DNAVisualization;
import com.angrysurfer.beatsui.grid.visualizer.DigDugVisualization;
import com.angrysurfer.beatsui.grid.visualizer.DrumPatternVisualization;
import com.angrysurfer.beatsui.grid.visualizer.EqualizerVisualization;
import com.angrysurfer.beatsui.grid.visualizer.EuclideanRhythmVisualization;
import com.angrysurfer.beatsui.grid.visualizer.ExplosionVisualization;
import com.angrysurfer.beatsui.grid.visualizer.FireworksVisualization;
import com.angrysurfer.beatsui.grid.visualizer.FrequencyBandsVisualization;
import com.angrysurfer.beatsui.grid.visualizer.FroggerVisualization;
import com.angrysurfer.beatsui.grid.visualizer.GameOfLifeVisualization;
import com.angrysurfer.beatsui.grid.visualizer.GateSequencerVisualization;
import com.angrysurfer.beatsui.grid.visualizer.HarmonicsVisualization;
import com.angrysurfer.beatsui.grid.visualizer.HeartVisualization;
import com.angrysurfer.beatsui.grid.visualizer.KaleidoscopeVisualization;
import com.angrysurfer.beatsui.grid.visualizer.LFOMatrixVisualization;
import com.angrysurfer.beatsui.grid.visualizer.LangtonVisualization;
import com.angrysurfer.beatsui.grid.visualizer.LifeSoupVisualization;
import com.angrysurfer.beatsui.grid.visualizer.LoopPulseVisualization;
import com.angrysurfer.beatsui.grid.visualizer.MandelbrotVisualization;
import com.angrysurfer.beatsui.grid.visualizer.MatrixRainVisualization;
import com.angrysurfer.beatsui.grid.visualizer.MatrixVisualization;
import com.angrysurfer.beatsui.grid.visualizer.MazeVisualization;
import com.angrysurfer.beatsui.grid.visualizer.MidiGridVisualization;
import com.angrysurfer.beatsui.grid.visualizer.MissileCommandVisualization;
import com.angrysurfer.beatsui.grid.visualizer.ModularCVVisualization;
import com.angrysurfer.beatsui.grid.visualizer.OscilloscopeVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PacmanVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PhaseShiftVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PianoRollVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PlasmaVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PlatformClimberVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PolePositionVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PolyphonicVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PolyrhythmVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PongVisualization;
import com.angrysurfer.beatsui.grid.visualizer.ProbabilityGridVisualization;
import com.angrysurfer.beatsui.grid.visualizer.PulseVisualization;
import com.angrysurfer.beatsui.grid.visualizer.RacingVisualization;
import com.angrysurfer.beatsui.grid.visualizer.RainbowVisualization;
import com.angrysurfer.beatsui.grid.visualizer.RippleVisualization;
import com.angrysurfer.beatsui.grid.visualizer.SnakeVisualization;
import com.angrysurfer.beatsui.grid.visualizer.SpaceInvadersVisualization;
import com.angrysurfer.beatsui.grid.visualizer.SpaceVisualization;
import com.angrysurfer.beatsui.grid.visualizer.SpectrumAnalyzerVisualization;
import com.angrysurfer.beatsui.grid.visualizer.SpiralVisualization;
import com.angrysurfer.beatsui.grid.visualizer.StarfieldVisualization;
import com.angrysurfer.beatsui.grid.visualizer.StepSequencerVisualization;
import com.angrysurfer.beatsui.grid.visualizer.TimeDivisionVisualization;
import com.angrysurfer.beatsui.grid.visualizer.TriggerBurstVisualization;
import com.angrysurfer.beatsui.grid.visualizer.TronVisualization;
import com.angrysurfer.beatsui.grid.visualizer.VUMeterVisualization;
import com.angrysurfer.beatsui.grid.visualizer.WaveVisualization;
import com.angrysurfer.beatsui.grid.visualizer.XYPadVisualization;
import com.angrysurfer.beatsui.widget.GridButton;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Display {

    private final JComponent parent;

    public static final int GRID_ROWS = 8;
    public static final int GRID_COLS = 36;

    private GridButton[][] buttons;
    private Timer animationTimer;
    private DisplayMode currentMode = null; // Changed from TEXT to null

    private Random random = new Random();

    private Color[] rainbowColors = {
            Color.RED, Color.ORANGE, Color.YELLOW,
            Color.GREEN, Color.BLUE, new Color(75, 0, 130)
    };
    // private double angle = 0; // For wave animation
    // private int bouncePos = 1; // Initialize to first usable row
    // private boolean bounceUp = true;

    // private int spiralX = GRID_COLS / 2;
    // private int spiralY = GRID_ROWS / 2;
    // private double spiralAngle = 0;
    // private double spiralRadius = 0;

    // private double pulseSize = 0;
    // private int rainbowOffset = 0;
    // private double heartBeat = 0;

    // private int ballX = GRID_COLS / 2;
    // private int ballY = 2;
    // private int ballDX = 1;
    // private int ballDY = 1;

    // private int[] levels = new int[GRID_COLS];

    private Timer screensaverTimer;
    private Timer modeChangeTimer;
    private long lastInteraction;
    private static final int SCREENSAVER_DELAY = 30; // 30 seconds
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

    // Add new instance variables
    private double[] waveformData = new double[GRID_COLS];
    private double[] spectrumData = new double[GRID_COLS];
    private int seqPosition = 0;
    private double phase = 0.0;

    // Add new instance variables
    private List<RubiksCuber> cubers = new ArrayList<>();
    private long competitionStartTime;
    private boolean hasWinner = false;

    // Add new instance variables
    private double[] phases = new double[GRID_ROWS];
    private int[] euclidSteps = { 3, 4, 5, 7 };
    private int[] euclidPulses = { 2, 3, 2, 5 };
    private double[][] lfoValues = new double[GRID_ROWS][GRID_COLS];
    private double[] lfoFreqs = { 1.0, 1.5, 2.0, 2.5, 3.0 };
    private int burstCount = 0;
    private int burstX = 0, burstY = 0;

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

    // Add inner class for Rubik's Cuber
    private class RubiksCuber {
        int x, y;
        double progress;
        boolean isWinner;
        Color[] colors;

        RubiksCuber(int x, int y) {
            this.x = x;
            this.y = y;
            this.progress = 0.0;
            this.isWinner = false;
            this.colors = new Color[] {
                    Color.RED, Color.ORANGE, Color.YELLOW,
                    Color.GREEN, Color.BLUE, Color.WHITE
            };
        }

        void update() {
            progress += random.nextDouble() * 0.03; // Random solving speed
            if (progress >= 1.0 && !isWinner) {
                isWinner = true;
                return;
            }
        }

        Color getCurrentColor() {
            int colorIndex = (int) (progress * colors.length);
            return colors[Math.min(colorIndex, colors.length - 1)];
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
        LANGTON("Langton's Ant"),
        SPECTRUM_ANALYZER("Spectrum Analyzer"),
        WAVEFORM("Waveform"),
        OSCILLOSCOPE("Oscilloscope"),
        VU_METERS("VU Meters"),
        FREQUENCY_BANDS("Frequency Bands"),
        MIDI_GRID("MIDI Grid"),
        STEP_SEQUENCER("Step Sequencer"),
        LOOP_PULSE("Loop Pulse"),
        DRUM_PATTERN("Drum Pattern"),
        TIME_DIVISION("Time Division"),
        POLYPHONIC("Polyphonic Lines"),
        PIANO_ROLL("Piano Roll"),
        RUBIKS_COMP("Rubik's Competition"),
        EUCLID("Euclidean Rhythm"),
        MODULAR("Modular CV"),
        POLYRHYTHM("Polyrhythm"),
        ARPEGGIATOR("Arpeggiator"),
        GATE_SEQ("Gate Sequencer"),
        CHORD_PROG("Chord Progression"),
        PROBABILITY("Probability Grid"),
        HARMONICS("Harmonic Series"),
        LFO_MATRIX("LFO Matrix"),
        PHASE_SHIFT("Phase Shifter"),
        XY_PAD("XY Pad"),
        TRIG_BURST("Trigger Burst"),
        TRON("Tron Light Cycles"),
        RACING("Racing"),
        INVADERS("Space Invaders"),
        MISSILE("Missile Command"),
        PACMAN("Pac-Man"),
        BREAKOUT("Breakout"),
        ASTEROID("Asteroids"),
        PONG("Pong Classic"),
        DIGDUG("Dig Dug"),
        CLIMBER("Platform Climber"), // Generic name for similar gameplay style
        FROGGER("Frogger"),
        POLE_POSITION("Pole Position");

        private final String label;

        DisplayMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // Add new fields for game states
    private List<Point> tronTrail1 = new ArrayList<>();
    private List<Point> tronTrail2 = new ArrayList<>();
    private int[] tronDirs1 = { 0, 0 }; // dx, dy
    private int[] tronDirs2 = { 0, 0 };

    private List<Point> asteroids = new ArrayList<>();
    private double[] asteroidAngles = new double[5];
    private Point shipPos = new Point(GRID_COLS / 2, GRID_ROWS / 2);
    private double shipAngle = 0;

    private List<Point> tunnels = new ArrayList<>();
    private Point digger = new Point(GRID_COLS / 2, GRID_ROWS - 1);
    private List<Point> rocks = new ArrayList<>();
    private Point climber = new Point(2, GRID_ROWS - 2);
    private List<Point> platforms = new ArrayList<>();
    private List<Point> ladders = new ArrayList<>();
    private int jumpPhase = 0;
    private boolean isJumping = false;
    private int direction = 1;

    private StatusConsumer statusConsumer;

    // // Add fields for Missile Command
    // private List<Missile> missiles = new ArrayList<>();
    // private List<City> cities = new ArrayList<>();
    // private List<Point> blocks = new ArrayList<>();

    // // Add fields for Missile Command
    // private List<Missile> missiles = new ArrayList<>();
    // private List<City> cities = new ArrayList<>();
    // private List<Explosion> explosions = new ArrayList<>();

    private static class Missile {
        Point start, end;
        double progress;
        boolean enemy;

        Missile(Point start, Point end, boolean enemy) {
            this.start = start;
            this.end = end;
            this.progress = 0;
            this.enemy = enemy;
        }
    }

    private static class City {
        int x;
        boolean alive;

        City(int x) {
            this.x = x;
            this.alive = true;
        }
    }

    private static class Explosion {
        Point pos;
        int radius;
        int maxRadius;

        Explosion(Point pos) {
            this.pos = pos;
            this.radius = 0;
            this.maxRadius = 2;
        }
    }

    private Map<DisplayMode, Visualization> visualizations = new HashMap<>();

    public Display(JComponent parent, StatusConsumer statusConsumer, GridButton[][] buttons) {
        this.parent = parent;
        this.statusConsumer = statusConsumer;
        this.buttons = buttons;
        initializeVisualizations();
        setupTimers();
        setupAnimation();
        additionalSetup();
    }

    private void initializeVisualizations() {
        visualizations.put(DisplayMode.EXPLOSION, new ExplosionVisualization());
        visualizations.put(DisplayMode.SPACE, new SpaceVisualization());
        visualizations.put(DisplayMode.GAME, new GameOfLifeVisualization());
        visualizations.put(DisplayMode.RAIN, new MatrixRainVisualization());
        visualizations.put(DisplayMode.WAVE, new WaveVisualization());
        visualizations.put(DisplayMode.BOUNCE, new BounceVisualization());
        visualizations.put(DisplayMode.SNAKE, new SnakeVisualization());
        visualizations.put(DisplayMode.SPIRAL, new SpiralVisualization());
        visualizations.put(DisplayMode.FIREWORKS, new FireworksVisualization());
        visualizations.put(DisplayMode.CONFETTI, new ConfettiVisualization());
        visualizations.put(DisplayMode.MATRIX, new MatrixVisualization());
        visualizations.put(DisplayMode.HEART, new HeartVisualization());
        visualizations.put(DisplayMode.DNA, new DNAVisualization());
        visualizations.put(DisplayMode.SPECTRUM_ANALYZER, new SpectrumAnalyzerVisualization());
        visualizations.put(DisplayMode.VU_METERS, new VUMeterVisualization());
        visualizations.put(DisplayMode.PLASMA, new PlasmaVisualization());
        visualizations.put(DisplayMode.MANDELBROT, new MandelbrotVisualization());
        visualizations.put(DisplayMode.KALEIDOSCOPE, new KaleidoscopeVisualization());
        visualizations.put(DisplayMode.RIPPLE, new RippleVisualization());
        visualizations.put(DisplayMode.PACMAN, new PacmanVisualization());
        visualizations.put(DisplayMode.INVADERS, new SpaceInvadersVisualization());
        visualizations.put(DisplayMode.STEP_SEQUENCER, new StepSequencerVisualization());
        visualizations.put(DisplayMode.DRUM_PATTERN, new DrumPatternVisualization());
        visualizations.put(DisplayMode.EQUALIZER, new EqualizerVisualization());
        visualizations.put(DisplayMode.OSCILLOSCOPE, new OscilloscopeVisualization());
        visualizations.put(DisplayMode.FREQUENCY_BANDS, new FrequencyBandsVisualization());
        visualizations.put(DisplayMode.MIDI_GRID, new MidiGridVisualization());
        visualizations.put(DisplayMode.PIANO_ROLL, new PianoRollVisualization());
        visualizations.put(DisplayMode.ARPEGGIATOR, new ArpeggiatorVisualization());
        visualizations.put(DisplayMode.POLYPHONIC, new PolyphonicVisualization());
        visualizations.put(DisplayMode.MODULAR, new ModularCVVisualization());
        visualizations.put(DisplayMode.LFO_MATRIX, new LFOMatrixVisualization());
        visualizations.put(DisplayMode.PHASE_SHIFT, new PhaseShiftVisualization());
        visualizations.put(DisplayMode.GATE_SEQ, new GateSequencerVisualization());
        visualizations.put(DisplayMode.CHORD_PROG, new ChordProgressionVisualization());
        visualizations.put(DisplayMode.HARMONICS, new HarmonicsVisualization());
        visualizations.put(DisplayMode.TIME_DIVISION, new TimeDivisionVisualization());
        visualizations.put(DisplayMode.POLYRHYTHM, new PolyrhythmVisualization());
        visualizations.put(DisplayMode.XY_PAD, new XYPadVisualization());
        visualizations.put(DisplayMode.LOOP_PULSE, new LoopPulseVisualization());
        visualizations.put(DisplayMode.TRIG_BURST, new TriggerBurstVisualization());
        visualizations.put(DisplayMode.EUCLID, new EuclideanRhythmVisualization());
        visualizations.put(DisplayMode.PROBABILITY, new ProbabilityGridVisualization());
        visualizations.put(DisplayMode.PULSE, new PulseVisualization());
        visualizations.put(DisplayMode.RAINBOW, new RainbowVisualization());
        visualizations.put(DisplayMode.PONG, new PongVisualization());
        visualizations.put(DisplayMode.BREAKOUT, new BreakoutVisualization());
        visualizations.put(DisplayMode.ASTEROID, new AsteroidsVisualization());
        visualizations.put(DisplayMode.MISSILE, new MissileCommandVisualization());
        visualizations.put(DisplayMode.TRON, new TronVisualization());
        visualizations.put(DisplayMode.DIGDUG, new DigDugVisualization());
        visualizations.put(DisplayMode.CLIMBER, new PlatformClimberVisualization());
        visualizations.put(DisplayMode.RACING, new RacingVisualization());
        visualizations.put(DisplayMode.CLOCK, new ClockVisualization());
        visualizations.put(DisplayMode.STARFIELD, new StarfieldVisualization());
        visualizations.put(DisplayMode.MAZE, new MazeVisualization());
        visualizations.put(DisplayMode.CRYSTAL, new CrystalVisualization());
        visualizations.put(DisplayMode.CELLULAR, new CellularVisualization());
        visualizations.put(DisplayMode.LIFE_SOUP, new LifeSoupVisualization());
        visualizations.put(DisplayMode.BROWNIAN, new BrownianVisualization());
        visualizations.put(DisplayMode.BINARY, new BinaryRainVisualization());
        visualizations.put(DisplayMode.LANGTON, new LangtonVisualization());
        visualizations.put(DisplayMode.FROGGER, new FroggerVisualization());
        visualizations.put(DisplayMode.POLE_POSITION, new PolePositionVisualization());
        // Add more visualizations as we create them...
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
        if (!isScreensaverMode || currentMode == null)
            return;

        Visualization viz = visualizations.get(currentMode);
        if (viz != null) {
            if (statusConsumer != null) {
                statusConsumer.setSender(viz.getName());
            }
            try {
                viz.update(buttons);
            } catch (Exception e) {
                System.err.println(viz.getName() + " Error updating display: " + e.getMessage());
            }
        }
    }

    private void setSender(String string) {
        if (Objects.nonNull(statusConsumer))
            statusConsumer.setSender(string);
    }

    // Keep supporting classes and methods that are shared across visualizations

    // ... other necessary supporting classes ...
}
