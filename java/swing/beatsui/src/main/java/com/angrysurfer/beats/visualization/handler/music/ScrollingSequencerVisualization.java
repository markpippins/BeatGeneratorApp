package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.service.SequencerManager;

public class ScrollingSequencerVisualization extends LockHandler implements IVisualizationHandler, CommandListener {
    // Frame rate for visual updates (60fps for smoother animation)
    private static final int FRAMES_PER_SECOND = 60;
    
    // Updated colors as requested
    private static final Color BEAT_MARKER_COLOR =  ColorUtils.charcoalGray; // new Color(220, 0, 0);  // Red for beats
    private static final Color BAR_MARKER_COLOR =  Color.BLUE; //ColorUtils.coolBlue; // new Color(0, 0, 220);   // Blue for bars
    private static final Color POSITION_INDICATOR = Color.WHITE; // ColorUtils.deepNavy;// Color.WHITE;
    private static final Color BACKGROUND_COLOR = Color.BLACK; // ColorUtils.warmOffWhite; // new Color(20, 20, 20);
    
    // Volatile fields for thread safety
    private volatile int currentTick = 0;
    private volatile int currentBeat = 0;
    private volatile int currentBar = 0;
    private volatile boolean isPlaying = false;
    
    // Previous state to avoid unnecessary updates
    private int lastDisplayedTick = -1;
    private int lastDisplayedBeat = -1;
    private int lastDisplayedBar = -1;
    
    // UI state
    private GridButton[][] currentButtons = null;
    private int lastPlayheadCol = -1;
    
    // Store original colors to fix trailing issue
    private Color[][] originalColors;
    
    // Scheduled executor for consistent frame rate
    private ScheduledExecutorService renderTimer;
    
    // Pre-calculated values
    private int ppq = 24;
    private int beatsPerBar = 4;
    private int totalBars = 16;  // Changed to 16 bars as requested
    private int ticksPerBar;
    private int totalTicks;
    private int columnsPerBeat;
    
    public ScrollingSequencerVisualization() {
        // Register for timing events
        TimingBus.getInstance().register(this);
        CommandBus.getInstance().register(this);
        
        // Initialize with current state
        SequencerManager sequencer = SequencerManager.getInstance();
        isPlaying = sequencer.isRunning();
        currentTick = sequencer.getCurrentTick();
        currentBeat = sequencer.getCurrentBeat();
        currentBar = sequencer.getCurrentBar();
        
        // Pre-calculate timing values
        updateTimingParameters(sequencer);
        
        // Start the render timer if playing
        if (isPlaying) {
            startRenderTimer();
        }
    }
    
    private void updateTimingParameters(SequencerManager sequencer) {
        ppq = sequencer.getPpq();
        beatsPerBar = sequencer.getBeatsPerBar();
        ticksPerBar = ppq * beatsPerBar;
        totalTicks = ticksPerBar * totalBars;
    }
    
