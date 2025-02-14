package com.angrysurfer.beatsui.visualization;

import com.angrysurfer.beatsui.visualization.handler.ArpeggiatorVisualization;
import com.angrysurfer.beatsui.visualization.handler.AsteroidsVisualization;
import com.angrysurfer.beatsui.visualization.handler.BinaryRainVisualization;
import com.angrysurfer.beatsui.visualization.handler.BounceVisualization;
import com.angrysurfer.beatsui.visualization.handler.BreakoutVisualization;
import com.angrysurfer.beatsui.visualization.handler.BrownianVisualization;
import com.angrysurfer.beatsui.visualization.handler.CellularVisualization;
import com.angrysurfer.beatsui.visualization.handler.ChordProgressionVisualization;
import com.angrysurfer.beatsui.visualization.handler.ClockVisualization;
import com.angrysurfer.beatsui.visualization.handler.CombatVisualization;
import com.angrysurfer.beatsui.visualization.handler.ConfettiVisualization;
import com.angrysurfer.beatsui.visualization.handler.CrystalVisualization;
import com.angrysurfer.beatsui.visualization.handler.DNAVisualization;
import com.angrysurfer.beatsui.visualization.handler.DigDugVisualization;
import com.angrysurfer.beatsui.visualization.handler.DrumPatternVisualization;
import com.angrysurfer.beatsui.visualization.handler.EqualizerVisualization;
import com.angrysurfer.beatsui.visualization.handler.ExplosionVisualization;
import com.angrysurfer.beatsui.visualization.handler.FireworksVisualization;
import com.angrysurfer.beatsui.visualization.handler.FlatlandVisualization;
import com.angrysurfer.beatsui.visualization.handler.FrequencyBandsVisualization;
import com.angrysurfer.beatsui.visualization.handler.FroggerVisualization;
import com.angrysurfer.beatsui.visualization.handler.GameOfLifeVisualization;
import com.angrysurfer.beatsui.visualization.handler.GateSequencerVisualization;
import com.angrysurfer.beatsui.visualization.handler.HarmonicsVisualization;
import com.angrysurfer.beatsui.visualization.handler.HeartVisualization;
import com.angrysurfer.beatsui.visualization.handler.KaleidoscopeVisualization;
import com.angrysurfer.beatsui.visualization.handler.KineticsVisualization;
import com.angrysurfer.beatsui.visualization.handler.LFOMatrixVisualization;
import com.angrysurfer.beatsui.visualization.handler.LangtonVisualization;
import com.angrysurfer.beatsui.visualization.handler.LifeSoupVisualization;
import com.angrysurfer.beatsui.visualization.handler.LightSpeedVisualization;
import com.angrysurfer.beatsui.visualization.handler.LoopPulseVisualization;
import com.angrysurfer.beatsui.visualization.handler.MandelbrotVisualization;
import com.angrysurfer.beatsui.visualization.handler.MapVisualization;
import com.angrysurfer.beatsui.visualization.handler.MatrixRainVisualization;
import com.angrysurfer.beatsui.visualization.handler.MatrixVisualization;
import com.angrysurfer.beatsui.visualization.handler.MazeVisualization;
import com.angrysurfer.beatsui.visualization.handler.PianoRollVisualization;
import com.angrysurfer.beatsui.visualization.handler.PlasmaVisualization;
import com.angrysurfer.beatsui.visualization.handler.PolePositionVisualization;
import com.angrysurfer.beatsui.visualization.handler.PolyphonicVisualization;
import com.angrysurfer.beatsui.visualization.handler.PolyrhythmVisualization;
import com.angrysurfer.beatsui.visualization.handler.PongVisualization;
import com.angrysurfer.beatsui.visualization.handler.ProbabilityGridVisualization;
import com.angrysurfer.beatsui.visualization.handler.PulseVisualization;
import com.angrysurfer.beatsui.visualization.handler.RacingVisualization;
import com.angrysurfer.beatsui.visualization.handler.RainbowJapaneseMatrixVisualization;
import com.angrysurfer.beatsui.visualization.handler.RainbowVisualization;
import com.angrysurfer.beatsui.visualization.handler.RippleVisualization;
import com.angrysurfer.beatsui.visualization.handler.RubiksCompVisualization;
import com.angrysurfer.beatsui.visualization.handler.SnakeVisualization;
import com.angrysurfer.beatsui.visualization.handler.SpaceInvadersVisualization;
import com.angrysurfer.beatsui.visualization.handler.SpaceVisualization;
import com.angrysurfer.beatsui.visualization.handler.SpectrumAnalyzerVisualization;
import com.angrysurfer.beatsui.visualization.handler.SpiralVisualization;
import com.angrysurfer.beatsui.visualization.handler.StarTrekVisualization;
import com.angrysurfer.beatsui.visualization.handler.StarfieldVisualization;
import com.angrysurfer.beatsui.visualization.handler.StepSequencerVisualization;
import com.angrysurfer.beatsui.visualization.handler.TetrisVisualization;
import com.angrysurfer.beatsui.visualization.handler.TimeDivisionVisualization;
import com.angrysurfer.beatsui.visualization.handler.TriggerBurstVisualization;
import com.angrysurfer.beatsui.visualization.handler.TronVisualization;
import com.angrysurfer.beatsui.visualization.handler.VUMeterVisualization;
import com.angrysurfer.beatsui.visualization.handler.WaveVisualization;
import com.angrysurfer.beatsui.visualization.handler.XYPadVisualization;

