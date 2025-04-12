package com.angrysurfer.beats.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.NoteEvent;

/**
 * Panel that handles all mute buttons for both drum and melodic sequencers
 */
public class MuteButtonsPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MuteButtonsPanel.class);
    
    // Button lists
    private final List<JToggleButton> drumMuteButtons = new ArrayList<>();
    private final List<JToggleButton> melodicMuteButtons = new ArrayList<>();
    
    // Sequencer references
    private DrumSequencer drumSequencer;
    private List<MelodicSequencer> melodicSequencers;
    
    // Button colors
    private static final Color DRUM_UNMUTED_COLOR = new Color(128, 0, 128); // Purple
    private static final Color MELODIC_UNMUTED_COLOR = new Color(0, 0, 200); // Blue
    private static final Color MUTED_COLOR = new Color(255, 0, 0); // Bright red
    
    // Track active notes for visual effect duration
    private final Map<Integer, ScheduledExecutorService> activeNoteTimers = new HashMap<>();

    // Activity indicator
    private JPanel activityIndicator;
    private long lastActivityTime = 0;
    
    /**
     * Create a new mute buttons panel (without sequencers initially)
     */
    public MuteButtonsPanel() {
        initializeUI();
        registerWithCommandBus();
        // Add debug message
        logger.info("MuteButtonsPanel created and registered with CommandBus");
    }
    
    /**
     * Create a new mute buttons panel with sequencers
     */
    public MuteButtonsPanel(DrumSequencer drumSequencer, List<MelodicSequencer> melodicSequencers) {
        this.drumSequencer = drumSequencer;
        this.melodicSequencers = melodicSequencers;
        initializeUI();
        registerWithCommandBus();
    }
    
    private void registerWithCommandBus() {
        CommandBus.getInstance().register(this);
        TimingBus.getInstance().register(this);
        logger.info("MuteButtonsPanel registered with CommandBus, listening for note events");
    }
    
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }
        
        // Always print info about received events
        logger.info("MuteButtonsPanel received command: {}, data type: {}", 
                  action.getCommand(), 
                  action.getData() != null ? action.getData().getClass().getSimpleName() : "null");

        // Flash activity indicator
        lastActivityTime = System.currentTimeMillis();
        SwingUtilities.invokeLater(() -> {
            activityIndicator.setBackground(Color.GREEN);
            activityIndicator.repaint();
        });

        // Create timer to reset indicator
        javax.swing.Timer timer = new javax.swing.Timer(100, e -> {
            if (System.currentTimeMillis() - lastActivityTime > 90) {
                activityIndicator.setBackground(Color.DARK_GRAY);
                activityIndicator.repaint();
                ((javax.swing.Timer)e.getSource()).stop();
            }
        });
        timer.setRepeats(false);
        timer.start();
        
        switch (action.getCommand()) {
            case Commands.DRUM_NOTE_TRIGGERED -> {
                if (action.getData() instanceof Integer) {
                    int drumIndex = (Integer) action.getData();
                    logger.debug("Received DRUM_NOTE_TRIGGERED for drum index: {}", drumIndex);
                    
                    if (drumIndex >= 0 && drumIndex < drumMuteButtons.size()) {
                        // Get velocity from sequencer (fallback to 100 if not available)
                        int velocity = drumSequencer != null ? 
                                     drumSequencer.getVelocity(drumIndex) : 100;
                        
                        // Always use a fairly strong velocity for visibility
                        velocity = Math.max(velocity, 80);
                        
                        // Flash the button
                        logger.debug("Flashing drum button at index {} with velocity {}", drumIndex, velocity);
                        flashButton(drumMuteButtons.get(drumIndex), true, velocity, 150);
                    } else {
                        logger.warn("Drum index out of range: {}, buttons size: {}", 
                                 drumIndex, drumMuteButtons.size());
                    }
                }
            }
            
            case Commands.MELODIC_NOTE_TRIGGERED -> {
                if (action.getData() instanceof NoteEvent && action.getSender() instanceof MelodicSequencer) {
                    MelodicSequencer source = (MelodicSequencer) action.getSender();
                    NoteEvent noteEvent = (NoteEvent) action.getData();
                    
                    // Find the index of the sequencer that triggered the note
                    if (melodicSequencers != null) {
                        for (int i = 0; i < melodicSequencers.size(); i++) {
                            if (melodicSequencers.get(i) == source) {
                                // Fix duration calculation - the previous was calling getDurationMs() twice
                                // This created an infinite recursion if getDurationMs() called itself
                                
                                // Use a fixed duration if unsure (in milliseconds)
                                int durationMs = noteEvent.getDurationMs(); 
                                
                                // Try to use note velocity for visual intensity
                                int velocity = noteEvent.getVelocity();
                                
                                // Ensure we have a valid index
                                if (i < melodicMuteButtons.size()) {
                                    logger.debug("Flashing melodic button {} with velocity {} for {}ms", 
                                               i, velocity, durationMs);
                                    flashButton(melodicMuteButtons.get(i), false, velocity, durationMs);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Flash a button for the duration of a note
     */
    private void flashButton(JToggleButton button, boolean isDrum, int velocity, int durationMs) {
        // Safety check
        if (button == null) return;
        
        // Generate hash code based on button identity
        final int buttonId = System.identityHashCode(button);
        
        // Cancel any existing timer for this button
        ScheduledExecutorService existingTimer = activeNoteTimers.get(buttonId);
        if (existingTimer != null) {
            existingTimer.shutdownNow();
        }
        
        // Get button current state
        final boolean isMuted = button.isSelected();
        
        // Store original look
        final Color originalBg = button.getBackground();
        final Color originalFg = button.getForeground();
        final javax.swing.border.Border originalBorder = button.getBorder();
        
        // Use VERY dramatic flash color - bright yellow or white regardless of original color
        final Color flashColor = new Color(255, 255, 100); // Bright yellow
        
        // Update UI on EDT with dramatic changes
        SwingUtilities.invokeLater(() -> {
            // Change background to bright yellow
            button.setBackground(flashColor);
            
            // Set black text for contrast
            button.setForeground(Color.BLACK);
            
            // Add thick border
            button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));
            
            // Force immediate repaint
            button.repaint();
        });
        
        // Create timer to revert color after duration
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        activeNoteTimers.put(buttonId, timer);
        
        // Schedule task to revert to original appearance
        timer.schedule(() -> {
            SwingUtilities.invokeLater(() -> {
                // Restore original appearance
                button.setBackground(originalBg);
                button.setForeground(originalFg);
                button.setBorder(originalBorder);
                
                // Force repaint
                button.repaint();
            });
            activeNoteTimers.remove(buttonId);
            timer.shutdown();
        }, durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Get a color based on note velocity
     */
    private Color getVelocityColor(int velocity, boolean isDrum) {
        // Normalize velocity to 0.0-1.0 range
        float normalizedVelocity = Math.min(1.0f, Math.max(0.0f, velocity / 127.0f));
        
        // Base color (purple for drums, blue for melodic)
        Color baseColor = isDrum ? DRUM_UNMUTED_COLOR : MELODIC_UNMUTED_COLOR;
        
        // Create spectrum: from base color to bright yellow as velocity increases
        float[] baseHSB = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), 
                                        baseColor.getBlue(), null);
        
        // As velocity increases: increase brightness and shift hue toward yellow
        float hue = baseHSB[0] * (1.0f - normalizedVelocity * 0.5f); // Shift toward yellow
        float saturation = baseHSB[1];
        float brightness = Math.min(1.0f, baseHSB[2] + normalizedVelocity * 0.5f); // Brighter
        
        return Color.getHSBColor(hue, saturation, brightness);
    }
    
    private void initializeUI() {
        // Initialize activityIndicator FIRST
        activityIndicator = new JPanel();
        activityIndicator.setPreferredSize(new Dimension(10, 10));
        activityIndicator.setBackground(Color.DARK_GRAY);
        activityIndicator.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        // Then set up the panel layout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        
        // Now add components after everything is initialized
        add(Box.createVerticalGlue());
        add(createButtonsPanel());
        add(Box.createVerticalGlue());
    }
    
    private JPanel createButtonsPanel() {
        // Change from RIGHT alignment to CENTER for better vertical centering
        // Increase vertical gap from 0 to 5 for better spacing
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 8)); // Add padding

        // Create drum mute buttons
        for (int i = 0; i < 16; i++) {
            JToggleButton muteButton = createMuteButton(i, true);
            buttonPanel.add(muteButton);
            drumMuteButtons.add(muteButton);
        }

        // Add a more visible separator
        buttonPanel.add(Box.createHorizontalStrut(12));

        // Create melodic sequencer mute buttons
        for (int i = 0; i < 4; i++) {
            JToggleButton muteButton = createMuteButton(i, false);
            buttonPanel.add(muteButton);
            melodicMuteButtons.add(muteButton);
        }

        // Add a test button
        JButton testButton = new JButton("Test");
        testButton.setToolTipText("Test button flashing");
        testButton.addActionListener(e -> {
            // Flash the first few buttons as a test
            for (int i = 0; i < Math.min(4, drumMuteButtons.size()); i++) {
                final int index = i;
                // Stagger the flashes
                new Thread(() -> {
                    try {
                        Thread.sleep(index * 200);
                        flashButton(drumMuteButtons.get(index), true, 100, 500);
                    } catch (InterruptedException ex) {
                        // Ignore
                    }
                }).start();
            }
            
            // Flash melodic buttons if available
            if (!melodicMuteButtons.isEmpty()) {
                flashButton(melodicMuteButtons.get(0), false, 100, 500);
            }
        });
        
        buttonPanel.add(Box.createHorizontalStrut(15));
        buttonPanel.add(testButton);

        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(activityIndicator);
        
        return buttonPanel;
    }
    
    private JToggleButton createMuteButton(int index, boolean isDrum) {
        JToggleButton muteButton = new JToggleButton();

        // Increase button size for better visibility
        Dimension size = new Dimension(28, 28);  // Even bigger
        muteButton.setPreferredSize(size);
        muteButton.setMinimumSize(size);
        muteButton.setMaximumSize(size);

        // Force button properties to ensure visibility
        muteButton.setOpaque(true);
        muteButton.setBorderPainted(true);
        muteButton.setContentAreaFilled(true);
        muteButton.setFocusPainted(false);

        // Add a number to each button for easier identification
        muteButton.setText(String.valueOf(index + 1));
        muteButton.setFont(new Font("Arial", Font.BOLD, 9));
        muteButton.setToolTipText("Mute " + (isDrum ? "Drum " : "Synth ") + (index + 1));
        
        // Use extremely saturated colors
        Color defaultColor = isDrum ? 
                   new Color(180, 0, 180) :  // Very bright purple for drums
                   new Color(0, 0, 220);     // Very bright blue for melodic

        // Set initial appearance
        muteButton.setBackground(defaultColor);
        muteButton.setForeground(Color.WHITE);
        muteButton.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

        muteButton.addActionListener(e -> {
            boolean isMuted = muteButton.isSelected();
            
            // Update color based on mute state - use VERY bright red for muted
            muteButton.setBackground(isMuted ? MUTED_COLOR : defaultColor);
            muteButton.setForeground(isMuted ? Color.WHITE : Color.WHITE);

            if (isDrum) {
                toggleDrumMute(index, isMuted);
            } else {
                toggleMelodicMute(index, isMuted);
            }
        });

        return muteButton;
    }
    
    private void toggleDrumMute(int drumIndex, boolean muted) {
        logger.info("{}muting drum {}", muted ? "" : "Un", drumIndex + 1);
        if (drumSequencer != null) {
            drumSequencer.setVelocity(drumIndex, muted ? 0 : 100);
        }
    }
    
    private void toggleMelodicMute(int seqIndex, boolean muted) {
        logger.info("{}muting melodic sequencer {}", muted ? "" : "Un", seqIndex + 1);
        if (melodicSequencers != null && seqIndex < melodicSequencers.size()) {
            MelodicSequencer sequencer = melodicSequencers.get(seqIndex);
            if (sequencer != null) {
                sequencer.setLevel(muted ? 0 : 100);
            }
        }
    }
    
    /**
     * Set the drum sequencer to control
     */
    public void setDrumSequencer(DrumSequencer sequencer) {
        this.drumSequencer = sequencer;
    }
    
    /**
     * Set the melodic sequencers to control
     */
    public void setMelodicSequencers(List<MelodicSequencer> sequencers) {
        this.melodicSequencers = sequencers;
    }
    
    /**
     * Check if a drum is muted
     */
    public boolean isDrumMuted(int index) {
        if (index >= 0 && index < drumMuteButtons.size()) {
            return drumMuteButtons.get(index).isSelected();
        }
        return false;
    }
    
    /**
     * Check if a melodic sequencer is muted
     */
    public boolean isMelodicMuted(int index) {
        if (index >= 0 && index < melodicMuteButtons.size()) {
            return melodicMuteButtons.get(index).isSelected();
        }
        return false;
    }
    
    /**
     * Set mute state for a drum
     */
    public void setDrumMuted(int index, boolean muted) {
        if (index >= 0 && index < drumMuteButtons.size()) {
            JToggleButton button = drumMuteButtons.get(index);
            if (button.isSelected() != muted) {
                button.setSelected(muted);
                // Update button appearance
                Color color = muted ? MUTED_COLOR : DRUM_UNMUTED_COLOR;
                button.setBackground(color);
                // Update sequencer
                toggleDrumMute(index, muted);
            }
        }
    }
    
    /**
     * Set mute state for a melodic sequencer
     */
    public void setMelodicMuted(int index, boolean muted) {
        if (index >= 0 && index < melodicMuteButtons.size()) {
            JToggleButton button = melodicMuteButtons.get(index);
            if (button.isSelected() != muted) {
                button.setSelected(muted);
                // Update button appearance
                Color color = muted ? MUTED_COLOR : MELODIC_UNMUTED_COLOR;
                button.setBackground(color);
                // Update sequencer
                toggleMelodicMute(index, muted);
            }
        }
    }
    
    /**
     * Mute all drums
     */
    public void muteAllDrums() {
        for (int i = 0; i < drumMuteButtons.size(); i++) {
            setDrumMuted(i, true);
        }
    }
    
    /**
     * Unmute all drums
     */
    public void unmuteAllDrums() {
        for (int i = 0; i < drumMuteButtons.size(); i++) {
            setDrumMuted(i, false);
        }
    }
    
    /**
     * Mute all melodic sequencers
     */
    public void muteAllMelodic() {
        for (int i = 0; i < melodicMuteButtons.size(); i++) {
            setMelodicMuted(i, true);
        }
    }
    
    /**
     * Unmute all melodic sequencers
     */
    public void unmuteAllMelodic() {
        for (int i = 0; i < melodicMuteButtons.size(); i++) {
            setMelodicMuted(i, false);
        }
    }
    
    @Override
    public void removeNotify() {
        super.removeNotify();
        // Clean up timers when panel is removed
        for (ScheduledExecutorService timer : activeNoteTimers.values()) {
            timer.shutdownNow();
        }
        activeNoteTimers.clear();
        
        // Unregister from command bus
        CommandBus.getInstance().unregister(this);
        TimingBus.getInstance().unregister(this);
    }
}