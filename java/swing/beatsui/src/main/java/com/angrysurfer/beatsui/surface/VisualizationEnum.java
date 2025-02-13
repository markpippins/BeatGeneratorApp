package com.angrysurfer.beatsui.surface;

import com.angrysurfer.beatsui.surface.visualization.ArpeggiatorVisualization;
import com.angrysurfer.beatsui.surface.visualization.AsteroidsVisualization;
import com.angrysurfer.beatsui.surface.visualization.BinaryRainVisualization;
import com.angrysurfer.beatsui.surface.visualization.BounceVisualization;
import com.angrysurfer.beatsui.surface.visualization.BreakoutVisualization;
import com.angrysurfer.beatsui.surface.visualization.BrownianVisualization;
import com.angrysurfer.beatsui.surface.visualization.CellularVisualization;
import com.angrysurfer.beatsui.surface.visualization.ChordProgressionVisualization;
import com.angrysurfer.beatsui.surface.visualization.ClockVisualization;
import com.angrysurfer.beatsui.surface.visualization.CombatVisualization;
import com.angrysurfer.beatsui.surface.visualization.ConfettiVisualization;
import com.angrysurfer.beatsui.surface.visualization.CrystalVisualization;
import com.angrysurfer.beatsui.surface.visualization.DNAVisualization;
import com.angrysurfer.beatsui.surface.visualization.DigDugVisualization;
import com.angrysurfer.beatsui.surface.visualization.DrumPatternVisualization;
import com.angrysurfer.beatsui.surface.visualization.EqualizerVisualization;
import com.angrysurfer.beatsui.surface.visualization.ExplosionVisualization;
import com.angrysurfer.beatsui.surface.visualization.FireworksVisualization;
import com.angrysurfer.beatsui.surface.visualization.FlatlandVisualization;
import com.angrysurfer.beatsui.surface.visualization.FrequencyBandsVisualization;
import com.angrysurfer.beatsui.surface.visualization.FroggerVisualization;
import com.angrysurfer.beatsui.surface.visualization.GameOfLifeVisualization;
import com.angrysurfer.beatsui.surface.visualization.GateSequencerVisualization;
import com.angrysurfer.beatsui.surface.visualization.HarmonicsVisualization;
import com.angrysurfer.beatsui.surface.visualization.HeartVisualization;
import com.angrysurfer.beatsui.surface.visualization.KaleidoscopeVisualization;
import com.angrysurfer.beatsui.surface.visualization.KineticsVisualization;
import com.angrysurfer.beatsui.surface.visualization.LFOMatrixVisualization;
import com.angrysurfer.beatsui.surface.visualization.LangtonVisualization;
import com.angrysurfer.beatsui.surface.visualization.LifeSoupVisualization;
import com.angrysurfer.beatsui.surface.visualization.LightSpeedVisualization;
import com.angrysurfer.beatsui.surface.visualization.LoopPulseVisualization;
import com.angrysurfer.beatsui.surface.visualization.MandelbrotVisualization;
import com.angrysurfer.beatsui.surface.visualization.MatrixRainVisualization;
import com.angrysurfer.beatsui.surface.visualization.MatrixVisualization;
import com.angrysurfer.beatsui.surface.visualization.MazeVisualization;
import com.angrysurfer.beatsui.surface.visualization.PianoRollVisualization;
import com.angrysurfer.beatsui.surface.visualization.PlasmaVisualization;
import com.angrysurfer.beatsui.surface.visualization.PongVisualization;
import com.angrysurfer.beatsui.surface.visualization.PulseVisualization;
import com.angrysurfer.beatsui.surface.visualization.RainbowVisualization;
import com.angrysurfer.beatsui.surface.visualization.RippleVisualization;
import com.angrysurfer.beatsui.surface.visualization.SnakeVisualization;
import com.angrysurfer.beatsui.surface.visualization.SpaceVisualization;
import com.angrysurfer.beatsui.surface.visualization.SpectrumAnalyzerVisualization;
import com.angrysurfer.beatsui.surface.visualization.SpiralVisualization;
import com.angrysurfer.beatsui.surface.visualization.StarfieldVisualization;
import com.angrysurfer.beatsui.surface.visualization.TetrisVisualization;
import com.angrysurfer.beatsui.surface.visualization.WaveVisualization;
import com.angrysurfer.beatsui.surface.visualization.PolePositionVisualization;
import com.angrysurfer.beatsui.surface.visualization.PolyphonicVisualization;
import com.angrysurfer.beatsui.surface.visualization.PolyrhythmVisualization;
import com.angrysurfer.beatsui.surface.visualization.ProbabilityGridVisualization;
import com.angrysurfer.beatsui.surface.visualization.RacingVisualization;
import com.angrysurfer.beatsui.surface.visualization.RubiksCompVisualization;
import com.angrysurfer.beatsui.surface.visualization.SpaceInvadersVisualization;
import com.angrysurfer.beatsui.surface.visualization.StarTrekVisualization;
import com.angrysurfer.beatsui.surface.visualization.StepSequencerVisualization;
import com.angrysurfer.beatsui.surface.visualization.TimeDivisionVisualization;
import com.angrysurfer.beatsui.surface.visualization.TriggerBurstVisualization;
import com.angrysurfer.beatsui.surface.visualization.TronVisualization;
import com.angrysurfer.beatsui.surface.visualization.VUMeterVisualization;
import com.angrysurfer.beatsui.surface.visualization.XYPadVisualization;

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
    PIANO_ROLL(PianoRollVisualization.class),
    PING_PONG(PongVisualization.class),
    PLASMA(PlasmaVisualization.class),
    POLE_POSITION(PolePositionVisualization.class),
    POLYPHONIC(PolyphonicVisualization.class),
    POLYRHYTHM(PolyrhythmVisualization.class),
    // PONG(PongVisualization.class),
    PROBABILITY_GRID(ProbabilityGridVisualization.class),
    PULSE(PulseVisualization.class),
    RACING(RacingVisualization.class),
    RAIN(MatrixRainVisualization.class),
    RAINBOW(RainbowVisualization.class),
    RAINBOW_MATRIX(RainbowVisualization.class),
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

    public static VisualizationEnum fromLabel(String label) {
        try {
            return valueOf(label.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
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