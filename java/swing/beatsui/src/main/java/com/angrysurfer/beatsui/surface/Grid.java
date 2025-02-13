package com.angrysurfer.beatsui.surface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.Timer;

import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.surface.visualizer.ArpeggiatorVisualization;
import com.angrysurfer.beatsui.surface.visualizer.AsteroidsVisualization;
import com.angrysurfer.beatsui.surface.visualizer.BinaryRainVisualization;
import com.angrysurfer.beatsui.surface.visualizer.BounceVisualization;
import com.angrysurfer.beatsui.surface.visualizer.BreakoutVisualization;
import com.angrysurfer.beatsui.surface.visualizer.BrownianVisualization;
import com.angrysurfer.beatsui.surface.visualizer.CellularVisualization;
import com.angrysurfer.beatsui.surface.visualizer.ChordProgressionVisualization;
import com.angrysurfer.beatsui.surface.visualizer.ClockVisualization;
import com.angrysurfer.beatsui.surface.visualizer.ConfettiVisualization;
import com.angrysurfer.beatsui.surface.visualizer.CrystalVisualization;
import com.angrysurfer.beatsui.surface.visualizer.DNAVisualization;
import com.angrysurfer.beatsui.surface.visualizer.DigDugVisualization;
import com.angrysurfer.beatsui.surface.visualizer.DrumPatternVisualization;
import com.angrysurfer.beatsui.surface.visualizer.EqualizerVisualization;
import com.angrysurfer.beatsui.surface.visualizer.EuclideanRhythmVisualization;
import com.angrysurfer.beatsui.surface.visualizer.ExplosionVisualization;
import com.angrysurfer.beatsui.surface.visualizer.FireworksVisualization;
import com.angrysurfer.beatsui.surface.visualizer.FrequencyBandsVisualization;
import com.angrysurfer.beatsui.surface.visualizer.FroggerVisualization;
import com.angrysurfer.beatsui.surface.visualizer.GameOfLifeVisualization;
import com.angrysurfer.beatsui.surface.visualizer.GateSequencerVisualization;
import com.angrysurfer.beatsui.surface.visualizer.HarmonicsVisualization;
import com.angrysurfer.beatsui.surface.visualizer.HeartVisualization;
import com.angrysurfer.beatsui.surface.visualizer.KaleidoscopeVisualization;
import com.angrysurfer.beatsui.surface.visualizer.LFOMatrixVisualization;
import com.angrysurfer.beatsui.surface.visualizer.LangtonVisualization;
import com.angrysurfer.beatsui.surface.visualizer.LifeSoupVisualization;
import com.angrysurfer.beatsui.surface.visualizer.LoopPulseVisualization;
import com.angrysurfer.beatsui.surface.visualizer.MandelbrotVisualization;
import com.angrysurfer.beatsui.surface.visualizer.MatrixRainVisualization;
import com.angrysurfer.beatsui.surface.visualizer.MatrixVisualization;
import com.angrysurfer.beatsui.surface.visualizer.MazeVisualization;
import com.angrysurfer.beatsui.surface.visualizer.MidiGridVisualization;
import com.angrysurfer.beatsui.surface.visualizer.MissileCommandVisualization;
import com.angrysurfer.beatsui.surface.visualizer.ModularCVVisualization;
import com.angrysurfer.beatsui.surface.visualizer.OscilloscopeVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PacmanVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PhaseShiftVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PianoRollVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PlasmaVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PlatformClimberVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PolePositionVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PolyphonicVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PolyrhythmVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PongVisualization;
import com.angrysurfer.beatsui.surface.visualizer.ProbabilityGridVisualization;
import com.angrysurfer.beatsui.surface.visualizer.PulseVisualization;
import com.angrysurfer.beatsui.surface.visualizer.RacingVisualization;
import com.angrysurfer.beatsui.surface.visualizer.RainbowJapaneseMatrixVisualization;
import com.angrysurfer.beatsui.surface.visualizer.RainbowVisualization;
import com.angrysurfer.beatsui.surface.visualizer.RippleVisualization;
import com.angrysurfer.beatsui.surface.visualizer.RubiksCompVisualization;
import com.angrysurfer.beatsui.surface.visualizer.SnakeVisualization;
import com.angrysurfer.beatsui.surface.visualizer.SpaceInvadersVisualization;
import com.angrysurfer.beatsui.surface.visualizer.SpaceVisualization;
import com.angrysurfer.beatsui.surface.visualizer.SpectrumAnalyzerVisualization;
import com.angrysurfer.beatsui.surface.visualizer.SpiralVisualization;
import com.angrysurfer.beatsui.surface.visualizer.StarfieldVisualization;
import com.angrysurfer.beatsui.surface.visualizer.StepSequencerVisualization;
import com.angrysurfer.beatsui.surface.visualizer.TimeDivisionVisualization;
import com.angrysurfer.beatsui.surface.visualizer.TriggerBurstVisualization;
import com.angrysurfer.beatsui.surface.visualizer.TronVisualization;
import com.angrysurfer.beatsui.surface.visualizer.VUMeterVisualization;
import com.angrysurfer.beatsui.surface.visualizer.WaveVisualization;
import com.angrysurfer.beatsui.surface.visualizer.XYPadVisualization;
import com.angrysurfer.beatsui.widget.GridButton;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Grid {

    private final JComponent parent;

    // public static final int GRID_ROWS = 8;
    // public static final int GRID_COLS = 36;

    private GridButton[][] buttons;
    private Timer animationTimer;
    private DisplayMode currentMode = null; // Changed from TEXT to null

    private Random random = new Random();

    private Timer screensaverTimer;
    private Timer modeChangeTimer;
    private long lastInteraction;
    private static final int SCREENSAVER_DELAY = 30; // 30 seconds
    private static final int MODE_CHANGE_DELAY = 10000; // 10 seconds
    private boolean isScreensaverMode = false;

    private StatusConsumer statusConsumer;

    private Map<DisplayMode, Visualization> visualizations = new HashMap<>();

    public Grid(JComponent parent, StatusConsumer statusConsumer, GridButton[][] buttons) {
        this.parent = parent;
        this.statusConsumer = statusConsumer;
        this.buttons = buttons;
        initializeVisualizations();
        setupTimers();
        setupAnimation();
        // additionalSetup();
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
        visualizations.put(DisplayMode.RUBIKS_COMP, new RubiksCompVisualization());
        visualizations.put(DisplayMode.RAINBOW_MATRIX, new RainbowJapaneseMatrixVisualization());
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
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                buttons[row][col].reset();
            }
        }
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