    private void startRenderTimer() {
        if (renderTimer != null && !renderTimer.isShutdown()) {
            renderTimer.shutdown();
        }
        
        renderTimer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SequencerVisualizer-RenderThread");
            t.setDaemon(true); // Won't prevent app exit
            t.setPriority(Thread.MAX_PRIORITY); // Higher priority for smoother rendering
            return t;
        });
        
        // Schedule regular updates at the target frame rate
        renderTimer.scheduleAtFixedRate(
            this::updateDisplay, 
            0, 
            1000 / FRAMES_PER_SECOND, 
            TimeUnit.MILLISECONDS
        );
    }
    
    private void stopRenderTimer() {
        if (renderTimer != null && !renderTimer.isShutdown()) {
            renderTimer.shutdown();
            renderTimer = null;
        }
    }
    
    // This method is called by the render timer, not directly by timing events
    private void updateDisplay() {
        if (currentButtons == null || !isPlaying || originalColors == null) return;
        
        // Only update if something has changed
        if (currentTick == lastDisplayedTick && 
            currentBeat == lastDisplayedBeat && 
            currentBar == lastDisplayedBar) {
            return;
        }
        
        // Remember current state
        lastDisplayedTick = currentTick;
        lastDisplayedBeat = currentBeat;
        lastDisplayedBar = currentBar;
        
        // Execute UI updates on the EDT
        SwingUtilities.invokeLater(() -> {
            try {
                if (currentButtons == null) return;
                
                int cols = currentButtons[0].length;
                int rows = currentButtons.length;
                
                // Calculate new playhead position
                int currentPosition = (currentBar % totalBars) * ticksPerBar + 
                                    (currentBeat % beatsPerBar) * ppq +
                                    currentTick;
                int playheadCol = (currentPosition * cols) / totalTicks;
                
                // Only update if the playhead has moved
                if (playheadCol != lastPlayheadCol || lastPlayheadCol == -1) {
                    // Clear old playhead position by restoring original color
                    if (lastPlayheadCol >= 0 && lastPlayheadCol < cols) {
                        for (int row = 0; row < rows; row++) {
                            // Use saved original color instead of calculating
                            currentButtons[row][lastPlayheadCol].setBackground(
                                originalColors[row][lastPlayheadCol]);
                        }
                    }
                    
                    // Draw new playhead position
                    if (playheadCol >= 0 && playheadCol < cols) {
                        for (int row = 0; row < rows; row++) {
                            currentButtons[row][playheadCol].setBackground(POSITION_INDICATOR);
                        }
                    }
                    
                    // Update text displays (only when needed)
                    if (rows > 2) {
                        currentButtons[0][0].setText(String.format("Beat: %d", currentBeat + 1));
                        currentButtons[1][0].setText(String.format("Bar: %d", currentBar + 1));
                        currentButtons[2][0].setText(String.format("Tick: %d", currentTick));
                    }
                    
                    lastPlayheadCol = playheadCol;
                }
            } catch (Exception e) {
                // Prevent any exceptions from breaking the visualization
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public void update(GridButton[][] buttons) {
        // Store reference to buttons
        this.currentButtons = buttons;
        
        // If this is first call or reset, initialize the grid
        if (lastPlayheadCol == -1) {
            initializeGrid(buttons);
        }
        
        // Update display now
        updateDisplay();
    }
    
    private void initializeGrid(GridButton[][] buttons) {
        SwingUtilities.invokeLater(() -> {
            if (buttons == null || buttons.length == 0) return;
            
            int cols = buttons[0].length;
            int rows = buttons.length;
            
            // Initialize the color storage array
            originalColors = new Color[rows][cols];
            
            // Clear all buttons first
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    buttons[row][col].setText("");
                    buttons[row][col].setBackground(BACKGROUND_COLOR);
                    originalColors[row][col] = BACKGROUND_COLOR;
                }
            }
            
            // Get latest timing parameters
            updateTimingParameters(SequencerManager.getInstance());
            
            // Calculate grid dimensions
            int beatsTotal = beatsPerBar * totalBars;
            columnsPerBeat = Math.max(1, cols / beatsTotal);
            
            // Draw beat and bar lines
            for (int beat = 0; beat < beatsTotal; beat++) {
                int x = beat * columnsPerBeat;
                
                // First determine if this is a bar line or beat line
                boolean isBarLine = beat % beatsPerBar == 0;
                Color lineColor = isBarLine ? BAR_MARKER_COLOR : BEAT_MARKER_COLOR;
                
                // Draw vertical lines for beats/bars
                for (int row = 0; row < rows; row++) {
                    if (x < cols) {
                        buttons[row][x].setBackground(lineColor);
                        originalColors[row][x] = lineColor; // Store the original color
                    }
                }
            }
            
            // Add initial status text
            if (rows > 2 && cols > 10) {
                buttons[0][0].setText("Beat: -");
                buttons[1][0].setText("Bar: -");
                buttons[2][0].setText("Tick: -");
            }
            
            // Reset playhead position tracker
            lastPlayheadCol = -1;
        });
    }
    
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;
        
        switch (action.getCommand()) {
            // Transport state changes
            case Commands.TRANSPORT_PLAY, Commands.METRONOME_START -> {
                isPlaying = true;
                startRenderTimer();
            }
            
            case Commands.TRANSPORT_STOP, Commands.METRONOME_STOP -> {
                isPlaying = false;
                resetPosition();
                stopRenderTimer();
            }
            
            case Commands.TRANSPORT_STATE_CHANGED -> {
                if (action.getData() instanceof Boolean playing) {
                    isPlaying = playing;
                    if (playing) {
                        startRenderTimer();
                    } else {
                        resetPosition();
                        stopRenderTimer();
                    }
                }
            }
            
            // Timing events
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
            
            // Listen for timing parameter changes
            case Commands.UPDATE_TEMPO, Commands.UPDATE_TIME_SIGNATURE -> {
                updateTimingParameters(SequencerManager.getInstance());
                // Re-initialize grid with new parameters
                if (currentButtons != null) {
                    initializeGrid(currentButtons);
                }
            }
        }
    }
    
    private void resetPosition() {
        currentTick = 0;
        currentBeat = 0;
        currentBar = 0;
        lastDisplayedTick = -1;
        lastDisplayedBeat = -1;
        lastDisplayedBar = -1;
        lastPlayheadCol = -1;
        
        // Re-initialize grid
        if (currentButtons != null) {
            initializeGrid(currentButtons);
        }
    }
    
    @Override
    public String getName() {
        return "16-Bar Sequencer";
    }
    
    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    // Make sure to clean up resources
    public void cleanup() {
        stopRenderTimer();
        TimingBus.getInstance().unregister(this);
        CommandBus.getInstance().unregister(this);
        originalColors = null;
        currentButtons = null;
    }
}
