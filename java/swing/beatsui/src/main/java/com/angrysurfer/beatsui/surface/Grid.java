package com.angrysurfer.beatsui.surface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.Timer;

import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.surface.visualization.ArpeggiatorVisualization;
import com.angrysurfer.beatsui.surface.visualization.AsteroidsVisualization;
import com.angrysurfer.beatsui.surface.visualization.BinaryRainVisualization;
import com.angrysurfer.beatsui.surface.visualization.BounceVisualization;
import com.angrysurfer.beatsui.surface.visualization.BreakoutVisualization;
import com.angrysurfer.beatsui.surface.visualization.BrownianVisualization;
import com.angrysurfer.beatsui.surface.visualization.CellularVisualization;
import com.angrysurfer.beatsui.surface.visualization.ChordProgressionVisualization;
import com.angrysurfer.beatsui.surface.visualization.ClockVisualization;
import com.angrysurfer.beatsui.surface.visualization.ConfettiVisualization;
import com.angrysurfer.beatsui.surface.visualization.CrystalVisualization;
import com.angrysurfer.beatsui.surface.visualization.DNAVisualization;
import com.angrysurfer.beatsui.surface.visualization.DigDugVisualization;
import com.angrysurfer.beatsui.surface.visualization.DrumPatternVisualization;
import com.angrysurfer.beatsui.surface.visualization.EqualizerVisualization;
import com.angrysurfer.beatsui.surface.visualization.EuclideanRhythmVisualization;
import com.angrysurfer.beatsui.surface.visualization.ExplosionVisualization;
import com.angrysurfer.beatsui.surface.visualization.FireworksVisualization;
import com.angrysurfer.beatsui.surface.visualization.FrequencyBandsVisualization;
import com.angrysurfer.beatsui.surface.visualization.FroggerVisualization;
import com.angrysurfer.beatsui.surface.visualization.GameOfLifeVisualization;
import com.angrysurfer.beatsui.surface.visualization.GateSequencerVisualization;
import com.angrysurfer.beatsui.surface.visualization.HarmonicsVisualization;
import com.angrysurfer.beatsui.surface.visualization.HeartVisualization;
import com.angrysurfer.beatsui.surface.visualization.KaleidoscopeVisualization;
import com.angrysurfer.beatsui.surface.visualization.LFOMatrixVisualization;
import com.angrysurfer.beatsui.surface.visualization.LangtonVisualization;
import com.angrysurfer.beatsui.surface.visualization.LifeSoupVisualization;
import com.angrysurfer.beatsui.surface.visualization.LoopPulseVisualization;
import com.angrysurfer.beatsui.surface.visualization.MandelbrotVisualization;
import com.angrysurfer.beatsui.surface.visualization.MatrixRainVisualization;
import com.angrysurfer.beatsui.surface.visualization.MatrixVisualization;
import com.angrysurfer.beatsui.surface.visualization.MazeVisualization;
import com.angrysurfer.beatsui.surface.visualization.MidiGridVisualization;
import com.angrysurfer.beatsui.surface.visualization.MissileCommandVisualization;
import com.angrysurfer.beatsui.surface.visualization.ModularCVVisualization;
import com.angrysurfer.beatsui.surface.visualization.OscilloscopeVisualization;
import com.angrysurfer.beatsui.surface.visualization.PacmanVisualization;
import com.angrysurfer.beatsui.surface.visualization.PhaseShiftVisualization;
import com.angrysurfer.beatsui.surface.visualization.PianoRollVisualization;
import com.angrysurfer.beatsui.surface.visualization.PlasmaVisualization;
import com.angrysurfer.beatsui.surface.visualization.PlatformClimberVisualization;
import com.angrysurfer.beatsui.surface.visualization.PolePositionVisualization;
import com.angrysurfer.beatsui.surface.visualization.PolyphonicVisualization;
import com.angrysurfer.beatsui.surface.visualization.PolyrhythmVisualization;
import com.angrysurfer.beatsui.surface.visualization.PongVisualization;
import com.angrysurfer.beatsui.surface.visualization.ProbabilityGridVisualization;
import com.angrysurfer.beatsui.surface.visualization.PulseVisualization;
import com.angrysurfer.beatsui.surface.visualization.RacingVisualization;
import com.angrysurfer.beatsui.surface.visualization.RainbowJapaneseMatrixVisualization;
import com.angrysurfer.beatsui.surface.visualization.RainbowVisualization;
import com.angrysurfer.beatsui.surface.visualization.RippleVisualization;
import com.angrysurfer.beatsui.surface.visualization.RubiksCompVisualization;
import com.angrysurfer.beatsui.surface.visualization.SnakeVisualization;
import com.angrysurfer.beatsui.surface.visualization.SpaceInvadersVisualization;
import com.angrysurfer.beatsui.surface.visualization.SpaceVisualization;
import com.angrysurfer.beatsui.surface.visualization.SpectrumAnalyzerVisualization;
import com.angrysurfer.beatsui.surface.visualization.SpiralVisualization;
import com.angrysurfer.beatsui.surface.visualization.StarfieldVisualization;
import com.angrysurfer.beatsui.surface.visualization.StepSequencerVisualization;
import com.angrysurfer.beatsui.surface.visualization.TimeDivisionVisualization;
import com.angrysurfer.beatsui.surface.visualization.TriggerBurstVisualization;
import com.angrysurfer.beatsui.surface.visualization.TronVisualization;
import com.angrysurfer.beatsui.surface.visualization.VUMeterVisualization;
import com.angrysurfer.beatsui.surface.visualization.WaveVisualization;
import com.angrysurfer.beatsui.surface.visualization.XYPadVisualization;
import com.angrysurfer.beatsui.widget.GridButton;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Grid {

    private final JComponent parent;

    private GridButton[][] buttons;
    private Timer animationTimer;
    private VisualizationEnum currentVisualization = null; // Changed from TEXT to null

    private Random random = new Random();

    private boolean isVisualizationMode = false;
    private long lastInteraction;

    private Timer visualizationTimer;
    private Timer visualizationChangeTimer;

    private static final int VISUALIZATION_DELAY = 30; // 30 seconds
    private static final int VISUALIZATION_CHANGE_DELAY = 10000; // 10 seconds

    private StatusConsumer statusConsumer;

    private Map<VisualizationEnum, VisualizationHandler> visualizations = new HashMap<>();

    public Grid(JComponent parent, StatusConsumer statusConsumer, GridButton[][] buttons) {
        this.parent = parent;
        this.statusConsumer = statusConsumer;
        this.buttons = buttons;
        initializeVisualizations();
        setupTimers();
        setupAnimation();
    }

    private void initializeVisualizations() {
        visualizations.put(VisualizationEnum.EXPLOSION, new ExplosionVisualization());
        visualizations.put(VisualizationEnum.SPACE, new SpaceVisualization());
        visualizations.put(VisualizationEnum.GAME, new GameOfLifeVisualization());
        visualizations.put(VisualizationEnum.RAIN, new MatrixRainVisualization());
        visualizations.put(VisualizationEnum.WAVE, new WaveVisualization());
        visualizations.put(VisualizationEnum.BOUNCE, new BounceVisualization());
        visualizations.put(VisualizationEnum.SNAKE, new SnakeVisualization());
        visualizations.put(VisualizationEnum.SPIRAL, new SpiralVisualization());
        visualizations.put(VisualizationEnum.FIREWORKS, new FireworksVisualization());
        visualizations.put(VisualizationEnum.CONFETTI, new ConfettiVisualization());
        visualizations.put(VisualizationEnum.MATRIX, new MatrixVisualization());
        visualizations.put(VisualizationEnum.HEART, new HeartVisualization());
        visualizations.put(VisualizationEnum.DNA, new DNAVisualization());
        visualizations.put(VisualizationEnum.SPECTRUM_ANALYZER, new SpectrumAnalyzerVisualization());
        visualizations.put(VisualizationEnum.VU_METERS, new VUMeterVisualization());
        visualizations.put(VisualizationEnum.PLASMA, new PlasmaVisualization());
        visualizations.put(VisualizationEnum.MANDELBROT, new MandelbrotVisualization());
        visualizations.put(VisualizationEnum.KALEIDOSCOPE, new KaleidoscopeVisualization());
        visualizations.put(VisualizationEnum.RIPPLE, new RippleVisualization());
        visualizations.put(VisualizationEnum.PACMAN, new PacmanVisualization());
        visualizations.put(VisualizationEnum.INVADERS, new SpaceInvadersVisualization());
        visualizations.put(VisualizationEnum.STEP_SEQUENCER, new StepSequencerVisualization());
        visualizations.put(VisualizationEnum.DRUM_PATTERN, new DrumPatternVisualization());
        visualizations.put(VisualizationEnum.EQUALIZER, new EqualizerVisualization());
        visualizations.put(VisualizationEnum.OSCILLOSCOPE, new OscilloscopeVisualization());
        visualizations.put(VisualizationEnum.FREQUENCY_BANDS, new FrequencyBandsVisualization());
        visualizations.put(VisualizationEnum.MIDI_GRID, new MidiGridVisualization());
        visualizations.put(VisualizationEnum.PIANO_ROLL, new PianoRollVisualization());
        visualizations.put(VisualizationEnum.ARPEGGIATOR, new ArpeggiatorVisualization());
        visualizations.put(VisualizationEnum.POLYPHONIC, new PolyphonicVisualization());
        visualizations.put(VisualizationEnum.MODULAR, new ModularCVVisualization());
        visualizations.put(VisualizationEnum.LFO_MATRIX, new LFOMatrixVisualization());
        visualizations.put(VisualizationEnum.PHASE_SHIFT, new PhaseShiftVisualization());
        visualizations.put(VisualizationEnum.GATE_SEQ, new GateSequencerVisualization());
        visualizations.put(VisualizationEnum.CHORD_PROG, new ChordProgressionVisualization());
        visualizations.put(VisualizationEnum.HARMONICS, new HarmonicsVisualization());
        visualizations.put(VisualizationEnum.TIME_DIVISION, new TimeDivisionVisualization());
        visualizations.put(VisualizationEnum.POLYRHYTHM, new PolyrhythmVisualization());
        visualizations.put(VisualizationEnum.XY_PAD, new XYPadVisualization());
        visualizations.put(VisualizationEnum.LOOP_PULSE, new LoopPulseVisualization());
        visualizations.put(VisualizationEnum.TRIG_BURST, new TriggerBurstVisualization());
        visualizations.put(VisualizationEnum.EUCLID, new EuclideanRhythmVisualization());
        visualizations.put(VisualizationEnum.PROBABILITY, new ProbabilityGridVisualization());
        visualizations.put(VisualizationEnum.PULSE, new PulseVisualization());
        visualizations.put(VisualizationEnum.RAINBOW, new RainbowVisualization());
        visualizations.put(VisualizationEnum.PONG, new PongVisualization());
        visualizations.put(VisualizationEnum.BREAKOUT, new BreakoutVisualization());
        visualizations.put(VisualizationEnum.ASTEROID, new AsteroidsVisualization());
        visualizations.put(VisualizationEnum.MISSILE, new MissileCommandVisualization());
        visualizations.put(VisualizationEnum.TRON, new TronVisualization());
        visualizations.put(VisualizationEnum.DIGDUG, new DigDugVisualization());
        visualizations.put(VisualizationEnum.CLIMBER, new PlatformClimberVisualization());
        visualizations.put(VisualizationEnum.RACING, new RacingVisualization());
        visualizations.put(VisualizationEnum.CLOCK, new ClockVisualization());
        visualizations.put(VisualizationEnum.STARFIELD, new StarfieldVisualization());
        visualizations.put(VisualizationEnum.MAZE, new MazeVisualization());
        visualizations.put(VisualizationEnum.CRYSTAL, new CrystalVisualization());
        visualizations.put(VisualizationEnum.CELLULAR, new CellularVisualization());
        visualizations.put(VisualizationEnum.LIFE_SOUP, new LifeSoupVisualization());
        visualizations.put(VisualizationEnum.BROWNIAN, new BrownianVisualization());
        visualizations.put(VisualizationEnum.BINARY, new BinaryRainVisualization());
        visualizations.put(VisualizationEnum.LANGTON, new LangtonVisualization());
        visualizations.put(VisualizationEnum.FROGGER, new FroggerVisualization());
        visualizations.put(VisualizationEnum.POLE_POSITION, new PolePositionVisualization());
        visualizations.put(VisualizationEnum.RUBIKS_COMP, new RubiksCompVisualization());
        visualizations.put(VisualizationEnum.RAINBOW_MATRIX, new RainbowJapaneseMatrixVisualization());
        // Add more visualizations as we create them...
    }

    private void setupTimers() {
        lastInteraction = System.currentTimeMillis();

        visualizationTimer = new Timer(1000, e -> checkScreensaver());
        visualizationTimer.start();

        visualizationChangeTimer = new Timer(VISUALIZATION_CHANGE_DELAY, e -> {
            if (isVisualizationMode) {
                setDisplayMode(VisualizationEnum.values()[random.nextInt(VisualizationEnum.values().length)]);
            }
        });
    }

    public void checkScreensaver() {
        long timeSinceLastInteraction = System.currentTimeMillis() - lastInteraction;
        if (!isVisualizationMode && timeSinceLastInteraction > VISUALIZATION_DELAY) {
            startScreensaver();
        }
    }

    public void startScreensaver() {
        isVisualizationMode = true;
        visualizationChangeTimer.start();
        setDisplayMode(VisualizationEnum.values()[random.nextInt(VisualizationEnum.values().length)]);
    }

    public void stopScreensaver() {
        isVisualizationMode = false;
        visualizationChangeTimer.stop();
        clearDisplay();
        currentVisualization = null; // Reset current mode
        lastInteraction = System.currentTimeMillis(); // Reset timer
    }

    private void setupAnimation() {
        animationTimer = new Timer(100, e -> updateDisplay());
        animationTimer.start();
    }

    private void setDisplayMode(VisualizationEnum mode) {
        currentVisualization = mode;
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
        if (!isVisualizationMode || currentVisualization == null)
            return;

        VisualizationHandler viz = visualizations.get(currentVisualization);
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
