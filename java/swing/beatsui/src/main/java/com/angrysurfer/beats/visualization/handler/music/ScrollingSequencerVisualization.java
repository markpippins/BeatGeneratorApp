package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.widget.GridButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.service.SequencerManager;

public class ScrollingSequencerVisualization extends LockHandler implements IVisualizationHandler, CommandListener {
    private static final int PIXELS_PER_BEAT = 4;  // How many grid columns per beat
    
    private static final Color BEAT_MARKER_COLOR = Color.BLUE; // new Color(40, 40, 40);
    private static final Color BAR_MARKER_COLOR = Color.GREEN; // new Color(60, 60, 60);
    private static final Color POSITION_INDICATOR = Color.WHITE;
    private static final Color CURRENT_BEAT = Color.GREEN;
    private static final Color CURRENT_BAR = Color.BLUE;

    private int currentTick = 0;
    private int currentBeat = 0;
    private int currentBar = 0;
    private boolean isPlaying = false;

    public ScrollingSequencerVisualization() {
        // Register for timing events from SequencerManager
        TimingBus.getInstance().register(this);
        CommandBus.getInstance().register(this); // Also register with CommandBus for transport state
        
        // Initialize with current state from SequencerManager
        SequencerManager sequencer = SequencerManager.getInstance();
        isPlaying = sequencer.isRunning();
        currentTick = sequencer.getCurrentTick();
        currentBeat = sequencer.getCurrentBeat();
        currentBar = sequencer.getCurrentBar();
    }

    @Override
    public void update(GridButton[][] buttons) {
        if (!isPlaying) return;
        
        int cols = buttons[0].length;
        int rows = buttons.length;
        
        // Clear display
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Get timing parameters from SequencerManager for accurate display
        SequencerManager sequencer = SequencerManager.getInstance();
        int ppq = sequencer.getPpq();
        int beatsPerBar = sequencer.getBeatsPerBar();
        int totalBars = 4; // Display 4 bars at a time
        
        // First draw the fixed grid showing beats and bars
        int beatsTotal = beatsPerBar * totalBars;
        int columnsPerBeat = cols / beatsTotal;  // Divide grid into equal beats
        
        // Draw beat and bar lines
        for (int beat = 0; beat < beatsTotal; beat++) {
            int x = beat * columnsPerBeat;
            Color lineColor = (beat % beatsPerBar == 0) ? BAR_MARKER_COLOR : BEAT_MARKER_COLOR;
            
            // Draw vertical lines for beats/bars
            for (int row = 0; row < rows; row++) {
                if (x < cols) {
                    buttons[row][x].setBackground(lineColor);
                }
            }
        }

        // Calculate playhead position
        int ticksPerBar = ppq * beatsPerBar;
        int totalTicks = ticksPerBar * totalBars;
        
        // Convert current musical position to grid position
        int currentPosition = (currentBar % totalBars) * ticksPerBar + 
                             (currentBeat % beatsPerBar) * ppq +
                             currentTick;
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
        buttons[2][0].setText(String.format("Tick: %d", currentTick));
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;

        switch (action.getCommand()) {
            // Transport state events
            case Commands.TRANSPORT_STATE_CHANGED -> {
                if (action.getData() instanceof Boolean playing) {
                    isPlaying = playing;
                    if (!playing) {
                        resetPosition();
                    }
                }
            }
            
            // MIDI timing events - use these directly instead of tracking manually
            case Commands.BASIC_TIMING_TICK -> {
                if (action.getData() instanceof Number tick) {
                    currentTick = tick.intValue();
                }
            }
            case Commands.BASIC_TIMING_BEAT -> {
                if (action.getData() instanceof Number beat) {
                    currentBeat = beat.intValue();
                }
            }
            case Commands.BASIC_TIMING_BAR -> {
                if (action.getData() instanceof Number bar) {
                    currentBar = bar.intValue();
                }
            }
        }
    }

    private void resetPosition() {
        currentTick = 0;
        currentBeat = 0;
        currentBar = 0;
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
