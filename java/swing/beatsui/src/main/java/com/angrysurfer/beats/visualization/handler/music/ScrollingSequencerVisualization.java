package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.widget.GridButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.api.Commands;

public class ScrollingSequencerVisualization extends LockHandler implements IVisualizationHandler, CommandListener {
    private static final int BEATS_PER_BAR = 4;
    private static final int TOTAL_BARS = 4;
    private static final int TICKS_PER_BEAT = 24;  // Standard MIDI resolution
    private static final int PIXELS_PER_BEAT = 4;  // How many grid columns per beat
    
    private static final Color BEAT_MARKER_COLOR = new Color(40, 40, 40);
    private static final Color BAR_MARKER_COLOR = new Color(60, 60, 60);
    private static final Color POSITION_INDICATOR = Color.WHITE;
    private static final Color CURRENT_BEAT = Color.GREEN;
    private static final Color CURRENT_BAR = Color.BLUE;

    private int currentTick = 0;
    private int currentBeat = 0;
    private int currentBar = 0;
    private boolean isPlaying = false;

    public ScrollingSequencerVisualization() {
        TimingBus.getInstance().register(this);
    }

    @Override
    public void update(GridButton[][] buttons) {
        if (!isPlaying) return;
        
        int cols = buttons[0].length;
        int rows = buttons.length;
        
        // Clear display
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // First draw the fixed grid showing 4 bars
        int beatsTotal = BEATS_PER_BAR * TOTAL_BARS;
        int columnsPerBeat = cols / beatsTotal;  // Divide grid into equal beats
        
        // Draw beat and bar lines
        for (int beat = 0; beat < beatsTotal; beat++) {
            int x = beat * columnsPerBeat;
            Color lineColor = (beat % BEATS_PER_BAR == 0) ? BAR_MARKER_COLOR : BEAT_MARKER_COLOR;
            
            // Draw vertical lines for beats/bars
            for (int row = 0; row < rows; row++) {
                buttons[row][x].setBackground(lineColor);
            }
        }

        // Calculate playhead position
        int ticksPerBar = TICKS_PER_BEAT * BEATS_PER_BAR;
        int totalTicks = ticksPerBar * TOTAL_BARS;
        
        // Convert current musical position to grid position
        int currentPosition = (currentBar * ticksPerBar) + (currentBeat * TICKS_PER_BEAT) + currentTick;
        int playheadCol = (currentPosition * cols) / totalTicks;
        
        // Draw playhead
        if (playheadCol < cols) {
            for (int row = 0; row < rows; row++) {
                buttons[row][playheadCol].setBackground(POSITION_INDICATOR);
            }
        }

        // Show current beat/bar numbers at top
        buttons[0][0].setText(String.format("Beat: %d", currentBeat + 1));
        buttons[1][0].setText(String.format("Bar: %d", currentBar + 1));
    }

    @Override
    public void onAction(Command action) {
        switch (action.getCommand()) {
            case Commands.TRANSPORT_STATE_CHANGED -> {
                if (action.getData() instanceof Boolean playing) {
                    isPlaying = playing;
                    if (!playing) {
                        resetPosition();
                    }
                }
            }
            case Commands.BASIC_TIMING_TICK -> {
                if (isPlaying) {
                    advancePosition();
                }
            }
        }
    }

    private void resetPosition() {
        currentTick = 0;
        currentBeat = 0;
        currentBar = 0;
    }

    private void advancePosition() {
        currentTick++;
        if (currentTick >= TICKS_PER_BEAT) {
            currentTick = 0;
            currentBeat++;
            if (currentBeat >= BEATS_PER_BAR) {
                currentBeat = 0;
                currentBar++;
                if (currentBar >= TOTAL_BARS) {
                    currentBar = 0;
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Scrolling Sequencer";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}