public enum VisualizationEnum {
    ARPEGGIATOR(ArpeggiatorVisualization.class),
    ASTEROID(AsteroidsVisualization.class),
    BINARY(BinaryRainVisualization.class),
    BOUNCE(BounceVisualization.class),
    BREAKOUT(BreakoutVisualization.class),
    BROWNIAN(BrownianVisualization.class),
    CELLULAR(CellularVisualization.class),
    CHORD_PROGRESSION(ChordProgressionVisualization.class),
    CLOCK(ClockVisualization.class),
    COMBAT(CombatVisualization.class),
    CONFETTI(ConfettiVisualization.class),
    CRYSTAL(CrystalVisualization.class),
    DIGDUG(DigDugVisualization.class),
    DNA(DNAVisualization.class),
    DRUM_PATTERN(DrumPatternVisualization.class),
    EQUALIZER(EqualizerVisualization.class),
    EXPLOSION(ExplosionVisualization.class),
    FIREWORKS(FireworksVisualization.class),
    FLATLAND(FlatlandVisualization.class),
    FREQUENCY_BANDS(FrequencyBandsVisualization.class),
    FROGGER(FroggerVisualization.class),
    GAME(GameOfLifeVisualization.class),
    GATE_SEQUENCER(GateSequencerVisualization.class),
    HARMONICS(HarmonicsVisualization.class),
    HEART(HeartVisualization.class),
    KALEIDOSCOPE(KaleidoscopeVisualization.class),
    KINETICS(KineticsVisualization.class),
    LANGTON(LangtonVisualization.class),
    LFO_MATRIX(LFOMatrixVisualization.class),
    LIFE_SOUP(LifeSoupVisualization.class),
    LIGHT_SPEED(LightSpeedVisualization.class),
    LOOP_PULSE(LoopPulseVisualization.class),
    MANDELBROT(MandelbrotVisualization.class),
    MATRIX(MatrixVisualization.class),
    MAZE(MazeVisualization.class),
    MAP(MapVisualization.class),
    PIANO_ROLL(PianoRollVisualization.class),
    PING_PONG(PongVisualization.class),
    PLASMA(PlasmaVisualization.class),
    POLE_POSITION(PolePositionVisualization.class),
    POLYPHONIC(PolyphonicVisualization.class),
    POLYRHYTHM(PolyrhythmVisualization.class),
    PROBABILITY_GRID(ProbabilityGridVisualization.class),
    PULSE(PulseVisualization.class),
    RACING(RacingVisualization.class),
    RAIN(MatrixRainVisualization.class),
    RAINBOW(RainbowVisualization.class),
    RAINBOW_MATRIX(RainbowJapaneseMatrixVisualization.class),
    RIPPLE(RippleVisualization.class),
    RUBIKS_COMP(RubiksCompVisualization.class),
    SNAKE(SnakeVisualization.class),
    SPACE(SpaceVisualization.class),
    SPACE_INVADERS(SpaceInvadersVisualization.class),
    SPECTRUM_ANALYZER(SpectrumAnalyzerVisualization.class),
    SPIRAL(SpiralVisualization.class),
    STAR_TREK(StarTrekVisualization.class),
    STARFIELD(StarfieldVisualization.class),
    STEP_SEQUENCER(StepSequencerVisualization.class),
    TETRIS(TetrisVisualization.class),
    TIME_DIVISION(TimeDivisionVisualization.class),
    TRIGGER_BURST(TriggerBurstVisualization.class),
    TRON(TronVisualization.class),
    VU_METER(VUMeterVisualization.class),
    WAVE(WaveVisualization.class),
    XY_PAD(XYPadVisualization.class);

    private final Class<? extends VisualizationHandler> visualizationClass;

    VisualizationEnum(Class<? extends VisualizationHandler> visualizationClass) {
        this.visualizationClass = visualizationClass;
    }

    public Class<? extends VisualizationHandler> getVisualizationClass() {
        return visualizationClass;
    }

    public static VisualizationEnum fromHandler(VisualizationHandler handler) {
        for (VisualizationEnum viz : values()) {
            if (viz.getVisualizationClass().isInstance(handler)) {
                return viz;
            }
        }
        return null;
    }

    public static VisualizationEnum fromLabel(String label) {
        try {
            String processedLabel = label.toUpperCase().replace(" ", "_");
            System.out.println("Looking for enum with label: " + processedLabel); // Debug line
            return valueOf(processedLabel);
        } catch (IllegalArgumentException e) {
            System.out.println("Failed to find enum for label: " + label); // Debug line
            return null;
        }
    }

    public VisualizationHandler createHandler() {
        try {
            return visualizationClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create visualization handler for " + this.name(), e);
        }
    }
}